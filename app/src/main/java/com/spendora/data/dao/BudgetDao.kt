package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE cycle_year_month = :yearMonth AND amount > 0 ORDER BY id ASC")
    fun getBudgetsForCycle(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE cycle_year_month = :yearMonth AND amount <= 0 ORDER BY id ASC")
    fun getArchivedBudgetsForCycle(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id IS NULL AND cycle_year_month = :yearMonth AND amount > 0 LIMIT 1")
    suspend fun getOverallBudgetForCycle(yearMonth: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND cycle_year_month = :yearMonth AND amount > 0 LIMIT 1")
    suspend fun getCategoryBudgetForCycle(categoryId: Long, yearMonth: String): BudgetEntity?

    // Non-destructive deactivation / archival: sets amount to 0.0 without deleting the row
    @Query("UPDATE budgets SET amount = 0.0, updated_at = :now WHERE id = :id")
    suspend fun archiveBudget(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>): List<Long>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
