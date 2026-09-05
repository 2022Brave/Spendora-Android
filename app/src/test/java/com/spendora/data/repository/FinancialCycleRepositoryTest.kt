package com.spendora.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.FinancialCycle
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
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
class FinancialCycleRepositoryTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var cycleRepo: FinancialCycleRepository
    private lateinit var txnRepo: TransactionRepository
    private val zone = ZoneId.of("Asia/Kolkata")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cycleRepo = FinancialCycleRepository(db.financialCycleDao(), db.appSettingDao())
        txnRepo = TransactionRepository(db.transactionDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDefaultCycleStartDay() = runBlocking {
        val startDay = cycleRepo.getCycleStartDay().first()
        assertEquals(1, startDay)
    }

    @Test
    fun testConfigureCycleStartDay() = runBlocking {
        cycleRepo.setCycleStartDay(5)
        val startDay = cycleRepo.getCycleStartDay().first()
        assertEquals(5, startDay)
    }

    @Test
    fun testStartNewCycleDoesNotDeleteOrModifyTransactions() = runBlocking {
        // Insert transactions across multiple cycles
        val t1 = LocalDate.of(2026, 8, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val t2 = LocalDate.of(2026, 9, 10).atStartOfDay(zone).toInstant().toEpochMilli()

        txnRepo.insertTransaction(
            TransactionEntity(
                amount = 500.0,
                transactionType = TransactionType.EXPENSE,
                merchant = "August Store",
                occurredTimestamp = t1,
                source = TransactionSource.MANUAL,
                dedupHash = "hash_aug_1"
            )
        )
        txnRepo.insertTransaction(
            TransactionEntity(
                amount = 800.0,
                transactionType = TransactionType.EXPENSE,
                merchant = "September Store",
                occurredTimestamp = t2,
                source = TransactionSource.MANUAL,
                dedupHash = "hash_sep_1"
            )
        )

        assertEquals(2, db.transactionDao().getCount())

        // User starts a new cycle
        cycleRepo.startNewCycle(cycleStartDay = 5, effectiveFromYearMonth = "2026-10", notes = "October Cycle")

        // CRITICAL INVARIANT: Transactions must remain 100% intact!
        assertEquals(2, db.transactionDao().getCount())
        val allTxns = txnRepo.getAllTransactions().first()
        assertEquals(2, allTxns.size)
    }

    @Test
    fun testCycleSummaryCategorization() = runBlocking {
        // Cycle: 5 Sep 2026 - 4 Oct 2026
        val cycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 15)).first()
        val baseTime = LocalDate.of(2026, 9, 10).atStartOfDay(zone).toInstant().toEpochMilli()

        // 1. Expense: 1000.0
        txnRepo.insertTransaction(
            TransactionEntity(amount = 1000.0, transactionType = TransactionType.EXPENSE, merchant = "Groceries", occurredTimestamp = baseTime, source = TransactionSource.MANUAL, dedupHash = "h1")
        )
        // 2. Income: 50000.0
        txnRepo.insertTransaction(
            TransactionEntity(amount = 50000.0, transactionType = TransactionType.INCOME, merchant = "Employer", occurredTimestamp = baseTime, source = TransactionSource.MANUAL, dedupHash = "h2")
        )
        // 3. Transfer: 5000.0 (Must NOT count as expense)
        txnRepo.insertTransaction(
            TransactionEntity(amount = 5000.0, transactionType = TransactionType.TRANSFER, merchant = "Self Transfer", occurredTimestamp = baseTime, source = TransactionSource.MANUAL, dedupHash = "h3")
        )
        // 4. Refund: 200.0 (Must NOT count as ordinary expense or ordinary income)
        txnRepo.insertTransaction(
            TransactionEntity(amount = 200.0, transactionType = TransactionType.REFUND, merchant = "Amazon Refund", occurredTimestamp = baseTime, source = TransactionSource.MANUAL, dedupHash = "h4")
        )
        // 5. Cash Withdrawal: 2000.0 (Distinguishable from ordinary expense)
        txnRepo.insertTransaction(
            TransactionEntity(amount = 2000.0, transactionType = TransactionType.CASH_WITHDRAWAL, merchant = "ATM", occurredTimestamp = baseTime, source = TransactionSource.MANUAL, dedupHash = "h5")
        )

        val summary = txnRepo.getCycleSummary(cycle).first()
        assertEquals(1000.0, summary.totalExpense, 0.001)
        assertEquals(50000.0, summary.totalIncome, 0.001)
        assertEquals(200.0, summary.totalRefunds, 0.001)
        assertEquals(5000.0, summary.totalTransfers, 0.001)
        assertEquals(2000.0, summary.totalCashWithdrawals, 0.001)
        // Net Cash Flow = (50000 + 200) - (1000 + 2000) = 47200.0
        assertEquals(47200.0, summary.netCashFlow, 0.001)
        assertEquals(5, summary.transactionCount)
    }
}
