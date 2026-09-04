package com.spendora.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.engine.FinancialCycleCalculator
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
import com.spendora.data.sms.model.RawSmsInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class PendingReviewWorkflowTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var repository: TransactionRepository
    private val zone = ZoneId.of("Asia/Kolkata")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            smsAuditDao = db.smsAuditDao(),
            accountDao = db.accountDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAmbiguousSmsEntersPendingReviewWithBoundedExcerpt() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 500.00 on 05-Sep.",
            smsTimestamp = 1725474600000L
        )

        val res = repository.processLiveSms(input)
        assertTrue(res is LiveSmsResult.Persisted)
        assertEquals(ReviewStatus.PENDING_REVIEW, (res as LiveSmsResult.Persisted).reviewStatus)

        val pending = repository.getPendingReviewTransactions().first()
        assertEquals(1, pending.size)
        assertEquals(ReviewStatus.PENDING_REVIEW, pending[0].reviewStatus)
        assertNotNull(pending[0].rawSmsExcerpt)
        assertTrue(pending[0].rawSmsExcerpt!!.length <= 160)
    }

    @Test
    fun testConfirmPendingTransaction() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 500.00.",
            smsTimestamp = 1725474600000L
        )
        val res = repository.processLiveSms(input) as LiveSmsResult.Persisted
        assertEquals(1, repository.getPendingReviewCount().first())

        // User confirms
        val success = repository.confirmPendingTransaction(res.transactionId)
        assertTrue(success)

        // Pending count reduces to 0
        assertEquals(0, repository.getPendingReviewCount().first())

        // Raw excerpt is cleared on confirmation per privacy policy
        val confirmedTxn = db.transactionDao().getById(res.transactionId)
        assertNotNull(confirmedTxn)
        assertEquals(ReviewStatus.CONFIRMED, confirmedTxn!!.reviewStatus)
        assertNull(confirmedTxn.rawSmsExcerpt)

        // Participating in confirmed transactions
        val allActive = repository.getAllTransactions().first()
        assertEquals(1, allActive.size)
    }

    @Test
    fun testEditAndConfirmPendingTransactionPreservesDedupIdentity() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 1200.00.",
            smsTimestamp = 1725474600000L
        )
        val res = repository.processLiveSms(input) as LiveSmsResult.Persisted
        val originalDedupHash = db.transactionDao().getById(res.transactionId)!!.dedupHash

        // User edits merchant to "IKEA" and confirms
        val success = repository.editAndConfirmPendingTransaction(
            transactionId = res.transactionId,
            newAmount = 1200.0,
            newType = TransactionType.EXPENSE,
            newMerchant = "IKEA",
            newCategoryId = null,
            newAccountId = null
        )
        assertTrue(success)

        val updated = db.transactionDao().getById(res.transactionId)!!
        assertEquals(ReviewStatus.CONFIRMED, updated.reviewStatus)
        assertEquals("IKEA", updated.merchant)
        assertNull(updated.rawSmsExcerpt)
        // CRITICAL INVARIANT: Original SMS dedup identity is preserved
        assertEquals(originalDedupHash, updated.dedupHash)

        // If the same SMS is ingested again, it is correctly flagged as DUPLICATE
        val rerun = repository.processLiveSms(input)
        assertTrue(rerun is LiveSmsResult.Duplicate)
    }

    @Test
    fun testIgnorePendingTransactionSurvivesRerun() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 300.00.",
            smsTimestamp = 1725474600000L
        )
        val res = repository.processLiveSms(input) as LiveSmsResult.Persisted

        // User chooses to IGNORE
        val success = repository.ignorePendingTransaction(res.transactionId)
        assertTrue(success)

        // Disappears from pending review
        assertEquals(0, repository.getPendingReviewCount().first())

        // Does NOT participate in active confirmed transactions
        assertEquals(0, repository.getAllTransactions().first().size)

        // If same SMS is re-delivered live or scanned historically, it must NOT recreate the pending item!
        val liveRetry = repository.processLiveSms(input)
        assertTrue(liveRetry is LiveSmsResult.Duplicate)

        val histRetry = repository.processHistoricalSms(input.copy(androidSmsRowId = 999L))
        assertTrue(histRetry is LiveSmsResult.Duplicate)

        // Pending review count remains 0
        assertEquals(0, repository.getPendingReviewCount().first())
    }

    @Test
    fun testPendingTransactionsExcludedFromFinancialCycleAggregates() = runBlocking {
        val cycle = FinancialCycleCalculator.calculateCycle(LocalDate.of(2026, 9, 10), 5, zone)
        val t = 1725474600000L // 5 Sep 2026

        // 1. Confirmed expense = ₹1000
        val confirmedInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 1,000.00 debited from A/c XX1234 to SWIGGY. UPI Ref: 11223344.",
            smsTimestamp = t
        )
        repository.processLiveSms(confirmedInput)

        // 2. Pending review expense = ₹500
        val pendingInput = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 500.00.",
            smsTimestamp = t
        )
        repository.processLiveSms(pendingInput)

        // Summary must report ONLY confirmed expense ₹1000, NOT ₹1500
        val summary = repository.getCycleSummary(cycle).first()
        assertEquals(1000.0, summary.totalExpense, 0.001)
        assertEquals(1, summary.transactionCount)
    }

    @Test
    fun testManualTransactionDoesNotCollideWithSmsTransaction() = runBlocking {
        val t = 1725474600000L
        // SMS transaction
        val smsInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 250.00 debited from card XX1234 at Starbucks.",
            smsTimestamp = t
        )
        repository.processLiveSms(smsInput)

        // Manual transaction on same day, same amount, same merchant
        val manualId = repository.createManualTransaction(
            amount = 250.0,
            type = TransactionType.EXPENSE,
            merchant = "Starbucks",
            categoryId = null,
            accountId = null,
            occurredTimestamp = t,
            notes = "Manual coffee"
        )
        assertTrue(manualId > 0)

        // Both transactions exist independently
        val allTxns = repository.getAllTransactions().first()
        assertEquals(2, allTxns.size)
    }
}
