package com.spendora.data.dao

import androidx.room.*
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategorySpending(
    val categoryId: Long?,
    val totalAmount: Double,
    val transactionCount: Int
)

data class CategorySpendingSummary(
    val categoryId: Long?,
    val expenseAmount: Double,
    val refundAmount: Double,
    val netAmount: Double,
    val transactionCount: Int
)

data class AccountSpendingSummary(
    val accountId: Long?,
    val totalSpent: Double,
    val totalIncome: Double,
    val transactionCount: Int
)

data class DailySpendingTrendRow(
    val dateString: String,
    val expenseAmount: Double,
    val refundAmount: Double,
    val netSpending: Double,
    val count: Int
)

data class MonthlySpendingTrendRow(
    val monthString: String,
    val expenseAmount: Double,
    val refundAmount: Double,
    val netSpending: Double,
    val count: Int
)

data class TransactionTypeBreakdown(
    val transactionType: TransactionType,
    val totalAmount: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED' ORDER BY occurred_timestamp DESC, id DESC")
    fun getAllActive(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED' ORDER BY occurred_timestamp DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED' AND occurred_timestamp BETWEEN :startTime AND :endTime ORDER BY occurred_timestamp DESC, id DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED' AND transaction_type = :type AND occurred_timestamp BETWEEN :startTime AND :endTime ORDER BY occurred_timestamp DESC, id DESC")
    fun getTransactionsByTypeBetween(type: TransactionType, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = :status ORDER BY occurred_timestamp DESC, id DESC")
    fun getTransactionsByReviewStatus(status: ReviewStatus): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 AND review_status = 'PENDING_REVIEW' ORDER BY occurred_timestamp DESC, id DESC")
    fun getPendingReviewTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED'")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0 AND review_status = 'CONFIRMED' AND occurred_timestamp BETWEEN :startTime AND :endTime")
    fun getTransactionCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0 AND review_status = 'PENDING_REVIEW'")
    fun getPendingReviewCount(): Flow<Int>

    // Room-backed Search and Filter Query (scalable for 10,000+ transactions)
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND (:searchQuery IS NULL OR merchant LIKE % || :searchQuery || % OR transaction_reference_number LIKE % || :searchQuery || % OR notes LIKE % || :searchQuery || %)
          AND (:type IS NULL OR transaction_type = :type)
          AND (:categoryId IS NULL OR category_id = :categoryId)
          AND (:accountId IS NULL OR account_id = :accountId)
          AND (:source IS NULL OR source = :source)
          AND (:startTime IS NULL OR occurred_timestamp >= :startTime)
          AND (:endTime IS NULL OR occurred_timestamp <= :endTime)
        ORDER BY occurred_timestamp DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    fun filterTransactions(
        searchQuery: String?,
        type: TransactionType?,
        categoryId: Long?,
        accountId: Long?,
        source: TransactionSource?,
        startTime: Long?,
        endTime: Long?,
        limit: Int,
        offset: Int
    ): Flow<List<TransactionEntity>>

    // Deduplication lookups
    @Query("SELECT * FROM transactions WHERE dedup_hash = :hash AND is_deleted = 0 LIMIT 1")
    suspend fun findByDedupHash(hash: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transaction_reference_number = :refNumber AND is_deleted = 0 LIMIT 1")
    suspend fun findByReferenceNumber(refNumber: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE android_sms_row_id = :rowId AND is_deleted = 0 LIMIT 1")
    suspend fun findByAndroidSmsRowId(rowId: Long): TransactionEntity?

    // Financial cycle aggregations - ONLY CONFIRMED transactions participate
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND transaction_type = EXPENSE 
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalExpensesBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND transaction_type = INCOME 
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND transaction_type = REFUND 
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalRefundsBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND transaction_type = TRANSFER 
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalTransfersBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE is_deleted = 0 
          AND review_status = CONFIRMED
          AND transaction_type = CASH_WITHDRAWAL 
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalCashWithdrawalsBetween(startTime: Long, endTime: Long): Flow<Double>

    // Category spending aggregation (net = expenses - refunds)
    @Query("""
        SELECT 
            category_id AS categoryId,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount ELSE 0.0 END), 0.0) AS expenseAmount,
            COALESCE(SUM(CASE WHEN transaction_type = REFUND THEN amount ELSE 0.0 END), 0.0) AS refundAmount,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount WHEN transaction_type = REFUND THEN -amount ELSE 0.0 END), 0.0) AS netAmount,
            COUNT(*) AS transactionCount
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND transaction_type IN (EXPENSE, REFUND)
          AND occurred_timestamp BETWEEN :startTime AND :endTime
        GROUP BY category_id
        ORDER BY netAmount DESC
    """)
    fun getCategorySpendingSummaryBetween(startTime: Long, endTime: Long): Flow<List<CategorySpendingSummary>>

    // Account spending & income aggregation
    @Query("""
        SELECT 
            account_id AS accountId,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount WHEN transaction_type = REFUND THEN -amount ELSE 0.0 END), 0.0) AS totalSpent,
            COALESCE(SUM(CASE WHEN transaction_type = INCOME THEN amount ELSE 0.0 END), 0.0) AS totalIncome,
            COUNT(*) AS transactionCount
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND transaction_type IN (EXPENSE, INCOME, REFUND)
          AND occurred_timestamp BETWEEN :startTime AND :endTime
        GROUP BY account_id
        ORDER BY totalSpent DESC
    """)
    fun getAccountSpendingSummaryBetween(startTime: Long, endTime: Long): Flow<List<AccountSpendingSummary>>

    // Transaction type breakdown
    @Query("""
        SELECT 
            transaction_type AS transactionType,
            COALESCE(SUM(amount), 0.0) AS totalAmount,
            COUNT(*) AS transactionCount
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND occurred_timestamp BETWEEN :startTime AND :endTime
        GROUP BY transaction_type
        ORDER BY totalAmount DESC
    """)
    fun getTransactionTypeBreakdownBetween(startTime: Long, endTime: Long): Flow<List<TransactionTypeBreakdown>>

    // Budget category net spending
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount WHEN transaction_type = REFUND THEN -amount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND category_id = :categoryId
          AND transaction_type IN (EXPENSE, REFUND)
          AND occurred_timestamp BETWEEN :startTime AND :endTime
    """)
    fun getBudgetSpendingForCategoryBetween(categoryId: Long, startTime: Long, endTime: Long): Flow<Double>

    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET raw_sms_excerpt = NULL, updated_at = :now WHERE id = :id")
    suspend fun clearRawSms(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET review_status = CONFIRMED, raw_sms_excerpt = NULL, updated_at = :now WHERE id = :id")
    suspend fun confirmTransaction(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET review_status = IGNORED, raw_sms_excerpt = NULL, updated_at = :now WHERE id = :id")
    suspend fun ignoreTransaction(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // Daily spending trend query
    @Query("""
        SELECT 
            strftime(%Y-%m-%d, occurred_timestamp / 1000, unixepoch, localtime) AS dateString,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount ELSE 0.0 END), 0.0) AS expenseAmount,
            COALESCE(SUM(CASE WHEN transaction_type = REFUND THEN amount ELSE 0.0 END), 0.0) AS refundAmount,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount WHEN transaction_type = REFUND THEN -amount ELSE 0.0 END), 0.0) AS netSpending,
            COUNT(*) AS count
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND transaction_type IN (EXPENSE, REFUND)
          AND occurred_timestamp BETWEEN :startTime AND :endTime
        GROUP BY dateString
        ORDER BY dateString ASC
    """)
    fun getDailySpendingTrendBetween(startTime: Long, endTime: Long): Flow<List<DailySpendingTrendRow>>

    // Monthly / Multi-cycle spending trend query
    @Query("""
        SELECT 
            strftime(%Y-%m, occurred_timestamp / 1000, unixepoch, localtime) AS monthString,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount ELSE 0.0 END), 0.0) AS expenseAmount,
            COALESCE(SUM(CASE WHEN transaction_type = REFUND THEN amount ELSE 0.0 END), 0.0) AS refundAmount,
            COALESCE(SUM(CASE WHEN transaction_type = EXPENSE THEN amount WHEN transaction_type = REFUND THEN -amount ELSE 0.0 END), 0.0) AS netSpending,
            COUNT(*) AS count
        FROM transactions
        WHERE is_deleted = 0
          AND review_status = CONFIRMED
          AND transaction_type IN (EXPENSE, REFUND)
          AND occurred_timestamp BETWEEN :startTime AND :endTime
        GROUP BY monthString
        ORDER BY monthString ASC
    """)
    fun getMonthlySpendingTrendBetween(startTime: Long, endTime: Long): Flow<List<MonthlySpendingTrendRow>>

}
