package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType

/**
 * Core transactions table representing the single source of truth for all financial movements.
 *
 * Indexing strategy:
 * - dedup_hash is UNIQUE to guarantee idempotency across live and historical inputs.
 * - transaction_reference_number is INDEXED (non-unique) to support transfer pairing and issuer lookups
 *   without failing on potential reference number collisions across disparate institutions.
 * - Composite indexes on (is_deleted, occurred_timestamp) and (is_deleted, transaction_type, occurred_timestamp)
 *   optimize high-volume (10,000+) date-range and reporting queries.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["transfer_target_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["occurred_timestamp"]),
        Index(value = ["category_id"]),
        Index(value = ["account_id"]),
        Index(value = ["transfer_target_account_id"]),
        Index(value = ["transaction_type"]),
        Index(value = ["review_status"]),
        Index(value = ["dedup_hash"], unique = true),
        Index(value = ["transaction_reference_number"]),
        Index(value = ["android_sms_row_id"]),
        Index(value = ["is_deleted"]),
        // High-performance composite indexes for financial cycle and dashboard aggregations (10,000+ rows)
        Index(value = ["is_deleted", "occurred_timestamp"]),
        Index(value = ["is_deleted", "transaction_type", "occurred_timestamp"]),
        Index(value = ["account_id", "is_deleted", "occurred_timestamp"]),
        Index(value = ["category_id", "is_deleted", "occurred_timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "transaction_type")
    val transactionType: TransactionType,

    @ColumnInfo(name = "merchant")
    val merchant: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "account_id")
    val accountId: Long? = null,

    @ColumnInfo(name = "transfer_target_account_id")
    val transferTargetAccountId: Long? = null,

    @ColumnInfo(name = "occurred_timestamp")
    val occurredTimestamp: Long,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "source")
    val source: TransactionSource,

    @ColumnInfo(name = "review_status")
    val reviewStatus: ReviewStatus = ReviewStatus.CONFIRMED,

    @ColumnInfo(name = "sms_sender")
    val smsSender: String? = null,

    @ColumnInfo(name = "sms_timestamp")
    val smsTimestamp: Long? = null,

    @ColumnInfo(name = "transaction_reference_number")
    val transactionReferenceNumber: String? = null,

    @ColumnInfo(name = "masked_account_identifier")
    val maskedAccountIdentifier: String? = null,

    @ColumnInfo(name = "dedup_hash")
    val dedupHash: String,

    @ColumnInfo(name = "android_sms_row_id")
    val androidSmsRowId: Long? = null,

    /**
     * Privacy Policy:
     * raw_sms_excerpt is NULL for confirmed transactions.
     * It is only temporarily populated when review_status == PENDING_REVIEW for user clarification,
     * and must be purged upon user review or bounded lifecycle expiration.
     */
    @ColumnInfo(name = "raw_sms_excerpt")
    val rawSmsExcerpt: String? = null,

    @ColumnInfo(name = "parser_version", defaultValue = "1")
    val parserVersion: Int = 1,

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "location_label")
    val locationLabel: String? = null,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
