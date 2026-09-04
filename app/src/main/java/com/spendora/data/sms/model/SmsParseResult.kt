package com.spendora.data.sms.model

import com.spendora.data.model.TransactionType

enum class ParseStatus {
    PARSED_TRANSACTION, // High-confidence transaction ready for auto-confirmation
    PENDING_REVIEW,     // Plausible transaction needing user verification
    IGNORE              // Non-financial, promotional, OTP, or duplicate message
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Structured output produced by the SMS engine.
 *
 * Privacy invariant:
 * rawSmsExcerpt is strictly populated ONLY when status == PENDING_REVIEW.
 * For confirmed transactions, raw text is discarded to preserve on-device privacy.
 */
data class SmsParseResult(
    val status: ParseStatus,
    val rejectionReason: String? = null,
    val transactionType: TransactionType? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val referenceNumber: String? = null,
    val maskedAccount: String? = null,
    val occurredTimestamp: Long? = null,
    val balance: Double? = null,
    val confidence: Double = 0.0,
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW,
    val evidenceTags: List<String> = emptyList(),
    val rawSmsExcerpt: String? = null,
    val dedupHash: String? = null,
    val parserVersion: Int = 1
)
