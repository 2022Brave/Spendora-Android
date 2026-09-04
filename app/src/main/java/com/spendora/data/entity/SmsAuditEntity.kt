package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_audits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["matched_transaction_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["android_sms_row_id"]),
        Index(value = ["matched_transaction_id"]),
        Index(value = ["sms_timestamp"])
    ]
)
data class SmsAuditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "android_sms_row_id")
    val androidSmsRowId: Long? = null,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "sms_timestamp")
    val smsTimestamp: Long,

    @ColumnInfo(name = "processed_timestamp")
    val processedTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_financial")
    val isFinancial: Boolean,

    @ColumnInfo(name = "rejection_reason")
    val rejectionReason: String? = null,

    @ColumnInfo(name = "matched_transaction_id")
    val matchedTransactionId: Long? = null,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String
)
