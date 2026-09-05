package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY is_default DESC, name ASC")
    fun getAllActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY is_archived ASC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts WHERE is_archived = 0")
    suspend fun getActiveCount(): Int

    @Query("SELECT * FROM accounts WHERE masked_number = :maskedNumber AND is_archived = 0 LIMIT 1")
    suspend fun findByMaskedNumber(maskedNumber: String): AccountEntity?

    @Query("UPDATE accounts SET current_balance = current_balance + :delta, updated_at = :now WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END, updated_at = :now")
    suspend fun setDefaultAccount(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archiveAccount(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY is_default DESC, name ASC")
    suspend fun getAllActiveSnapshot(): List<AccountEntity>
    @Query("SELECT * FROM accounts WHERE is_archived = 1 ORDER BY name ASC")
    fun getAllArchived(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}