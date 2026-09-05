package com.spendora.data.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialCycleCalculatorTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun testCycleStartDay1() {
        val date1 = LocalDate.of(2026, 9, 1)
        val cycle1 = FinancialCycleCalculator.calculateCycle(date1, 1, zone)
        assertEquals(LocalDate.of(2026, 9, 1), cycle1.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), cycle1.endDate)
        assertEquals("CYCLE_2026-09", cycle1.cycleId)

        val date2 = LocalDate.of(2026, 9, 30)
        val cycle2 = FinancialCycleCalculator.calculateCycle(date2, 1, zone)
        assertEquals(cycle1, cycle2)

        val date3 = LocalDate.of(2026, 10, 1)
        val cycle3 = FinancialCycleCalculator.calculateCycle(date3, 1, zone)
        assertEquals(LocalDate.of(2026, 10, 1), cycle3.startDate)
        assertEquals(LocalDate.of(2026, 10, 31), cycle3.endDate)
        assertEquals("CYCLE_2026-10", cycle3.cycleId)
    }

    @Test
    fun testCycleStartDay5() {
        // 2026-09-04 -> 2026-08-05 through 2026-09-04
        val d1 = LocalDate.of(2026, 9, 4)
        val c1 = FinancialCycleCalculator.calculateCycle(d1, 5, zone)
        assertEquals(LocalDate.of(2026, 8, 5), c1.startDate)
        assertEquals(LocalDate.of(2026, 9, 4), c1.endDate)
        assertEquals("CYCLE_2026-08", c1.cycleId)

        // 2026-09-05 -> 2026-09-05 through 2026-10-04
        val d2 = LocalDate.of(2026, 9, 5)
        val c2 = FinancialCycleCalculator.calculateCycle(d2, 5, zone)
        assertEquals(LocalDate.of(2026, 9, 5), c2.startDate)
        assertEquals(LocalDate.of(2026, 10, 4), c2.endDate)
        assertEquals("CYCLE_2026-09", c2.cycleId)

        // 2026-10-04 -> 2026-09-05 through 2026-10-04
        val d3 = LocalDate.of(2026, 10, 4)
        val c3 = FinancialCycleCalculator.calculateCycle(d3, 5, zone)
        assertEquals(c2, c3)

        // 2026-10-05 -> 2026-10-05 through 2026-11-04
        val d4 = LocalDate.of(2026, 10, 5)
        val c4 = FinancialCycleCalculator.calculateCycle(d4, 5, zone)
        assertEquals(LocalDate.of(2026, 10, 5), c4.startDate)
        assertEquals(LocalDate.of(2026, 11, 4), c4.endDate)
        assertEquals("CYCLE_2026-10", c4.cycleId)
    }

    @Test
    fun testCycleStartDay31Clamping() {
        // January (31 days) to non-leap February (28 days in 2026)
        val jan31 = LocalDate.of(2026, 1, 31)
        val janCycle = FinancialCycleCalculator.calculateCycle(jan31, 31, zone)
        assertEquals(LocalDate.of(2026, 1, 31), janCycle.startDate)
        assertEquals(LocalDate.of(2026, 2, 27), janCycle.endDate)

        // February 28, 2026 starts the February cycle
        val feb28 = LocalDate.of(2026, 2, 28)
        val febCycle = FinancialCycleCalculator.calculateCycle(feb28, 31, zone)
        assertEquals(LocalDate.of(2026, 2, 28), febCycle.startDate)
        assertEquals(LocalDate.of(2026, 3, 30), febCycle.endDate)

        // March (31 days) to April (30 days)
        val mar31 = LocalDate.of(2026, 3, 31)
        val marCycle = FinancialCycleCalculator.calculateCycle(mar31, 31, zone)
        assertEquals(LocalDate.of(2026, 3, 31), marCycle.startDate)
        assertEquals(LocalDate.of(2026, 4, 29), marCycle.endDate)

        // April (30 days) to May (31 days)
        val apr30 = LocalDate.of(2026, 4, 30)
        val aprCycle = FinancialCycleCalculator.calculateCycle(apr30, 31, zone)
        assertEquals(LocalDate.of(2026, 4, 30), aprCycle.startDate)
        assertEquals(LocalDate.of(2026, 5, 30), aprCycle.endDate)
    }

    @Test
    fun testLeapYearFebruary31() {
        // In leap year 2024, Feb has 29 days
        val jan31 = LocalDate.of(2024, 1, 31)
        val janCycle = FinancialCycleCalculator.calculateCycle(jan31, 31, zone)
        assertEquals(LocalDate.of(2024, 1, 31), janCycle.startDate)
        assertEquals(LocalDate.of(2024, 2, 28), janCycle.endDate)

        val feb29 = LocalDate.of(2024, 2, 29)
        val febCycle = FinancialCycleCalculator.calculateCycle(feb29, 31, zone)
        assertEquals(LocalDate.of(2024, 2, 29), febCycle.startDate)
        assertEquals(LocalDate.of(2024, 3, 30), febCycle.endDate)
    }

    @Test
    fun testYearBoundaryHandling() {
        // Cycle day 25 across New Year
        val dec28 = LocalDate.of(2026, 12, 28)
        val cycle = FinancialCycleCalculator.calculateCycle(dec28, 25, zone)
        assertEquals(LocalDate.of(2026, 12, 25), cycle.startDate)
        assertEquals(LocalDate.of(2027, 1, 24), cycle.endDate)
        assertEquals("CYCLE_2026-12", cycle.cycleId)

        val jan10 = LocalDate.of(2027, 1, 10)
        val cycleJan = FinancialCycleCalculator.calculateCycle(jan10, 25, zone)
        assertEquals(cycle, cycleJan)
    }

    @Test
    fun testExactMillisecondTransitions() {
        val cycle = FinancialCycleCalculator.calculateCycle(LocalDate.of(2026, 9, 10), 5, zone)
        // startTimestamp must be 2026-09-05T00:00:00
        val expectedStart = LocalDate.of(2026, 9, 5).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, cycle.startTimestamp)

        // 1 millisecond before cycle starts belongs to previous cycle
        val prevCycle = FinancialCycleCalculator.calculateCycleForTimestamp(cycle.startTimestamp - 1, 5, zone)
        assertEquals(LocalDate.of(2026, 8, 5), prevCycle.startDate)
        assertEquals(LocalDate.of(2026, 9, 4), prevCycle.endDate)

        // endTimestamp belongs to current cycle
        val cycleAtEnd = FinancialCycleCalculator.calculateCycleForTimestamp(cycle.endTimestamp, 5, zone)
        assertEquals(cycle.cycleId, cycleAtEnd.cycleId)

        // 1 millisecond after endTimestamp belongs to next cycle
        val nextCycle = FinancialCycleCalculator.calculateCycleForTimestamp(cycle.endTimestamp + 1, 5, zone)
        assertEquals(LocalDate.of(2026, 10, 5), nextCycle.startDate)
        assertEquals(LocalDate.of(2026, 11, 4), nextCycle.endDate)
    }

    @Test
    fun testPreviousAndNextCycleNavigation() {
        val cycle = FinancialCycleCalculator.calculateCycle(LocalDate.of(2026, 9, 15), 5, zone)
        val prev = FinancialCycleCalculator.getPreviousCycle(cycle, zone)
        assertEquals("CYCLE_2026-08", prev.cycleId)
        assertEquals(LocalDate.of(2026, 8, 5), prev.startDate)
        assertEquals(LocalDate.of(2026, 9, 4), prev.endDate)

        val next = FinancialCycleCalculator.getNextCycle(cycle, zone)
        assertEquals("CYCLE_2026-10", next.cycleId)
        assertEquals(LocalDate.of(2026, 10, 5), next.startDate)
        assertEquals(LocalDate.of(2026, 11, 4), next.endDate)
    }
}
