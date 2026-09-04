package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendora.data.model.CategoryType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name", "type"], unique = true),
        Index(value = ["type"]),
        Index(value = ["is_archived"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: CategoryType,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0
)
