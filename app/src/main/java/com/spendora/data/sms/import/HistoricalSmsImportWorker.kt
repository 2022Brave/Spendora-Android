package com.spendora.data.sms.import

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.repository.TransactionRepository

/**
 * HistoricalSmsImportWorker
 *
 * Dedicated WorkManager CoroutineWorker executing historical SMS import in the background.
 * Unaffected by Activity lifecycle, screen locks, or UI destruction.
 */
class HistoricalSmsImportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_START_TIMESTAMP = "start_timestamp"
        const val KEY_END_TIMESTAMP = "end_timestamp"
        const val KEY_TOTAL_EXAMINED = "total_examined"
        const val KEY_IMPORTED_COUNT = "imported_count"
        const val KEY_DUPLICATE_COUNT = "duplicate_count"
        const val KEY_IGNORED_COUNT = "ignored_count"
        const val KEY_PENDING_REVIEW_COUNT = "pending_review_count"
        const val KEY_STATE = "import_state"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val UNIQUE_WORK_NAME = "spendora_historical_sms_import"
    }

    override suspend fun doWork(): Result {
        val startTimestamp = inputData.getLong(KEY_START_TIMESTAMP, -1L)
        val endTimestamp = inputData.getLong(KEY_END_TIMESTAMP, System.currentTimeMillis())

        if (startTimestamp <= 0L || startTimestamp > endTimestamp) {
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Invalid historical date range")
            )
        }

        val db = SpendoraDatabase.getInstance(applicationContext)
        val repo = TransactionRepository(db.transactionDao(), db.smsAuditDao(), db.accountDao())
        val importer = HistoricalSmsImporter(applicationContext, repo)

        return try {
            val finalProgress = importer.importRange(startTimestamp, endTimestamp) { progress ->
                setProgress(
                    workDataOf(
                        KEY_STATE to progress.state.name,
                        KEY_TOTAL_EXAMINED to progress.totalExamined,
                        KEY_IMPORTED_COUNT to progress.importedCount,
                        KEY_DUPLICATE_COUNT to progress.duplicateCount,
                        KEY_IGNORED_COUNT to progress.ignoredCount,
                        KEY_PENDING_REVIEW_COUNT to progress.pendingReviewCount
                    )
                )
            }

            if (finalProgress.state == ImportState.COMPLETED) {
                Result.success(
                    workDataOf(
                        KEY_STATE to finalProgress.state.name,
                        KEY_TOTAL_EXAMINED to finalProgress.totalExamined,
                        KEY_IMPORTED_COUNT to finalProgress.importedCount,
                        KEY_DUPLICATE_COUNT to finalProgress.duplicateCount,
                        KEY_IGNORED_COUNT to finalProgress.ignoredCount,
                        KEY_PENDING_REVIEW_COUNT to finalProgress.pendingReviewCount
                    )
                )
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to (finalProgress.errorMessage ?: "Import failed"))
                )
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Worker execution error"))
            )
        }
    }
}
