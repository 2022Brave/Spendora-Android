package com.spendora.data.model

import java.time.LocalDate

/**
 * Domain model representing a financial reporting cycle.
 *
 * A financial cycle is a reporting window defined by a start date and an end date (inclusive).
 * It never modifies or deletes underlying transactions.
 */
data class FinancialCycle(
    val cycleId: String,
    val cycleYearMonth: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val displayLabel: String,
    val cycleStartDay: Int
)
