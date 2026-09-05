package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: AppSettingEntity)

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM app_settings ORDER BY `key` ASC")
    suspend fun getAllSnapshot(): List<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAll(settings: List<AppSettingEntity>)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}