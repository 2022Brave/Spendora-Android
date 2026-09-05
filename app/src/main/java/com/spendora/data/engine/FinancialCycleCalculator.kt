package com.spendora.data.engine

import com.spendora.data.model.FinancialCycle
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * FinancialCycleCalculator
 *
 * Pure, deterministic date calculation engine using Java 8+ java.time.
 * Accurately implements salary cycle reporting windows with strict month-end clamping.
 *
 * Month clamping rule:
 * For any year and month (Y, M), the effective cycle start date is:
 *   LocalDate.of(Y, M, min(cycleStartDay, lengthOfMonth(Y, M)))
 *
 * Clamping ensures invalid dates (e.g. Feb 31) are never created,
 * mapping leap-year Feb 29 and non-leap Feb 28 cleanly.
 */
object FinancialCycleCalculator {

    private val LABEL_MONTH_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val LABEL_FULL_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

    fun calculateCycle(
        date: LocalDate,
        cycleStartDay: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FinancialCycle {
        require(cycleStartDay in 1..31) { "cycleStartDay must be in 1..31, got $cycleStartDay" }

        val currentMonthStart = getEffectiveStartDate(date.year, date.monthValue, cycleStartDay)

        val (cycleYear, cycleMonth, cycleStartDate, nextCycleStartDate) = if (!date.isBefore(currentMonthStart)) {
            // Target date is on or after current month start -> belongs to current month cycle
            val nextYm = YearMonth.of(date.year, date.monthValue).plusMonths(1)
            val nextStart = getEffectiveStartDate(nextYm.year, nextYm.monthValue, cycleStartDay)
            CycleBounds(date.year, date.monthValue, currentMonthStart, nextStart)
        } else {
            // Target date is before current month start -> belongs to previous month cycle
            val prevYm = YearMonth.of(date.year, date.monthValue).minusMonths(1)
            val prevStart = getEffectiveStartDate(prevYm.year, prevYm.monthValue, cycleStartDay)
            CycleBounds(prevYm.year, prevYm.monthValue, prevStart, currentMonthStart)
        }

        val cycleEndDate = nextCycleStartDate.minusDays(1)

        val startZdt = cycleStartDate.atStartOfDay(zoneId)
        val startTimestamp = startZdt.toInstant().toEpochMilli()

        val endZdt = cycleEndDate.atTime(LocalTime.MAX).atZone(zoneId)
        val endTimestamp = endZdt.toInstant().toEpochMilli()

        val yearMonthString = String.format(Locale.US, "%04d-%02d", cycleYear, cycleMonth)
        val cycleId = "CYCLE_$yearMonthString"
        val label = buildDisplayLabel(cycleStartDate, cycleEndDate)

        return FinancialCycle(
            cycleId = cycleId,
            cycleYearMonth = yearMonthString,
            startDate = cycleStartDate,
            endDate = cycleEndDate,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            displayLabel = label,
            cycleStartDay = cycleStartDay
        )
    }

    fun calculateCycleForTimestamp(
        timestamp: Long,
        cycleStartDay: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FinancialCycle {
        val localDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        return calculateCycle(localDate, cycleStartDay, zoneId)
    }

    fun getPreviousCycle(
        currentCycle: FinancialCycle,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FinancialCycle {
        val prevDate = currentCycle.startDate.minusDays(1)
        return calculateCycle(prevDate, currentCycle.cycleStartDay, zoneId)
    }

    fun getNextCycle(
        currentCycle: FinancialCycle,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FinancialCycle {
        val nextDate = currentCycle.endDate.plusDays(1)
        return calculateCycle(nextDate, currentCycle.cycleStartDay, zoneId)
    }

    private fun getEffectiveStartDate(year: Int, month: Int, configuredDay: Int): LocalDate {
        val ym = YearMonth.of(year, month)
        val clampedDay = Math.min(configuredDay, ym.lengthOfMonth())
        return LocalDate.of(year, month, clampedDay)
    }

    private fun buildDisplayLabel(start: LocalDate, end: LocalDate): String {
        return if (start.year == end.year) {
            "${start.format(LABEL_MONTH_DAY)} – ${end.format(LABEL_FULL_DATE)}"
        } else {
            "${start.format(LABEL_FULL_DATE)} – ${end.format(LABEL_FULL_DATE)}"
        }
    }

    private data class CycleBounds(
        val year: Int,
        val month: Int,
        val startDate: LocalDate,
        val nextStartDate: LocalDate
    )
}
