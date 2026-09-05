package com.spendora.data.sms.import

enum class ImportState {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Immutable snapshot of historical SMS import progress.
 */
data class HistoricalImportProgress(
    val state: ImportState = ImportState.IDLE,
    val totalExamined: Int = 0,
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val ignoredCount: Int = 0,
    val pendingReviewCount: Int = 0,
    val errorMessage: String? = null
)
