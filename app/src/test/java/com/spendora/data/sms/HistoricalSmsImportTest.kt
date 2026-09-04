package com.spendora.data.sms

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
import com.spendora.data.repository.LiveSmsResult
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.sms.import.HistoricalImportProgress
import com.spendora.data.sms.import.ImportState
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
class HistoricalSmsImportTest {

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
    fun testHistoricalImportWithProviderRowId() = runBlocking {
        val rowId = 1001L
        val timestamp = 1725430000000L
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 1,200.00 debited from A/c XX4321 on 04-Sep to Swiggy. UPI Ref: 987654321.",
            smsTimestamp = timestamp,
            androidSmsRowId = rowId
        )

        val result = repository.processHistoricalSms(input)
        assertTrue(result is LiveSmsResult.Persisted)

        val txns = repository.getAllTransactions().first()
        assertEquals(1, txns.size)
        val txn = txns[0]
        assertEquals(TransactionSource.SMS_HISTORICAL, txn.source)
        assertEquals(rowId, txn.androidSmsRowId)
        assertEquals(timestamp, txn.smsTimestamp)
        assertEquals("Swiggy", txn.merchant)
        assertEquals(1200.0, txn.amount, 0.001)
    }

    @Test
    fun testLayer1ProviderRowIdIdempotency() = runBlocking {
        val rowId = 2002L
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 350.00 debited at Cafe. UPI Ref: 11223344.",
            smsTimestamp = 1725430000000L,
            androidSmsRowId = rowId
        )

        val res1 = repository.processHistoricalSms(input)
        assertTrue(res1 is LiveSmsResult.Persisted)

        // Rerun import with exact same provider row ID
        val res2 = repository.processHistoricalSms(input)
        assertTrue(res2 is LiveSmsResult.Duplicate)

        // Total transactions remain exactly 1
        assertEquals(1, repository.getAllTransactions().first().size)
    }

    @Test
    fun testCrossPipelineLiveThenHistoricalDuplicatePrevention() = runBlocking {
        val t = 1725430000000L
        val body = "INR 800.00 debited from A/c XX1234 at Starbucks. UPI Ref: 44556677."

        // 1. Live SMS broadcast arrives first (androidSmsRowId is null)
        val liveInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = body,
            smsTimestamp = t,
            androidSmsRowId = null
        )
        val liveRes = repository.processLiveSms(liveInput)
        assertTrue(liveRes is LiveSmsResult.Persisted)
        assertEquals(1, repository.getAllTransactions().first().size)

        // 2. User subsequently runs historical import (same message, now has provider row ID 501)
        val histInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = body,
            smsTimestamp = t,
            androidSmsRowId = 501L
        )
        val histRes = repository.processHistoricalSms(histInput)
        assertTrue("Historical scan must detect duplicate of existing live transaction", histRes is LiveSmsResult.Duplicate)

        // Still exactly 1 transaction in Room
        assertEquals(1, repository.getAllTransactions().first().size)
    }

    @Test
    fun testCrossPipelineHistoricalThenLiveDuplicatePrevention() = runBlocking {
        val t = 1725430000000L
        val body = "INR 500.00 debited from A/c XX1234 at Zomato. UPI Ref: 88990011."

        // 1. Historical import runs first (has provider row ID 601)
        val histInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = body,
            smsTimestamp = t,
            androidSmsRowId = 601L
        )
        val histRes = repository.processHistoricalSms(histInput)
        assertTrue(histRes is LiveSmsResult.Persisted)
        assertEquals(1, repository.getAllTransactions().first().size)

        // 2. A live SMS broadcast subsequently delivers the same message (androidSmsRowId is null)
        val liveInput = RawSmsInput(
            sender = "VM-HDFCBK",
            body = body,
            smsTimestamp = t,
            androidSmsRowId = null
        )
        val liveRes = repository.processLiveSms(liveInput)
        assertTrue("Live receiver must detect duplicate of existing historical transaction", liveRes is LiveSmsResult.Duplicate)

        // Still exactly 1 transaction in Room
        assertEquals(1, repository.getAllTransactions().first().size)
    }

    @Test
    fun testRepeatedAndOverlappingImportIdempotency() = runBlocking {
        val t1 = 1725430000000L
        val t2 = 1725435000000L

        val sms1 = RawSmsInput("VM-HDFCBK", "INR 100.00 debited from card XX1234 at Cafe. Ref: 101010.", t1, 701L)
        val sms2 = RawSmsInput("VM-HDFCBK", "INR 200.00 debited from card XX1234 at Bakery. Ref: 202020.", t2, 702L)

        // Import 1: Jan 1 -> Today (contains sms1 and sms2)
        repository.processHistoricalSms(sms1)
        repository.processHistoricalSms(sms2)
        assertEquals(2, repository.getAllTransactions().first().size)

        // Import 2: Overlapping range (contains sms2 and a new sms3)
        val sms3 = RawSmsInput("VM-HDFCBK", "INR 300.00 debited from card XX1234 at Pharmacy. Ref: 303030.", t2 + 1000, 703L)
        repository.processHistoricalSms(sms2) // duplicate
        repository.processHistoricalSms(sms3) // new

        assertEquals(3, repository.getAllTransactions().first().size)
    }

    @Test
    fun testTwoLegitimateSameAmountTransactionsNotCollapsed() = runBlocking {
        val t = 1725430000000L
        val smsA = RawSmsInput("VM-HDFCBK", "INR 50 debited at Metro on card XX1234.", t, 801L)
        val smsB = RawSmsInput("VM-HDFCBK", "INR 50 debited at Bus on card XX1234.", t, 802L)

        val resA = repository.processHistoricalSms(smsA)
        val resB = repository.processHistoricalSms(smsB)

        assertTrue(resA is LiveSmsResult.Persisted)
        assertTrue(resB is LiveSmsResult.Persisted)
        assertEquals(2, repository.getAllTransactions().first().size)
    }
}
