package com.spendora.data.backup

import androidx.room.withTransaction
import com.spendora.data.database.CategorySeedData
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.*
import com.spendora.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class BackupValidationResult {
    data class Valid(val accountCount: Int, val categoryCount: Int, val transactionCount: Int) : BackupValidationResult()
    data class Invalid(val reason: String) : BackupValidationResult()
}

/**
 * DataBackupManager
 *
 * Local JSON export and atomic transactional restore for SPENDORA.
 *
 * Privacy Invariants:
 * - Never exports raw SMS bodies, OTP contents, or audit body logs.
 * - Restores SMS deduplication hashes and provenance fields intact to prevent re-import duplicates.
 * - Restores manual transactions with their independent nonces.
 * - Restores PENDING_REVIEW transactions as pending with bounded excerpts (<= 160 chars).
 * - Atomic restore: validation runs completely before any database modification. On failure, zero changes are made.
 */
class DataBackupManager(
    private val database: SpendoraDatabase
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_VERSION = "1.0.0"
    }

    suspend fun exportToJson(): String {
        val accounts = database.accountDao().getAllSnapshot()
        val categories = database.categoryDao().getAllSnapshot()
        val transactions = database.transactionDao().getAllSnapshot()
        val budgets = database.budgetDao().getAllSnapshot()
        val cycles = database.financialCycleDao().getAllSnapshot()
        val settings = database.appSettingDao().getAllSnapshot()

        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("appVersion", APP_VERSION)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        // 1. Accounts
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type.name)
            obj.put("maskedNumber", acc.maskedNumber ?: JSONObject.NULL)
            obj.put("institutionName", acc.institutionName ?: JSONObject.NULL)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("currentBalance", acc.currentBalance)
            obj.put("isDefault", acc.isDefault)
            obj.put("isArchived", acc.isArchived)
            obj.put("createdAt", acc.createdAt)
            obj.put("updatedAt", acc.updatedAt)
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        // 2. Categories
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("type", cat.type.name)
            obj.put("icon", cat.icon)
            obj.put("colorHex", cat.colorHex)
            obj.put("isSystem", cat.isSystem)
            obj.put("isArchived", cat.isArchived)
            obj.put("sortOrder", cat.sortOrder)
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // 3. Transactions (Full provenance and dedupHash, NO raw SMS bodies for confirmed)
        val transactionsArray = JSONArray()
        for (txn in transactions) {
            val obj = JSONObject()
            obj.put("id", txn.id)
            obj.put("amount", txn.amount)
            obj.put("transactionType", txn.transactionType.name)
            obj.put("merchant", txn.merchant)
            obj.put("categoryId", txn.categoryId ?: JSONObject.NULL)
            obj.put("accountId", txn.accountId ?: JSONObject.NULL)
            obj.put("transferTargetAccountId", txn.transferTargetAccountId ?: JSONObject.NULL)
            obj.put("occurredTimestamp", txn.occurredTimestamp)
            obj.put("notes", txn.notes ?: JSONObject.NULL)
            obj.put("source", txn.source.name)
            obj.put("reviewStatus", txn.reviewStatus.name)
            obj.put("smsSender", txn.smsSender ?: JSONObject.NULL)
            obj.put("smsTimestamp", txn.smsTimestamp ?: JSONObject.NULL)
            obj.put("transactionReferenceNumber", txn.transactionReferenceNumber ?: JSONObject.NULL)
            obj.put("maskedAccountIdentifier", txn.maskedAccountIdentifier ?: JSONObject.NULL)
            obj.put("dedupHash", txn.dedupHash)
            obj.put("androidSmsRowId", txn.androidSmsRowId ?: JSONObject.NULL)
            // Privacy Invariant: rawSmsExcerpt exported ONLY if PENDING_REVIEW
            val excerpt = if (txn.reviewStatus == ReviewStatus.PENDING_REVIEW) txn.rawSmsExcerpt?.take(160) else null
            obj.put("rawSmsExcerpt", excerpt ?: JSONObject.NULL)
            obj.put("parserVersion", txn.parserVersion)
            obj.put("latitude", txn.latitude ?: JSONObject.NULL)
            obj.put("longitude", txn.longitude ?: JSONObject.NULL)
            obj.put("locationLabel", txn.locationLabel ?: JSONObject.NULL)
            obj.put("isDeleted", txn.isDeleted)
            obj.put("createdAt", txn.createdAt)
            obj.put("updatedAt", txn.updatedAt)
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // 4. Budgets
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("categoryId", b.categoryId ?: JSONObject.NULL)
            obj.put("amount", b.amount)
            obj.put("cycleYearMonth", b.cycleYearMonth)
            obj.put("createdAt", b.createdAt)
            obj.put("updatedAt", b.updatedAt)
            budgetsArray.put(obj)
        }
        root.put("budgets", budgetsArray)

        // 5. Financial Cycles
        val cyclesArray = JSONArray()
        for (c in cycles) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("cycleStartDay", c.cycleStartDay)
            obj.put("effectiveFromYearMonth", c.effectiveFromYearMonth)
            obj.put("notes", c.notes ?: JSONObject.NULL)
            obj.put("createdAt", c.createdAt)
            cyclesArray.put(obj)
        }
        root.put("financialCycles", cyclesArray)

        // 6. App Settings
        val settingsObj = JSONObject()
        for (s in settings) {
            settingsObj.put(s.key, s.value)
        }
        root.put("appSettings", settingsObj)

        return root.toString(2)
    }

    fun validateBackup(backupJson: String): BackupValidationResult {
        return try {
            val root = JSONObject(backupJson)
            if (!root.has("schemaVersion")) {
                return BackupValidationResult.Invalid("Missing schemaVersion")
            }
            val schemaVer = root.getInt("schemaVersion")
            if (schemaVer != CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult.Invalid("Unsupported backup schemaVersion: $schemaVer")
            }

            val accountsArray = root.optJSONArray("accounts") ?: return BackupValidationResult.Invalid("Missing accounts array")
            val categoriesArray = root.optJSONArray("categories") ?: return BackupValidationResult.Invalid("Missing categories array")
            val txnsArray = root.optJSONArray("transactions") ?: return BackupValidationResult.Invalid("Missing transactions array")

            val accountIds = mutableSetOf<Long>()
            for (i in 0 until accountsArray.length()) {
                val acc = accountsArray.getJSONObject(i)
                if (!acc.has("id") || !acc.has("name") || !acc.has("type")) {
                    return BackupValidationResult.Invalid("Malformed account entry at index $i")
                }
                accountIds.add(acc.getLong("id"))
            }

            val categoryIds = mutableSetOf<Long>()
            for (i in 0 until categoriesArray.length()) {
                val cat = categoriesArray.getJSONObject(i)
                if (!cat.has("id") || !cat.has("name") || !cat.has("type")) {
                    return BackupValidationResult.Invalid("Malformed category entry at index $i")
                }
                categoryIds.add(cat.getLong("id"))
            }

            for (i in 0 until txnsArray.length()) {
                val txn = txnsArray.getJSONObject(i)
                if (!txn.has("amount") || !txn.has("transactionType") || !txn.has("dedupHash") || !txn.has("occurredTimestamp")) {
                    return BackupValidationResult.Invalid("Malformed transaction entry at index $i")
                }
                val amt = txn.getDouble("amount")
                if (amt <= 0.0) {
                    return BackupValidationResult.Invalid("Invalid non-positive transaction amount at index $i: $amt")
                }

                // Check foreign key references if present
                if (!txn.isNull("accountId")) {
                    val accId = txn.getLong("accountId")
                    if (!accountIds.contains(accId)) {
                        return BackupValidationResult.Invalid("Transaction references unknown accountId: $accId")
                    }
                }
                if (!txn.isNull("categoryId")) {
                    val catId = txn.getLong("categoryId")
                    if (!categoryIds.contains(catId)) {
                        return BackupValidationResult.Invalid("Transaction references unknown categoryId: $catId")
                    }
                }
            }

            BackupValidationResult.Valid(
                accountCount = accountsArray.length(),
                categoryCount = categoriesArray.length(),
                transactionCount = txnsArray.length()
            )
        } catch (e: Exception) {
            BackupValidationResult.Invalid("Malformed JSON backup: ${e.message}")
        }
    }

    suspend fun restoreFromJson(backupJson: String): Boolean {
        val validation = validateBackup(backupJson)
        if (validation !is BackupValidationResult.Valid) {
            return false
        }

        val root = JSONObject(backupJson)
        val accountsArray = root.getJSONArray("accounts")
        val categoriesArray = root.getJSONArray("categories")
        val txnsArray = root.getJSONArray("transactions")
        val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
        val cyclesArray = root.optJSONArray("financialCycles") ?: JSONArray()
        val settingsObj = root.optJSONObject("appSettings") ?: JSONObject()

        // Atomic replacement within Room transaction
        database.withTransaction {
            // 1. Clear existing data
            database.transactionDao().deleteAll()
            database.budgetDao().deleteAll()
            database.accountDao().deleteAll()
            database.categoryDao().deleteAll()
            database.financialCycleDao().deleteAll()
            database.appSettingDao().deleteAll()
            database.smsAuditDao().deleteAll()

            // 2. Restore Categories (ensuring 27 system categories exist)
            val restoredCategories = mutableListOf<CategoryEntity>()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.getJSONObject(i)
                restoredCategories.add(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = CategoryType.valueOf(obj.getString("type")),
                        icon = obj.optString("icon", "category"),
                        colorHex = obj.optString("colorHex", "#9C27B0"),
                        isSystem = obj.optBoolean("isSystem", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }
            database.categoryDao().insertAll(restoredCategories)
            // Ensure any missing system categories are seeded
            val currentCatCount = database.categoryDao().getCount()
            if (currentCatCount < 27) {
                database.categoryDao().insertAll(CategorySeedData.defaultCategories)
            }

            // 3. Restore Accounts
            val restoredAccounts = mutableListOf<AccountEntity>()
            for (i in 0 until accountsArray.length()) {
                val obj = accountsArray.getJSONObject(i)
                restoredAccounts.add(
                    AccountEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = AccountType.valueOf(obj.getString("type")),
                        maskedNumber = if (obj.isNull("maskedNumber")) null else obj.getString("maskedNumber"),
                        institutionName = if (obj.isNull("institutionName")) null else obj.getString("institutionName"),
                        initialBalance = obj.optDouble("initialBalance", 0.0),
                        currentBalance = obj.optDouble("currentBalance", 0.0),
                        isDefault = obj.optBoolean("isDefault", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            database.accountDao().insertAll(restoredAccounts)

            // 4. Restore Transactions
            val restoredTransactions = mutableListOf<TransactionEntity>()
            val restoredAudits = mutableListOf<SmsAuditEntity>()

            for (i in 0 until txnsArray.length()) {
                val obj = txnsArray.getJSONObject(i)
                val reviewStatus = ReviewStatus.valueOf(obj.getString("reviewStatus"))
                val rawExcerpt = if (reviewStatus == ReviewStatus.PENDING_REVIEW && !obj.isNull("rawSmsExcerpt")) {
                    obj.getString("rawSmsExcerpt").take(160)
                } else null

                val txn = TransactionEntity(
                    id = obj.getLong("id"),
                    amount = obj.getDouble("amount"),
                    transactionType = TransactionType.valueOf(obj.getString("transactionType")),
                    merchant = obj.getString("merchant"),
                    categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                    accountId = if (obj.isNull("accountId")) null else obj.getLong("accountId"),
                    transferTargetAccountId = if (obj.isNull("transferTargetAccountId")) null else obj.getLong("transferTargetAccountId"),
                    occurredTimestamp = obj.getLong("occurredTimestamp"),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    source = TransactionSource.valueOf(obj.getString("source")),
                    reviewStatus = reviewStatus,
                    smsSender = if (obj.isNull("smsSender")) null else obj.getString("smsSender"),
                    smsTimestamp = if (obj.isNull("smsTimestamp")) null else obj.getLong("smsTimestamp"),
                    transactionReferenceNumber = if (obj.isNull("transactionReferenceNumber")) null else obj.getString("transactionReferenceNumber"),
                    maskedAccountIdentifier = if (obj.isNull("maskedAccountIdentifier")) null else obj.getString("maskedAccountIdentifier"),
                    dedupHash = obj.getString("dedupHash"),
                    androidSmsRowId = if (obj.isNull("androidSmsRowId")) null else obj.getLong("androidSmsRowId"),
                    rawSmsExcerpt = rawExcerpt,
                    parserVersion = obj.optInt("parserVersion", 1),
                    latitude = if (obj.isNull("latitude")) null else obj.getDouble("latitude"),
                    longitude = if (obj.isNull("longitude")) null else obj.getDouble("longitude"),
                    locationLabel = if (obj.isNull("locationLabel")) null else obj.getString("locationLabel"),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                restoredTransactions.add(txn)

                // Populate sms_audits for restored SMS transactions to ensure Layer 3 deduplication is active
                if (txn.source == TransactionSource.SMS_LIVE || txn.source == TransactionSource.SMS_HISTORICAL) {
                    val sender = txn.smsSender ?: "RESTORED"
                    val smsTs = txn.smsTimestamp ?: txn.occurredTimestamp
                    val fp = "RESTORED|$sender|$smsTs|${txn.dedupHash.take(16)}"
                    restoredAudits.add(
                        SmsAuditEntity(
                            androidSmsRowId = txn.androidSmsRowId,
                            sender = sender,
                            smsTimestamp = smsTs,
                            processedTimestamp = System.currentTimeMillis(),
                            isFinancial = true,
                            rejectionReason = null,
                            matchedTransactionId = txn.id,
                            fingerprint = fp
                        )
                    )
                }
            }
            database.transactionDao().insertAll(restoredTransactions)
            for (audit in restoredAudits) {
                database.smsAuditDao().insert(audit)
            }

            // 5. Restore Budgets
            val restoredBudgets = mutableListOf<BudgetEntity>()
            for (i in 0 until budgetsArray.length()) {
                val obj = budgetsArray.getJSONObject(i)
                restoredBudgets.add(
                    BudgetEntity(
                        id = obj.getLong("id"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        amount = obj.getDouble("amount"),
                        cycleYearMonth = obj.getString("cycleYearMonth"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            database.budgetDao().insertAll(restoredBudgets)

            // 6. Restore Financial Cycles
            val restoredCycles = mutableListOf<FinancialCycleEntity>()
            for (i in 0 until cyclesArray.length()) {
                val obj = cyclesArray.getJSONObject(i)
                restoredCycles.add(
                    FinancialCycleEntity(
                        id = obj.getLong("id"),
                        cycleStartDay = obj.getInt("cycleStartDay"),
                        effectiveFromYearMonth = obj.getString("effectiveFromYearMonth"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.financialCycleDao().insertAll(restoredCycles)

            // 7. Restore Settings
            val restoredSettings = mutableListOf<AppSettingEntity>()
            val keys = settingsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                restoredSettings.add(
                    AppSettingEntity(
                        key = k,
                        value = settingsObj.getString(k),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            database.appSettingDao().setAll(restoredSettings)
        }

        return true
    }
}
