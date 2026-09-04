package com.spendora.data.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.CategorySeedData
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.AccountEntity
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.*
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.LiveSmsResult
import com.spendora.data.repository.TransactionRepository
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
class CoreUiAndTransactionManagementTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var txnRepo: TransactionRepository
    private lateinit var cycleRepo: FinancialCycleRepository
    private val zone = ZoneId.of("Asia/Kolkata")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        txnRepo = TransactionRepository(
            transactionDao = db.transactionDao(),
            smsAuditDao = db.smsAuditDao(),
            accountDao = db.accountDao()
        )
        cycleRepo = FinancialCycleRepository(
            financialCycleDao = db.financialCycleDao(),
            appSettingDao = db.appSettingDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFreshInstallZeroTransactions() = runBlocking {
        assertEquals(0, txnRepo.getAllTransactions().first().size)
        assertEquals(0, txnRepo.getPendingReviewCount().first())
        assertEquals(0, db.accountDao().getActiveCount())
    }

    @Test
    fun testManualExpenseCreationAndEditing() = runBlocking {
        val t = 1725470000000L
        val id = txnRepo.createManualTransaction(
            amount = 450.0,
            type = TransactionType.EXPENSE,
            merchant = "Starbucks",
            categoryId = null,
            accountId = null,
            occurredTimestamp = t,
            notes = "Morning coffee"
        )
        assertTrue(id > 0)

        val txns = txnRepo.getAllTransactions().first()
        assertEquals(1, txns.size)
        val created = txns[0]
        assertEquals(450.0, created.amount, 0.001)
        assertEquals(TransactionSource.MANUAL, created.source)
        assertEquals("Starbucks", created.merchant)

        // Edit manual transaction
        val editSuccess = txnRepo.editConfirmedTransaction(
            id = id,
            newAmount = 500.0,
            newType = TransactionType.EXPENSE,
            newMerchant = "Starbucks Reserve",
            newCategoryId = null,
            newAccountId = null
        )
        assertTrue(editSuccess)

        val updated = db.transactionDao().getById(id)!!
        assertEquals(500.0, updated.amount, 0.001)
        assertEquals("Starbucks Reserve", updated.merchant)
    }

    @Test
    fun testSmsTransactionEditingPreservesSmsIdentity() = runBlocking {
        val t = 1725470000000L
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 1,200.00 debited from A/c XX1234 at Swiggy. UPI Ref: 998877.",
            smsTimestamp = t
        )
        val res = txnRepo.processLiveSms(input) as LiveSmsResult.Persisted
        val original = db.transactionDao().getById(res.transactionId)!!

        // User edits merchant to "Swiggy Instamart"
        val editSuccess = txnRepo.editConfirmedTransaction(
            id = res.transactionId,
            newAmount = 1200.0,
            newType = TransactionType.EXPENSE,
            newMerchant = "Swiggy Instamart",
            newCategoryId = null,
            newAccountId = null
        )
        assertTrue(editSuccess)

        val updated = db.transactionDao().getById(res.transactionId)!!
        assertEquals("Swiggy Instamart", updated.merchant)
        // CRITICAL INVARIANT: SMS provenance fields are NOT altered
        assertEquals(original.dedupHash, updated.dedupHash)
        assertEquals(original.smsSender, updated.smsSender)
        assertEquals(original.smsTimestamp, updated.smsTimestamp)
        assertEquals(original.transactionReferenceNumber, updated.transactionReferenceNumber)
    }

    @Test
    fun testSafeTransactionSoftDeletion() = runBlocking {
        val t = 1725470000000L
        val id = txnRepo.createManualTransaction(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            merchant = "Cafe",
            categoryId = null,
            accountId = null,
            occurredTimestamp = t
        )
        assertEquals(1, txnRepo.getAllTransactions().first().size)

        // Delete
        val delSuccess = txnRepo.deleteTransaction(id)
        assertTrue(delSuccess)

        // Excluded from active transactions
        assertEquals(0, txnRepo.getAllTransactions().first().size)

        // Preserved in database with is_deleted = 1
        val row = db.transactionDao().getById(id)!!
        assertTrue(row.isDeleted)
    }

    @Test
    fun testSearchAndFilterTransactions() = runBlocking {
        val t1 = 1725470000000L
        val t2 = 1725475000000L

        txnRepo.createManualTransaction(150.0, TransactionType.EXPENSE, "Starbucks Coffee", t1)
        txnRepo.createManualTransaction(800.0, TransactionType.EXPENSE, "Amazon Shopping", t2)
        txnRepo.createManualTransaction(50000.0, TransactionType.INCOME, "Salary Client", t2)

        // Search by keyword "Starbucks"
        val searchStarbucks = txnRepo.searchAndFilterTransactions(searchQuery = "Starbucks").first()
        assertEquals(1, searchStarbucks.size)
        assertEquals("Starbucks Coffee", searchStarbucks[0].merchant)

        // Filter by Type = INCOME
        val filterIncome = txnRepo.searchAndFilterTransactions(type = TransactionType.INCOME).first()
        assertEquals(1, filterIncome.size)
        assertEquals("Salary Client", filterIncome[0].merchant)

        // Search + Type combined
        val combined = txnRepo.searchAndFilterTransactions(searchQuery = "Amazon", type = TransactionType.EXPENSE).first()
        assertEquals(1, combined.size)
        assertEquals("Amazon Shopping", combined[0].merchant)
    }

    @Test
    fun testDashboardMetricsExcludesPendingAndTransfers() = runBlocking {
        val cycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 10)).first()
        val t = 1725470000000L

        // Confirmed Expense = ₹1000
        txnRepo.createManualTransaction(1000.0, TransactionType.EXPENSE, "Grocery", t)

        // Transfer = ₹5000 (Must NOT count as expense)
        txnRepo.createManualTransaction(5000.0, TransactionType.TRANSFER, "Self Transfer", t)

        // Pending review = ₹2000 (Must NOT count in confirmed metrics)
        val pendingInput = RawSmsInput("VK-ALERT", "Your account was debited by Rs 2000.00.", t)
        txnRepo.processLiveSms(pendingInput)

        val summary = txnRepo.getCycleSummary(cycle).first()
        assertEquals(1000.0, summary.totalExpense, 0.001)
        assertEquals(5000.0, summary.totalTransfers, 0.001)
        assertEquals(1, summary.transactionCount)
    }
}
