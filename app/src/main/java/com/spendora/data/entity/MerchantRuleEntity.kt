package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendora.data.model.TransactionType

@Entity(
    tableName = "merchant_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["default_category_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["default_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["merchant_pattern"]),
        Index(value = ["default_category_id"]),
        Index(value = ["default_account_id"])
    ]
)
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "merchant_pattern")
    val merchantPattern: String,

    @ColumnInfo(name = "default_category_id")
    val defaultCategoryId: Long,

    @ColumnInfo(name = "default_account_id")
    val defaultAccountId: Long? = null,

    @ColumnInfo(name = "default_type")
    val defaultType: TransactionType? = null,

    @ColumnInfo(name = "is_regex", defaultValue = "0")
    val isRegex: Boolean = false,

    @ColumnInfo(name = "priority", defaultValue = "0")
    val priority: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
