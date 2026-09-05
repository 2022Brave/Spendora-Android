package com.spendora.data.sms.parser

import com.spendora.data.model.TransactionType
import com.spendora.data.sms.model.ConfidenceLevel
import com.spendora.data.sms.model.ParseStatus
import java.security.MessageDigest

data class ConfidenceScore(
    val status: ParseStatus,
    val score: Double,
    val level: ConfidenceLevel,
    val tags: List<String>,
    val dedupHash: String?
)

object SmsConfidenceScorer {

    fun score(
        sender: String,
        body: String,
        type: TransactionType?,
        amount: Double?,
        merchant: String?,
        account: String?,
        refNumber: String?,
        timestamp: Long
    ): ConfidenceScore {
        val tags = mutableListOf<String>()
        var score = 0.0

        // 1. Amount evidence (+0.30)
        if (amount != null && amount > 0.0) {
            score += 0.30
            tags.add("EVIDENCE_AMOUNT")
        }

        // 2. Transaction Type / Verb (+0.25)
        if (type != null) {
            score += 0.25
            tags.add("EVIDENCE_TYPE_${type.name}")
        }

        // 3. Sender Header validation (+0.15)
        if (isRecognizedSender(sender)) {
            score += 0.15
            tags.add("EVIDENCE_SENDER_HEADER")
        }

        // 4. Merchant / Counterparty (+0.15)
        if (!merchant.isNullOrEmpty()) {
            score += 0.15
            tags.add("EVIDENCE_MERCHANT")
        }

        // 5. Account or Card mask (+0.10)
        if (!account.isNullOrEmpty()) {
            score += 0.10
            tags.add("EVIDENCE_ACCOUNT_MASK")
        }

        // 6. Transaction / Reference number (+0.10)
        if (!refNumber.isNullOrEmpty()) {
            score += 0.10
            tags.add("EVIDENCE_REFERENCE_NUMBER")
        }

        // Deductions for ambiguity
        // If it is a generic credit without merchant/payer and without salary keyword
        if (type == TransactionType.INCOME && merchant.isNullOrEmpty() && !body.contains("salary", ignoreCase = true)) {
            score -= 0.25
            tags.add("FLAG_AMBIGUOUS_CREDIT")
        }

        // If no merchant and no account for an expense
        if (type == TransactionType.EXPENSE && merchant.isNullOrEmpty() && account.isNullOrEmpty()) {
            score -= 0.20
            tags.add("FLAG_VAGUE_EXPENSE")
        }

        val clampedScore = score.coerceIn(0.0, 1.0)
        val level = when {
            clampedScore >= 0.70 -> ConfidenceLevel.HIGH
            clampedScore >= 0.40 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val status = when {
            amount == null || type == null -> ParseStatus.IGNORE
            level == ConfidenceLevel.HIGH -> ParseStatus.PARSED_TRANSACTION
            level == ConfidenceLevel.MEDIUM -> ParseStatus.PENDING_REVIEW
            else -> ParseStatus.IGNORE
        }

        // Compute dedupHash if eligible
        val dedupHash = if (status != ParseStatus.IGNORE && amount != null && type != null) {
            computeDedupHash(sender, timestamp, amount, account, merchant, refNumber, body)
        } else null

        return ConfidenceScore(
            status = status,
            score = clampedScore,
            level = level,
            tags = tags,
            dedupHash = dedupHash
        )
    }

    private fun isRecognizedSender(sender: String): Boolean {
        val s = sender.uppercase()
        // Common Indian 6-character alphanumeric sender format: XX-BANKID (e.g. VM-HDFCBK, AX-ICICIB)
        if (s.matches(Regex("^[A-Z]{2}-[A-Z0-9]{4,8}$"))) return true
        val financialKeywords = listOf("HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "YESB", "IDFC", "PAYTM", "GPAY", "PHONEPE", "CRED")
        return financialKeywords.any { s.contains(it) }
    }

    private fun computeDedupHash(
        sender: String,
        timestamp: Long,
        amount: Double,
        account: String?,
        merchant: String?,
        refNumber: String?,
        body: String
    ): String {
        val normalizedBody = body.trim().lowercase().replace(Regex("\\s+"), " ")
        val bodyDigest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedBody.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)

        val rawKey = if (!refNumber.isNullOrEmpty()) {
            "REF|$sender|${account ?: "UNKNOWN"}|$refNumber"
        } else {
            "SMS|$sender|$timestamp|$amount|${account ?: "UNKNOWN"}|${merchant ?: "UNKNOWN"}|$bodyDigest"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
