package com.spendora.data.repository

import com.spendora.data.dao.AccountDao
import com.spendora.data.dao.SmsAuditDao
import com.spendora.data.dao.TransactionDao
import com.spendora.data.entity.SmsAuditEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.*
import com.spendora.data.sms.SmsEngine
import com.spendora.data.sms.model.ParseStatus
import com.spendora.data.sms.model.RawSmsInput
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.util.UUID

sealed class LiveSmsResult {
    data class Persisted(val transactionId: Long, val reviewStatus: ReviewStatus) : LiveSmsResult()
    data class Duplicate(val existingTransactionId: Long) : LiveSmsResult()
    data class Ignored(val reason: String?) : LiveSmsResult()
    data class Failed(val error: String) : LiveSmsResult()
}

/**
 * TransactionRepository
 *
 * Coordinates transaction persistence, deduplication, live & historical SMS ingestion,
 * pending review workflows, and reporting-period aggregations.
 *
 * Invariants:
 * - Single source of truth is Room SQLite.
 * - Confirmed transactions never retain raw SMS bodies.
 * - PENDING_REVIEW transactions contain only bounded excerpts (<= 160 chars).
 * - Only CONFIRMED transactions participate in financial metrics (dashboard, cycles, budgets).
 * - Deduplication is multi-layered and conservative.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val smsAuditDao: SmsAuditDao? = null,
    private val accountDao: AccountDao? = null
) {
    private val _liveTransactionEvents = MutableSharedFlow<TransactionEntity>(extraBufferCapacity = 64)
    val liveTransactionEvents: SharedFlow<TransactionEntity> = _liveTransactionEvents.asSharedFlow()

    // Query active confirmed transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllActive()
    }

    // Pending review flow (newest first)
    fun getPendingReviewTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getPendingReviewTransactions()
    }

    fun getPendingReviewCount(): Flow<Int> {
        return transactionDao.getPendingReviewCount()
    }

    // Cycle transactions (strictly confirmed only)
    fun getTransactionsForCycle(cycle: FinancialCycle): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(cycle.startTimestamp, cycle.endTimestamp)
    }

    // Aggregate summary for cycle (strictly confirmed only)
    fun getCycleSummary(cycle: FinancialCycle): Flow<FinancialCycleSummary> {
        val expensesFlow = transactionDao.getTotalExpensesBetween(cycle.startTimestamp, cycle.endTimestamp)
        val incomeFlow = transactionDao.getTotalIncomeBetween(cycle.startTimestamp, cycle.endTimestamp)
        val refundsFlow = transactionDao.getTotalRefundsBetween(cycle.startTimestamp, cycle.endTimestamp)
        val transfersFlow = transactionDao.getTotalTransfersBetween(cycle.startTimestamp, cycle.endTimestamp)
        val cashWithdrawalsFlow = transactionDao.getTotalCashWithdrawalsBetween(cycle.startTimestamp, cycle.endTimestamp)
        val countFlow = transactionDao.getTransactionCountBetween(cycle.startTimestamp, cycle.endTimestamp)

        return combine(
            expensesFlow,
            incomeFlow,
            refundsFlow,
            transfersFlow,
            cashWithdrawalsFlow,
            countFlow
        ) { expense, income, refunds, transfers, cash, count ->
            val netFlow = (income + refunds) - (expense + cash)
            FinancialCycleSummary(
                cycle = cycle,
                totalExpense = expense,
                totalIncome = income,
                totalRefunds = refunds,
                totalTransfers = transfers,
                totalCashWithdrawals = cash,
                netCashFlow = netFlow,
                transactionCount = count
            )
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun softDelete(id: Long) {
        transactionDao.softDelete(id)
    }

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.getRecentTransactions(limit)
    }

    fun searchAndFilterTransactions(
        searchQuery: String? = null,
        type: TransactionType? = null,
        categoryId: Long? = null,
        accountId: Long? = null,
        source: TransactionSource? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<TransactionEntity>> {
        val query = if (searchQuery.isNullOrBlank()) null else searchQuery.trim()
        return transactionDao.filterTransactions(query, type, categoryId, accountId, source, startTime, endTime, limit, offset)
    }

    fun getCategorySpendingForCycle(cycle: FinancialCycle): Flow<List<com.spendora.data.dao.CategorySpending>> {
        return transactionDao.getCategorySpendingBetween(cycle.startTimestamp, cycle.endTimestamp)
    }

    suspend fun editConfirmedTransaction(
        id: Long,
        newAmount: Double,
        newType: TransactionType,
        newMerchant: String,
        newCategoryId: Long?,
        newAccountId: Long?,
        newOccurredTimestamp: Long? = null,
        newNotes: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
        removeLocation: Boolean = false
    ): Boolean {
        val existing = transactionDao.getById(id) ?: return false
        val updated = existing.copy(
            amount = newAmount,
            transactionType = newType,
            merchant = newMerchant,
            categoryId = newCategoryId,
            accountId = newAccountId,
            occurredTimestamp = newOccurredTimestamp ?: existing.occurredTimestamp,
            notes = newNotes ?: existing.notes,
            latitude = if (removeLocation) null else (latitude ?: existing.latitude),
            longitude = if (removeLocation) null else (longitude ?: existing.longitude),
            locationLabel = if (removeLocation) null else (locationLabel ?: existing.locationLabel),
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

    suspend fun deleteTransaction(id: Long): Boolean {
        val existing = transactionDao.getById(id) ?: return false
        transactionDao.softDelete(id)
        return true
    }


    // --- PENDING REVIEW ACTIONS ---

    /**
     * Confirms a pending review transaction without altering financial amounts.
     * Clears rawSmsExcerpt per privacy policy and updates reviewStatus to CONFIRMED.
     */
    suspend fun confirmPendingTransaction(
        transactionId: Long,
        categoryId: Long? = null,
        accountId: Long? = null
    ): Boolean {
        val existing = transactionDao.getById(transactionId) ?: return false
        if (existing.reviewStatus != ReviewStatus.PENDING_REVIEW) return false

        val updated = existing.copy(
            reviewStatus = ReviewStatus.CONFIRMED,
            categoryId = categoryId ?: existing.categoryId,
            accountId = accountId ?: existing.accountId,
            rawSmsExcerpt = null, // Strictly cleared on confirmation
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

    /**
     * Edits financial details and confirms a pending review transaction.
     * CRITICAL: Preserves original SMS identity fields (dedupHash, sender, timestamps, ref)
     * so deduplication identity is maintained while financial fields are corrected.
     */
    suspend fun editAndConfirmPendingTransaction(
        transactionId: Long,
        newAmount: Double,
        newType: TransactionType,
        newMerchant: String,
        newCategoryId: Long?,
        newAccountId: Long?,
        newOccurredTimestamp: Long? = null
    ): Boolean {
        val existing = transactionDao.getById(transactionId) ?: return false
        if (existing.reviewStatus != ReviewStatus.PENDING_REVIEW) return false

        val updated = existing.copy(
            amount = newAmount,
            transactionType = newType,
            merchant = newMerchant,
            categoryId = newCategoryId,
            accountId = newAccountId,
            occurredTimestamp = newOccurredTimestamp ?: existing.occurredTimestamp,
            reviewStatus = ReviewStatus.CONFIRMED,
            rawSmsExcerpt = null, // Cleared on confirmation
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

    /**
     * Ignores a pending review transaction.
     * Keeps the row marked as IGNORED with its dedupHash to prevent repeated ingestion
     * from recreating the pending item. Excluded from all financial metrics.
     */
    suspend fun ignorePendingTransaction(transactionId: Long): Boolean {
        val existing = transactionDao.getById(transactionId) ?: return false
        if (existing.reviewStatus != ReviewStatus.PENDING_REVIEW) return false

        val updated = existing.copy(
            reviewStatus = ReviewStatus.IGNORED,
            rawSmsExcerpt = null,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

    suspend fun bulkConfirm(transactionIds: List<Long>): Int {
        var confirmedCount = 0
        for (id in transactionIds) {
            if (confirmPendingTransaction(id)) confirmedCount++
        }
        return confirmedCount
    }

    suspend fun bulkIgnore(transactionIds: List<Long>): Int {
        var ignoredCount = 0
        for (id in transactionIds) {
            if (ignorePendingTransaction(id)) ignoredCount++
        }
        return ignoredCount
    }

    /**
     * Creates a manual transaction.
     * Manual transactions use source = MANUAL and a unique nonce/UUID in dedupHash,
     * guaranteeing they never collide with or get deduplicated against SMS transactions.
     */
    suspend fun createManualTransaction(
        amount: Double,
        type: TransactionType,
        merchant: String,
        categoryId: Long?,
        accountId: Long?,
        occurredTimestamp: Long,
        notes: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null
    ): Long {
        val nonce = UUID.randomUUID().toString().take(8)
        val dedupHash = "MANUAL|$occurredTimestamp|$amount|${accountId ?: "NO_ACC"}|$merchant|$nonce"
        val entity = TransactionEntity(
            amount = amount,
            transactionType = type,
            merchant = merchant,
            categoryId = categoryId,
            accountId = accountId,
            transferTargetAccountId = null,
            occurredTimestamp = occurredTimestamp,
            notes = notes,
            source = TransactionSource.MANUAL,
            reviewStatus = ReviewStatus.CONFIRMED,
            smsSender = null,
            smsTimestamp = null,
            transactionReferenceNumber = null,
            maskedAccountIdentifier = null,
            dedupHash = dedupHash,
            androidSmsRowId = null,
            rawSmsExcerpt = null,
            parserVersion = 1,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
            isDeleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return transactionDao.insert(entity)
    }

    // --- SMS INGESTION & DEDUPLICATION ---

    suspend fun processLiveSms(input: RawSmsInput): LiveSmsResult {
        return processSmsInternal(input, TransactionSource.SMS_LIVE)
    }

    suspend fun processHistoricalSms(input: RawSmsInput): LiveSmsResult {
        return processSmsInternal(input, TransactionSource.SMS_HISTORICAL)
    }

    private suspend fun processSmsInternal(input: RawSmsInput, source: TransactionSource): LiveSmsResult {
        // Step 1: Run Stage 3 SmsEngine
        val parseResult = SmsEngine.parse(input)

        if (parseResult.status == ParseStatus.IGNORE) {
            recordAuditIfPossible(
                input = input,
                isFinancial = false,
                rejectionReason = parseResult.rejectionReason,
                matchedTxnId = null
            )
            return LiveSmsResult.Ignored(parseResult.rejectionReason)
        }

        val amount = parseResult.amount ?: return LiveSmsResult.Ignored("NO_AMOUNT")
        val type = parseResult.transactionType ?: return LiveSmsResult.Ignored("NO_TRANSACTION_TYPE")
        val merchant = parseResult.merchant ?: "Unknown"
        val dedupHash = parseResult.dedupHash ?: return LiveSmsResult.Ignored("NO_DEDUP_HASH")

        // Step 2: Layer 1 Deduplication (Android SMS provider row ID if historical)
        if (input.androidSmsRowId != null) {
            val existingByRow = transactionDao.findByAndroidSmsRowId(input.androidSmsRowId)
            if (existingByRow != null) {
                return LiveSmsResult.Duplicate(existingByRow.id)
            }
        }

        // Step 3: Layer 2 Deduplication (dedup_hash uniqueness lookup)
        val existingByHash = transactionDao.findByDedupHash(dedupHash)
        if (existingByHash != null) {
            // Cross-pipeline synchronization: if transaction was recorded live without row ID,
            // update it with the historical row ID
            if (existingByHash.androidSmsRowId == null && input.androidSmsRowId != null) {
                transactionDao.update(existingByHash.copy(androidSmsRowId = input.androidSmsRowId))
            }
            return LiveSmsResult.Duplicate(existingByHash.id)
        }

        // Step 4: Layer 3 Deduplication (SMS audit fingerprint lookup)
        val normalizedBody = input.body.trim().lowercase().replace(Regex("\\s+"), " ")
        val bodyDigest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedBody.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)

        val fingerprint = "LIVE|${input.sender}|${input.smsTimestamp}|$bodyDigest"
        if (smsAuditDao != null) {
            val existingAudit = smsAuditDao.findByFingerprint(fingerprint)
            if (existingAudit?.matchedTransactionId != null) {
                return LiveSmsResult.Duplicate(existingAudit.matchedTransactionId)
            }
        }

        // Step 5: Deterministic Account Resolution
        val resolvedAccountId = resolveAccount(parseResult.maskedAccount)

        // Step 6: Map to TransactionEntity
        val reviewStatus = if (parseResult.status == ParseStatus.PENDING_REVIEW) {
            ReviewStatus.PENDING_REVIEW
        } else {
            ReviewStatus.CONFIRMED
        }

        val entity = TransactionEntity(
            amount = amount,
            transactionType = type,
            merchant = merchant,
            categoryId = null,
            accountId = resolvedAccountId,
            transferTargetAccountId = null,
            occurredTimestamp = parseResult.occurredTimestamp ?: input.smsTimestamp,
            notes = null,
            source = source,
            reviewStatus = reviewStatus,
            smsSender = input.sender,
            smsTimestamp = input.smsTimestamp,
            transactionReferenceNumber = parseResult.referenceNumber,
            maskedAccountIdentifier = parseResult.maskedAccount,
            dedupHash = dedupHash,
            androidSmsRowId = input.androidSmsRowId,
            rawSmsExcerpt = parseResult.rawSmsExcerpt, // Non-null ONLY for PENDING_REVIEW
            parserVersion = parseResult.parserVersion,
            isDeleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Step 7: Atomic insertion into Room
        val txnId: Long = try {
            transactionDao.insert(entity)
        } catch (e: Exception) {
            val existing = transactionDao.findByDedupHash(dedupHash)
            if (existing != null) {
                return LiveSmsResult.Duplicate(existing.id)
            } else {
                return LiveSmsResult.Failed(e.message ?: "Constraint failure during insert")
            }
        }

        // Step 8: Persist provenance in sms_audits
        recordAuditIfPossible(
            input = input,
            isFinancial = true,
            rejectionReason = null,
            matchedTxnId = txnId,
            customFingerprint = fingerprint
        )

        // Step 9: Notify active UI listeners
        _liveTransactionEvents.tryEmit(entity.copy(id = txnId))

        return LiveSmsResult.Persisted(txnId, reviewStatus)
    }

    private suspend fun resolveAccount(maskedAccount: String?): Long? {
        if (maskedAccount.isNullOrEmpty() || accountDao == null) return null
        val digits = maskedAccount.replace(Regex("[^0-9]"), "")
        if (digits.length < 3) return null
        val match = accountDao.findByMaskedNumber(digits)
        return match?.id
    }

    private suspend fun recordAuditIfPossible(
        input: RawSmsInput,
        isFinancial: Boolean,
        rejectionReason: String?,
        matchedTxnId: Long?,
        customFingerprint: String? = null
    ) {
        if (smsAuditDao == null) return
        val normalizedBody = input.body.trim().lowercase().replace(Regex("\\s+"), " ")
        val bodyDigest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedBody.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)
        val fp = customFingerprint ?: "AUDIT|${input.sender}|${input.smsTimestamp}|$bodyDigest"
        try {
            smsAuditDao.insert(
                SmsAuditEntity(
                    androidSmsRowId = input.androidSmsRowId,
                    sender = input.sender,
                    smsTimestamp = input.smsTimestamp,
                    processedTimestamp = System.currentTimeMillis(),
                    isFinancial = isFinancial,
                    rejectionReason = rejectionReason,
                    matchedTransactionId = matchedTxnId,
                    fingerprint = fp
                )
            )
        } catch (_: Exception) {
            // Ignore audit insertion conflict
        }
    }

    // --- STAGE 9 DASHBOARD & ANALYTICS EXTENSIONS ---

    fun getDashboardSummary(
        cycle: FinancialCycle,
        budgetDao: com.spendora.data.dao.BudgetDao? = null
    ): Flow<DashboardSummary> {
        val expensesFlow = transactionDao.getTotalExpensesBetween(cycle.startTimestamp, cycle.endTimestamp)
        val incomeFlow = transactionDao.getTotalIncomeBetween(cycle.startTimestamp, cycle.endTimestamp)
        val refundsFlow = transactionDao.getTotalRefundsBetween(cycle.startTimestamp, cycle.endTimestamp)
        val transfersFlow = transactionDao.getTotalTransfersBetween(cycle.startTimestamp, cycle.endTimestamp)
        val cashWithdrawalsFlow = transactionDao.getTotalCashWithdrawalsBetween(cycle.startTimestamp, cycle.endTimestamp)
        val txnCountFlow = transactionDao.getTransactionCountBetween(cycle.startTimestamp, cycle.endTimestamp)
        val pendingCountFlow = transactionDao.getPendingReviewCount()
        val budgetsFlow = if (budgetDao != null) budgetDao.getBudgetsForCycle(cycle.cycleYearMonth) else flowOf(emptyList())

        return combine(
            expensesFlow,
            incomeFlow,
            refundsFlow,
            transfersFlow,
            cashWithdrawalsFlow,
            txnCountFlow,
            pendingCountFlow,
            budgetsFlow
        ) { exp, inc, ref, trf, csh, count, pending, budgets ->
            val netSpending = (exp - ref).coerceAtLeast(0.0)
            val netFlow = (inc + ref) - (exp + csh)

            // Cycle progress
            val now = System.currentTimeMillis()
            val totalDuration = (cycle.endTimestamp - cycle.startTimestamp).coerceAtLeast(1L)
            val elapsedMillis = (now - cycle.startTimestamp).coerceIn(0L, totalDuration)
            val progressFraction = (elapsedMillis.toDouble() / totalDuration.toDouble()).toFloat()

            val nowLocal = java.time.LocalDate.now()
            val daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(cycle.startDate, nowLocal).toInt().coerceAtLeast(0)
            val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(nowLocal, cycle.endDate).toInt().coerceAtLeast(0)

            val totalBudget = budgets.sumOf { it.amount }

            DashboardSummary(
                cycle = cycle,
                totalSpending = netSpending,
                totalIncome = inc,
                totalRefunds = ref,
                totalTransfers = trf,
                totalCashWithdrawals = csh,
                netCashFlow = netFlow,
                confirmedTransactionCount = count,
                pendingReviewCount = pending,
                activeBudgetCount = budgets.size,
                totalBudgetAmount = totalBudget,
                totalBudgetSpent = netSpending,
                cycleProgressFraction = progressFraction,
                daysElapsed = daysElapsed,
                daysRemaining = daysRemaining
            )
        }
    }

    fun getCategorySpendingItems(
        cycle: FinancialCycle,
        categoryDao: com.spendora.data.dao.CategoryDao? = null
    ): Flow<List<CategorySpendingItem>> {
        val summariesFlow = transactionDao.getCategorySpendingSummaryBetween(cycle.startTimestamp, cycle.endTimestamp)
        val categoriesFlow = if (categoryDao != null) categoryDao.getAllActive() else flowOf(emptyList())

        return combine(summariesFlow, categoriesFlow) { summaries, categories ->
            val catMap = categories.associateBy { it.id }
            val totalNet = summaries.sumOf { it.netAmount }.coerceAtLeast(0.0)

            summaries.filter { it.netAmount > 0.0 }.map { s ->
                val cat = s.categoryId?.let { catMap[it] }
                val name = cat?.name ?: "Uncategorized"
                val color = cat?.colorHex ?: "#9E9E9E"
                val icon = cat?.icon ?: "category"
                val pct = if (totalNet > 0.0) (s.netAmount / totalNet) * 100.0 else 0.0

                CategorySpendingItem(
                    categoryId = s.categoryId,
                    categoryName = name,
                    colorHex = color,
                    icon = icon,
                    expenseAmount = s.expenseAmount,
                    refundAmount = s.refundAmount,
                    netAmount = s.netAmount,
                    percentageOfTotal = pct,
                    transactionCount = s.transactionCount
                )
            }
        }
    }

    fun getCycleComparison(
        currentCycle: FinancialCycle,
        previousCycle: FinancialCycle
    ): Flow<CycleComparison> {
        val currentExp = transactionDao.getTotalExpensesBetween(currentCycle.startTimestamp, currentCycle.endTimestamp)
        val currentRef = transactionDao.getTotalRefundsBetween(currentCycle.startTimestamp, currentCycle.endTimestamp)
        val prevExp = transactionDao.getTotalExpensesBetween(previousCycle.startTimestamp, previousCycle.endTimestamp)
        val prevRef = transactionDao.getTotalRefundsBetween(previousCycle.startTimestamp, previousCycle.endTimestamp)

        return combine(currentExp, currentRef, prevExp, prevRef) { cExp, cRef, pExp, pRef ->
            val curSpending = (cExp - cRef).coerceAtLeast(0.0)
            val prevSpending = (pExp - pRef).coerceAtLeast(0.0)
            val diff = curSpending - prevSpending
            val pctChange = if (prevSpending > 0.0) {
                (diff / prevSpending) * 100.0
            } else null

            CycleComparison(
                currentSpending = curSpending,
                previousSpending = prevSpending,
                absoluteDifference = diff,
                percentageChange = pctChange,
                hasPreviousData = prevSpending > 0.0
            )
        }
    }

    fun getAccountSpendingItems(
        cycle: FinancialCycle,
        accountDao: com.spendora.data.dao.AccountDao? = null
    ): Flow<List<AccountSpendingItem>> {
        val summariesFlow = transactionDao.getAccountSpendingSummaryBetween(cycle.startTimestamp, cycle.endTimestamp)
        val accountsFlow = if (accountDao != null) accountDao.getAll() else flowOf(emptyList())

        return combine(summariesFlow, accountsFlow) { summaries, accounts ->
            val accMap = accounts.associateBy { it.id }
            summaries.map { s ->
                val acc = s.accountId?.let { accMap[it] }
                val name = acc?.name ?: "Unlinked Account"
                val masked = acc?.maskedNumber?.let { "•••• $it" }

                AccountSpendingItem(
                    accountId = s.accountId,
                    accountName = name,
                    maskedNumber = masked,
                    totalSpent = s.totalSpent,
                    totalIncome = s.totalIncome,
                    transactionCount = s.transactionCount
                )
            }
        }
    }

    fun getTransactionTypeBreakdown(cycle: FinancialCycle): Flow<List<TransactionTypeItem>> {
        return transactionDao.getTransactionTypeBreakdownBetween(cycle.startTimestamp, cycle.endTimestamp).map { list ->
            list.map { b ->
                TransactionTypeItem(
                    type = b.transactionType,
                    totalAmount = b.totalAmount,
                    count = b.transactionCount
                )
            }
        }
    }


    fun getDailySpendingTrend(startTime: Long, endTime: Long): Flow<List<SpendingTrendPoint>> {
        return transactionDao.getDailySpendingTrendBetween(startTime, endTime).map { rows ->
            rows.map { r ->
                SpendingTrendPoint(
                    label = r.dateString,
                    netSpending = r.netSpending,
                    expenseAmount = r.expenseAmount,
                    refundAmount = r.refundAmount,
                    count = r.count
                )
            }
        }
    }

    fun getMonthlySpendingTrend(startTime: Long, endTime: Long): Flow<List<SpendingTrendPoint>> {
        return transactionDao.getMonthlySpendingTrendBetween(startTime, endTime).map { rows ->
            rows.map { r ->
                SpendingTrendPoint(
                    label = r.monthString,
                    netSpending = r.netSpending,
                    expenseAmount = r.expenseAmount,
                    refundAmount = r.refundAmount,
                    count = r.count
                )
            }
        }
    }

    fun getSpendingTrendForTimeRange(timeRange: AnalyticsTimeRange): Flow<List<SpendingTrendPoint>> {
        return if (timeRange.isMultiCycle) {
            getMonthlySpendingTrend(timeRange.startTimestamp, timeRange.endTimestamp)
        } else {
            getDailySpendingTrend(timeRange.startTimestamp, timeRange.endTimestamp)
        }
    }


    suspend fun updateTransactionLocation(
        id: Long,
        latitude: Double,
        longitude: Double,
        locationLabel: String? = null
    ): Boolean {
        val existing = transactionDao.getById(id) ?: return false
        val updated = existing.copy(
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

    suspend fun removeTransactionLocation(id: Long): Boolean {
        val existing = transactionDao.getById(id) ?: return false
        val updated = existing.copy(
            latitude = null,
            longitude = null,
            locationLabel = null,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
        return true
    }

}
