package com.spendora.data.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.BudgetEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.*
import com.spendora.data.repository.BudgetRepository
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
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
class Stage9DashboardAnalyticsTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var txnRepo: TransactionRepository
    private lateinit var cycleRepo: FinancialCycleRepository
    private lateinit var budgetRepo: BudgetRepository
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
        budgetRepo = BudgetRepository(
            budgetDao = db.budgetDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDashboardNetSpendingWithRefunds() = runBlocking {
        val cycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 10)).first()
        val t = cycle.startTimestamp + 100000L

        // Confirmed Expense = ₹2,000
        txnRepo.createManualTransaction(2000.0, TransactionType.EXPENSE, "Store A", 1, 1, t)
        // Confirmed Refund = ₹500
        txnRepo.createManualTransaction(500.0, TransactionType.REFUND, "Store A Refund", 1, 1, t + 1000)

        val summary = txnRepo.getDashboardSummary(cycle, db.budgetDao()).first()
        // Net spending must be ₹1,500
        assertEquals(1500.0, summary.totalSpending, 0.001)
        assertEquals(500.0, summary.totalRefunds, 0.001)
    }

    @Test
    fun testPendingTransactionsExcludedFromDashboard() = runBlocking {
        val cycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 10)).first()
        val t = cycle.startTimestamp + 100000L

        // Confirmed Expense = ₹1,000
        txnRepo.createManualTransaction(1000.0, TransactionType.EXPENSE, "Confirmed Store", 1, 1, t)

        // Pending Expense = ₹5,000
        db.transactionDao().insert(
            TransactionEntity(
                amount = 5000.0,
                transactionType = TransactionType.EXPENSE,
                merchant = "Pending Store",
                occurredTimestamp = t,
                source = TransactionSource.SMS_LIVE,
                reviewStatus = ReviewStatus.PENDING_REVIEW,
                dedupHash = "DEDUP_PENDING_TEST",
                parserVersion = 1,
                isDeleted = false,
                createdAt = t,
                updatedAt = t
            )
        )

        val summary = txnRepo.getDashboardSummary(cycle, db.budgetDao()).first()
        // Must report ONLY confirmed expense ₹1,000, NOT ₹6,000
        assertEquals(1000.0, summary.totalSpending, 0.001)
        assertEquals(1, summary.pendingReviewCount)
        assertEquals(1, summary.confirmedTransactionCount)
    }

    @Test
    fun testBudgetProgressAndStatus() = runBlocking {
        val cycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 10)).first()
        val t = cycle.startTimestamp + 100000L

        // Create Budget for Food (category 1) = ₹5,000
        budgetRepo.createOrUpdateBudget(1L, 5000.0, cycle.cycleYearMonth)

        // Spend ₹4,500 in Food (90% -> WARNING)
        txnRepo.createManualTransaction(4500.0, TransactionType.EXPENSE, "Food Store", 1L, 1L, t)

        val budgets = budgetRepo.getBudgetsWithProgressForCycle(cycle).first()
        assertEquals(1, budgets.size)
        val foodBudget = budgets[0]
        assertEquals(4500.0, foodBudget.spentAmount, 0.001)
        assertEquals(500.0, foodBudget.remainingAmount, 0.001)
        assertEquals(90.0, foodBudget.percentageConsumed, 0.001)
        assertEquals(BudgetStatus.WARNING, foodBudget.status)
    }

    @Test
    fun testCycleComparisonCalculation() = runBlocking {
        val curCycle = cycleRepo.getCurrentCycle(zone, LocalDate.of(2026, 9, 10)).first()
        val prevDate = curCycle.startDate.minusDays(5)
        val prevCycle = cycleRepo.getCurrentCycle(zone, prevDate).first()

        // Current spending = ₹3,000
        txnRepo.createManualTransaction(3000.0, TransactionType.EXPENSE, "Current Store", 1L, 1L, curCycle.startTimestamp + 5000L)
        // Previous spending = ₹2,000
        txnRepo.createManualTransaction(2000.0, TransactionType.EXPENSE, "Previous Store", 1L, 1L, prevCycle.startTimestamp + 5000L)

        val comp = txnRepo.getCycleComparison(curCycle, prevCycle).first()
        assertEquals(3000.0, comp.currentSpending, 0.001)
        assertEquals(2000.0, comp.previousSpending, 0.001)
        assertEquals(1000.0, comp.absoluteDifference, 0.001)
        assertNotNull(comp.percentageChange)
        assertEquals(50.0, comp.percentageChange!!, 0.001) // +50%
        assertTrue(comp.hasPreviousData)
    }
}
