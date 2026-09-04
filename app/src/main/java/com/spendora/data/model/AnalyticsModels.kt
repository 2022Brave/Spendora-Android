package com.spendora.data.model

enum class BudgetStatus {
    HEALTHY,  // < 80%
    WARNING,  // 80% to < 100%
    EXCEEDED  // >= 100%
}

data class DashboardSummary(
    val cycle: FinancialCycle,
    val totalSpending: Double, // Confirmed Expenses - Confirmed Refunds
    val totalIncome: Double,
    val totalRefunds: Double,
    val totalTransfers: Double,
    val totalCashWithdrawals: Double,
    val netCashFlow: Double, // (Income + Refunds) - (Expenses + Cash Withdrawals)
    val confirmedTransactionCount: Int,
    val pendingReviewCount: Int,
    val activeBudgetCount: Int,
    val totalBudgetAmount: Double,
    val totalBudgetSpent: Double,
    val cycleProgressFraction: Float,
    val daysElapsed: Int,
    val daysRemaining: Int
)

data class CategorySpendingItem(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val icon: String,
    val expenseAmount: Double,
    val refundAmount: Double,
    val netAmount: Double,
    val percentageOfTotal: Double,
    val transactionCount: Int
)

data class CycleComparison(
    val currentSpending: Double,
    val previousSpending: Double,
    val absoluteDifference: Double,
    val percentageChange: Double?, // null if previousSpending == 0.0 ("No previous spending")
    val hasPreviousData: Boolean
)

data class BudgetItemWithProgress(
    val budgetId: Long,
    val categoryId: Long?,
    val categoryName: String,
    val icon: String,
    val colorHex: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentageConsumed: Double,
    val status: BudgetStatus
)

data class AccountSpendingItem(
    val accountId: Long?,
    val accountName: String,
    val maskedNumber: String?,
    val totalSpent: Double,
    val totalIncome: Double,
    val transactionCount: Int
)

data class TransactionTypeItem(
    val type: TransactionType,
    val totalAmount: Double,
    val count: Int
)

data class SpendingTrendPoint(
    val label: String,
    val netSpending: Double,
    val expenseAmount: Double,
    val refundAmount: Double,
    val count: Int
)

enum class AnalyticsPeriod(val label: String) {
    CURRENT_CYCLE("Current Cycle"),
    PREVIOUS_CYCLE("Previous Cycle"),
    LAST_3_CYCLES("Last 3 Cycles"),
    LAST_6_CYCLES("Last 6 Cycles"),
    LAST_12_CYCLES("Last 12 Cycles"),
    CUSTOM_RANGE("Custom Range")
}

data class AnalyticsTimeRange(
    val period: AnalyticsPeriod,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val displayLabel: String,
    val isMultiCycle: Boolean
)
