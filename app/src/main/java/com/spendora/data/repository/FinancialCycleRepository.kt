package com.spendora.data.repository

import com.spendora.data.dao.AppSettingDao
import com.spendora.data.dao.FinancialCycleDao
import com.spendora.data.engine.FinancialCycleCalculator
import com.spendora.data.entity.AppSettingEntity
import com.spendora.data.entity.FinancialCycleEntity
import com.spendora.data.model.FinancialCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository responsible for financial cycle configuration, history, and active reporting periods.
 *
 * Invariant: Changing or starting cycles NEVER modifies or deletes existing transactions.
 */
class FinancialCycleRepository(
    private val financialCycleDao: FinancialCycleDao,
    private val appSettingDao: AppSettingDao
) {
    companion object {
        const val KEY_CYCLE_START_DAY = "financial_cycle_start_day"
        const val DEFAULT_CYCLE_START_DAY = 1
    }

    fun getCycleStartDay(): Flow<Int> {
        return appSettingDao.observe(KEY_CYCLE_START_DAY).map { value ->
            value?.toIntOrNull()?.coerceIn(1, 31) ?: DEFAULT_CYCLE_START_DAY
        }.distinctUntilChanged()
    }

    suspend fun getCycleStartDaySnapshot(): Int {
        val value = appSettingDao.get(KEY_CYCLE_START_DAY)
        return value?.toIntOrNull()?.coerceIn(1, 31) ?: DEFAULT_CYCLE_START_DAY
    }

    suspend fun setCycleStartDay(day: Int) {
        require(day in 1..31) { "Invalid cycle start day: $day" }
        appSettingDao.set(
            AppSettingEntity(
                key = KEY_CYCLE_START_DAY,
                value = day.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun getCurrentCycle(
        zoneId: ZoneId = ZoneId.systemDefault(),
        currentDate: LocalDate = LocalDate.now(zoneId)
    ): Flow<FinancialCycle> {
        return getCycleStartDay().map { startDay ->
            FinancialCycleCalculator.calculateCycle(currentDate, startDay, zoneId)
        }.distinctUntilChanged()
    }

    suspend fun getCurrentCycleSnapshot(
        zoneId: ZoneId = ZoneId.systemDefault(),
        currentDate: LocalDate = LocalDate.now(zoneId)
    ): FinancialCycle {
        val startDay = getCycleStartDaySnapshot()
        return FinancialCycleCalculator.calculateCycle(currentDate, startDay, zoneId)
    }

    /**
     * Starts a new financial cycle configuration.
     * Note: This only records the active reporting period. It DOES NOT touch, archive, or delete transactions.
     */
    suspend fun startNewCycle(
        cycleStartDay: Int,
        effectiveFromYearMonth: String,
        notes: String? = null
    ): Long {
        require(cycleStartDay in 1..31) { "Invalid cycle start day: $cycleStartDay" }
        setCycleStartDay(cycleStartDay)
        val entity = FinancialCycleEntity(
            cycleStartDay = cycleStartDay,
            effectiveFromYearMonth = effectiveFromYearMonth,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
        return financialCycleDao.insert(entity)
    }

    fun getLatestCycleEntity(): Flow<FinancialCycleEntity?> {
        return financialCycleDao.getLatestCycle()
    }
}
