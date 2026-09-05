package com.spendora.data.sms

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.AccountEntity
import com.spendora.data.model.AccountType
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
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

@RunWith(RobolectricTestRunner::class)
class SmsBroadcastReceiverTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var repository: TransactionRepository

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
    fun testLiveSmsPersistenceDirectToRoomWithoutUi() = runBlocking {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 850.00 debited from A/c XX1234 on 04-Sep-26 to SWIGGY. UPI Ref: 987654321.",
            smsTimestamp = 1725430000000L
        )

        val result = repository.processLiveSms(input)
        assertTrue(result is LiveSmsResult.Persisted)
        assertEquals(ReviewStatus.CONFIRMED, (result as LiveSmsResult.Persisted).reviewStatus)

        // Directly query Room database to confirm persistence without UI
        val allTxns = repository.getAllTransactions().first()
        assertEquals(1, allTxns.size)
        val txn = allTxns[0]
        assertEquals(850.0, txn.amount, 0.001)
        assertEquals("Swiggy", txn.merchant)
        assertEquals("XX1234", txn.maskedAccountIdentifier)
        assertEquals("987654321", txn.transactionReferenceNumber)
        assertEquals(TransactionSource.SMS_LIVE, txn.source)
        assertEquals(1725430000000L, txn.smsTimestamp)
        assertNull("Confirmed transactions must have null raw SMS excerpt", txn.rawSmsExcerpt)
    }

    @Test
    fun testPendingReviewPersistenceRetainsBoundedExcerpt() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 500.00.",
            smsTimestamp = 1725430000000L
        )

        val result = repository.processLiveSms(input)
        assertTrue(result is LiveSmsResult.Persisted)
        assertEquals(ReviewStatus.PENDING_REVIEW, (result as LiveSmsResult.Persisted).reviewStatus)

        val allTxns = repository.getAllTransactions().first()
        assertEquals(1, allTxns.size)
        val txn = allTxns[0]
        assertEquals(ReviewStatus.PENDING_REVIEW, txn.reviewStatus)
        assertNotNull(txn.rawSmsExcerpt)
        assertTrue(txn.rawSmsExcerpt!!.length <= 160)
    }

    @Test
    fun testIgnoreCreatesNoTransaction() = runBlocking {
        val input = RawSmsInput(
            sender = "VK-SBIINB",
            body = "482913 is your OTP for transaction of INR 5,000.00 at Amazon. Do not share.",
            smsTimestamp = 1725430000000L
        )

        val result = repository.processLiveSms(input)
        assertTrue(result is LiveSmsResult.Ignored)
        assertEquals(0, repository.getAllTransactions().first().size)
    }

    @Test
    fun testIdempotencyRepeatedDeliveryCreatesOnlyOneTransaction() = runBlocking {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 1,200.00 debited from A/c XX1234 to ZOMATO. UPI Ref: 1122334455.",
            smsTimestamp = 1725435000000L
        )

        // Process 1st time -> Persisted
        val res1 = repository.processLiveSms(input)
        assertTrue(res1 is LiveSmsResult.Persisted)

        // Process 2nd time -> Duplicate
        val res2 = repository.processLiveSms(input)
        assertTrue(res2 is LiveSmsResult.Duplicate)

        // Process 3rd time -> Duplicate
        val res3 = repository.processLiveSms(input)
        assertTrue(res3 is LiveSmsResult.Duplicate)

        // Total in Room must be exactly 1
        assertEquals(1, repository.getAllTransactions().first().size)
    }

    @Test
    fun testTwoLegitimateIdenticalAmountPurchasesCoexist() = runBlocking {
        // User buys ₹100 coffee at 10:00 AM
        val t1 = 1725430000000L
        val input1 = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 100.00 debited from Card XX4321 at Starbucks.",
            smsTimestamp = t1
        )
        val res1 = repository.processLiveSms(input1)
        assertTrue(res1 is LiveSmsResult.Persisted)

        // User buys another ₹100 coffee at 2:00 PM
        val t2 = 1725444400000L
        val input2 = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 100.00 debited from Card XX4321 at Starbucks.",
            smsTimestamp = t2
        )
        val res2 = repository.processLiveSms(input2)
        assertTrue(res2 is LiveSmsResult.Persisted)

        // Both legitimate purchases preserved
        assertEquals(2, repository.getAllTransactions().first().size)
    }

    @Test
    fun testDeterministicAccountResolution() = runBlocking {
        // Pre-configure user account in Room
        val accId = db.accountDao().insert(
            AccountEntity(
                name = "HDFC Primary",
                type = AccountType.BANK_ACCOUNT,
                institutionName = "HDFC Bank",
                maskedNumber = "4321"
            )
        )

        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 450.00 debited from A/c XX4321 at Cafe. UPI Ref: 123.",
            smsTimestamp = 1725430000000L
        )
        repository.processLiveSms(input)

        val txn = repository.getAllTransactions().first()[0]
        assertEquals(accId, txn.accountId)
    }
}
