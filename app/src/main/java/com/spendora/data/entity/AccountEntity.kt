package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendora.data.model.AccountType

/**
 * AccountEntity represents a user financial account (bank account, credit card, cash, etc.).
 * Note: The name column is indexed for search and display ordering, but is NOT uniquely constrained.
 * This ensures users can hold multiple accounts from the same bank or with the same colloquial name
 * (e.g. "Salary Account" across multiple institutions or multiple cards from the same issuer)
 * without being forced to enter artificial names.
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["type"]),
        Index(value = ["is_archived"]),
        Index(value = ["institution_name", "masked_number"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: AccountType,

    @ColumnInfo(name = "masked_number")
    val maskedNumber: String? = null,

    @ColumnInfo(name = "institution_name")
    val institutionName: String? = null,

    @ColumnInfo(name = "initial_balance")
    val initialBalance: Double = 0.0,

    @ColumnInfo(name = "current_balance")
    val currentBalance: Double = 0.0,

    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
