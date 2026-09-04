package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getByNameAndType(name: String, type: CategoryType): CategoryEntity?

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order ASC, name ASC")
    fun getAllActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_archived = 0 AND type = :type ORDER BY sort_order ASC, name ASC")
    fun getActiveByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    @Query("UPDATE categories SET is_archived = 1 WHERE id = :id AND is_system = 0")
    suspend fun archiveCategory(id: Long)

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order ASC, name ASC")
    suspend fun getAllActiveSnapshot(): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE is_archived = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllArchived(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE is_system = 0")
    suspend fun deleteCustomCategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}