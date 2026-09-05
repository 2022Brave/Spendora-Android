package com.spendora.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendora.data.converter.Converters
import com.spendora.data.dao.*
import com.spendora.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        MerchantRuleEntity::class,
        FinancialCycleEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        AppSettingEntity::class,
        SmsAuditEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpendoraDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun financialCycleDao(): FinancialCycleDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun smsAuditDao(): SmsAuditDao

    companion object {
        const val DATABASE_NAME = "spendora.db"

        @Volatile
        private var INSTANCE: SpendoraDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): SpendoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendoraDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(SpendoraDatabaseCallback(scope))
                    .addMigrations(*Migrations.ALL_MIGRATIONS)
                    // Note: fallbackToDestructiveMigration() is intentionally OMITTED
                    // to prevent data loss on schema updates.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SpendoraDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(database: SpendoraDatabase) {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCount() == 0) {
                categoryDao.insertAll(CategorySeedData.defaultCategories)
            }
        }
    }
}
