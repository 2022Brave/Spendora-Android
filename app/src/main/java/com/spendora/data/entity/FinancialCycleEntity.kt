package com.spendora.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_cycles",
    indices = [
        Index(value = ["effective_from_year_month"], unique = true)
    ]
)
data class FinancialCycleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "cycle_start_day")
    val cycleStartDay: Int, // Day of month: 1 to 31

    @ColumnInfo(name = "effective_from_year_month")
    val effectiveFromYearMonth: String, // e.g. "2026-09"

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
