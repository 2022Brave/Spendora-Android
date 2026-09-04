package com.spendora.data.model

/**
 * Aggregate summary for a given financial reporting cycle.
 *
 * Transfers and refunds are strictly distinguished from ordinary expenses.
 */
data class FinancialCycleSummary(
    val cycle: FinancialCycle,
    val totalExpense: Double,
    val totalIncome: Double,
    val totalRefunds: Double,
    val totalTransfers: Double,
    val totalCashWithdrawals: Double,
    val netCashFlow: Double,
    val transactionCount: Int
)
