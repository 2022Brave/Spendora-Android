package com.spendora.data.repository

import com.spendora.data.dao.BudgetDao
import com.spendora.data.dao.CategoryDao
import com.spendora.data.dao.TransactionDao
import com.spendora.data.entity.BudgetEntity
import com.spendora.data.model.BudgetItemWithProgress
import com.spendora.data.model.BudgetStatus
import com.spendora.data.model.FinancialCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * BudgetRepository
 *
 * Coordinates category budgets and active-period consumption calculations.
 *
 * Non-destructive Invariants:
 * - Budget archival/deletion does NOT delete the database row; it archives the budget by setting amount = 0.0.
 * - Archiving a budget NEVER deletes or modifies transactions, categories, or accounts.
 * - Active budget queries filter amount > 0, safely excluding archived budgets.
 */
class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    fun getBudgetsForCycle(cycleYearMonth: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForCycle(cycleYearMonth)
    }

    fun getBudgetsWithProgressForCycle(cycle: FinancialCycle): Flow<List<BudgetItemWithProgress>> {
        val budgetsFlow = budgetDao.getBudgetsForCycle(cycle.cycleYearMonth)
        val categoriesFlow = categoryDao.getAllActive()

        return combine(budgetsFlow, categoriesFlow) { budgets, categories ->
            val catMap = categories.associateBy { it.id }
            budgets.map { b ->
                val cat = b.categoryId?.let { catMap[it] }
                val catName = cat?.name ?: "All Categories"
                val catIcon = cat?.icon ?: "pie_chart"
                val catColor = cat?.colorHex ?: "#9C27B0"
                Pair(b, Triple(catName, catIcon, catColor))
            }
        }.flatMapLatest { pairs ->
            if (pairs.isEmpty()) {
                flowOf(emptyList())
            } else {
                val spendingFlows = pairs.map { (budget, _) ->
                    if (budget.categoryId != null) {
                        transactionDao.getBudgetSpendingForCategoryBetween(
                            budget.categoryId,
                            cycle.startTimestamp,
                            cycle.endTimestamp
                        )
                    } else {
                        // Overall budget: total confirmed expenses minus total refunds
                        combine(
                            transactionDao.getTotalExpensesBetween(cycle.startTimestamp, cycle.endTimestamp),
                            transactionDao.getTotalRefundsBetween(cycle.startTimestamp, cycle.endTimestamp)
                        ) { exp, ref -> (exp - ref).coerceAtLeast(0.0) }
                    }
                }

                combine(spendingFlows) { spendings ->
                    pairs.mapIndexed { index, (budget, meta) ->
                        val (catName, catIcon, catColor) = meta
                        val spent = spendings[index]
                        val budgetAmt = budget.amount
                        val remaining = (budgetAmt - spent).coerceAtLeast(0.0)
                        val pct = if (budgetAmt > 0) (spent / budgetAmt) * 100.0 else 0.0

                        val status = when {
                            pct >= 100.0 -> BudgetStatus.EXCEEDED
                            pct >= 80.0 -> BudgetStatus.WARNING
                            else -> BudgetStatus.HEALTHY
                        }

                        BudgetItemWithProgress(
                            budgetId = budget.id,
                            categoryId = budget.categoryId,
                            categoryName = catName,
                            icon = catIcon,
                            colorHex = catColor,
                            budgetAmount = budgetAmt,
                            spentAmount = spent,
                            remainingAmount = remaining,
                            percentageConsumed = pct,
                            status = status
                        )
                    }
                }
            }
        }
    }

    suspend fun createOrUpdateBudget(categoryId: Long?, amount: Double, cycleYearMonth: String): Long {
        if (amount <= 0.0) throw IllegalArgumentException("Budget amount must be positive")
        return budgetDao.insertOrUpdate(
            BudgetEntity(
                categoryId = categoryId,
                amount = amount,
                cycleYearMonth = cycleYearMonth,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Non-destructive budget deactivation.
     * Sets amount to 0.0 without deleting the row or altering transaction history.
     */
    suspend fun archiveBudget(id: Long) {
        budgetDao.archiveBudget(id)
    }

    suspend fun deleteBudget(id: Long) {
        archiveBudget(id)
    }
}
