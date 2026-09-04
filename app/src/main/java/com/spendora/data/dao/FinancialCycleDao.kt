package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.FinancialCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: FinancialCycleEntity): Long

    @Query("SELECT * FROM financial_cycles ORDER BY effective_from_year_month DESC LIMIT 1")
    fun getLatestCycle(): Flow<FinancialCycleEntity?>

    @Query("SELECT * FROM financial_cycles ORDER BY effective_from_year_month DESC LIMIT 1")
    suspend fun getLatestCycleSnapshot(): FinancialCycleEntity?

    @Query("SELECT * FROM financial_cycles WHERE effective_from_year_month <= :yearMonth ORDER BY effective_from_year_month DESC LIMIT 1")
    suspend fun getCycleForYearMonth(yearMonth: String): FinancialCycleEntity?
}    @Query("SELECT * FROM financial_cycles ORDER BY effective_from_year_month ASC")
    suspend fun getAllSnapshot(): List<FinancialCycleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cycles: List<FinancialCycleEntity>): List<Long>

    @Query("DELETE FROM financial_cycles")
    suspend fun deleteAll()
}