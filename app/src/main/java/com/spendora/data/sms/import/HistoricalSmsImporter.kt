package com.spendora.data.sms.import

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.spendora.data.model.ReviewStatus
import com.spendora.data.repository.LiveSmsResult
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.sms.model.RawSmsInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * HistoricalSmsImporter
 *
 * Queries Android Telephony Provider (content://sms/inbox) in chunked batches,
 * constructing RawSmsInput envelopes with provider _id as androidSmsRowId,
 * and dispatching to the existing Stage 3 SmsEngine via TransactionRepository.
 *
 * Invariants:
 * - Never loads the entire SMS database into memory.
 * - Streams rows via projecting only required columns: _ID, ADDRESS, BODY, DATE, TYPE.
 * - Cursor is guaranteed closed in finally block.
 * - Respects cooperative coroutine cancellation without database corruption.
 */
class HistoricalSmsImporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository
) {
    companion object {
        val INBOX_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI // content://sms/inbox
        val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        const val BATCH_SIZE = 50
    }

    suspend fun importRange(
        startTimestamp: Long,
        endTimestamp: Long,
        onProgress: suspend (HistoricalImportProgress) -> Unit = {}
    ): HistoricalImportProgress {
        var totalExamined = 0
        var importedCount = 0
        var duplicateCount = 0
        var ignoredCount = 0
        var pendingReviewCount = 0

        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val selectionArgs = arrayOf(startTimestamp.toString(), endTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        val contentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                INBOX_URI,
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor == null) {
                val failure = HistoricalImportProgress(
                    state = ImportState.FAILED,
                    errorMessage = "Failed to query SMS content provider: null cursor"
                )
                onProgress(failure)
                return failure
            }

            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            onProgress(
                HistoricalImportProgress(
                    state = ImportState.RUNNING,
                    totalExamined = 0,
                    importedCount = 0,
                    duplicateCount = 0,
                    ignoredCount = 0,
                    pendingReviewCount = 0
                )
            )

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()

                val rowId = cursor.getLong(idIndex)
                val address = cursor.getString(addressIndex) ?: "UNKNOWN"
                val body = cursor.getString(bodyIndex) ?: ""
                val date = cursor.getLong(dateIndex)

                totalExamined++

                if (body.isNotBlank()) {
                    val input = RawSmsInput(
                        sender = address,
                        body = body,
                        smsTimestamp = date,
                        androidSmsRowId = rowId
                    )

                    when (val result = transactionRepository.processHistoricalSms(input)) {
                        is LiveSmsResult.Persisted -> {
                            if (result.reviewStatus == ReviewStatus.PENDING_REVIEW) {
                                pendingReviewCount++
                            }
                            importedCount++
                        }
                        is LiveSmsResult.Duplicate -> {
                            duplicateCount++
                        }
                        is LiveSmsResult.Ignored -> {
                            ignoredCount++
                        }
                        is LiveSmsResult.Failed -> {
                            ignoredCount++
                        }
                    }
                } else {
                    ignoredCount++
                }

                if (totalExamined % BATCH_SIZE == 0) {
                    onProgress(
                        HistoricalImportProgress(
                            state = ImportState.RUNNING,
                            totalExamined = totalExamined,
                            importedCount = importedCount,
                            duplicateCount = duplicateCount,
                            ignoredCount = ignoredCount,
                            pendingReviewCount = pendingReviewCount
                        )
                    )
                }
            }

            val completed = HistoricalImportProgress(
                state = ImportState.COMPLETED,
                totalExamined = totalExamined,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                ignoredCount = ignoredCount,
                pendingReviewCount = pendingReviewCount
            )
            onProgress(completed)
            return completed

        } catch (e: CancellationException) {
            val cancelled = HistoricalImportProgress(
                state = ImportState.CANCELLED,
                totalExamined = totalExamined,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                ignoredCount = ignoredCount,
                pendingReviewCount = pendingReviewCount
            )
            onProgress(cancelled)
            throw e
        } catch (e: Exception) {
            val failed = HistoricalImportProgress(
                state = ImportState.FAILED,
                totalExamined = totalExamined,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                ignoredCount = ignoredCount,
                pendingReviewCount = pendingReviewCount,
                errorMessage = e.message ?: "Unknown provider error"
            )
            onProgress(failed)
            return failed
        } finally {
            cursor?.close()
        }
    }
}
