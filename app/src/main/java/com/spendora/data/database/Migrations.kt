package com.spendora.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration definitions for SpendoraDatabase.
 * Fallback to destructive migration is strictly prohibited to prevent data loss.
 */
object Migrations {
    /**
     * Example migration from Version 1 to Version 2:
     * Adds an optional user note column or index to financial cycles if needed,
     * demonstrating a non-destructive schema evolution.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Non-destructive update example: adding an index or optional metadata table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_timestamp_type` ON `transactions` (`occurred_timestamp`, `transaction_type`)")
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2
    )
}
