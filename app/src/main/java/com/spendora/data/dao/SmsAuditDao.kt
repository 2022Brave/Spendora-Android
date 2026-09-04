package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.SmsAuditEntity

@Dao
interface SmsAuditDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(audit: SmsAuditEntity): Long

    @Query("SELECT * FROM sms_audits WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): SmsAuditEntity?

    @Query("SELECT * FROM sms_audits WHERE android_sms_row_id = :rowId LIMIT 1")
    suspend fun findByAndroidSmsRowId(rowId: Long): SmsAuditEntity?

    @Query("SELECT COUNT(*) FROM sms_audits")
    suspend fun getCount(): Int
}    @Query("DELETE FROM sms_audits")
    suspend fun deleteAll()
}