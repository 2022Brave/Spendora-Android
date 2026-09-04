package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MerchantRuleEntity): Long

    @Update
    suspend fun update(rule: MerchantRuleEntity)

    @Delete
    suspend fun delete(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC, id ASC")
    fun getAllRules(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC, id ASC")
    suspend fun getAllRulesSnapshot(): List<MerchantRuleEntity>
}
