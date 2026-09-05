package com.spendora.data.sms

import com.spendora.data.sms.classifier.SmsEligibilityClassifier
import com.spendora.data.sms.model.ConfidenceLevel
import com.spendora.data.sms.model.ParseStatus
import com.spendora.data.sms.model.RawSmsInput
import com.spendora.data.sms.model.SmsParseResult
import com.spendora.data.sms.parser.*
import java.time.ZoneId

/**
 * SmsEngine
 *
 * Production-grade, deterministic SMS processing engine for SPENDORA.
 * Serves as the single unified processing pipeline for both live SMS broadcast reception
 * and user-initiated historical SMS scanning.
 *
 * Privacy Invariants:
 * - 100% offline, local on-device operation (no internet, no remote APIs, no telemetry).
 * - Full raw SMS bodies are never permanently retained.
 * - rawSmsExcerpt is strictly populated only when status == PENDING_REVIEW.
 */
object SmsEngine {

    const val ENGINE_VERSION = 1

    fun parse(
        input: RawSmsInput,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SmsParseResult {
        // Step 1: Normalize input
        val cleanBody = input.body.trim()
        val cleanSender = input.sender.trim()

        if (cleanBody.isEmpty()) {
            return SmsParseResult(
                status = ParseStatus.IGNORE,
                rejectionReason = "EMPTY_BODY",
                parserVersion = ENGINE_VERSION
            )
        }

        // Step 2: Screen via Eligibility Classifier (OTPs, Promotions, Failed Alerts, Non-Financial)
        val eligibility = SmsEligibilityClassifier.classify(input.copy(body = cleanBody, sender = cleanSender))
        if (!eligibility.isEligible) {
            return SmsParseResult(
                status = ParseStatus.IGNORE,
                rejectionReason = eligibility.rejectionReason,
                evidenceTags = eligibility.tags,
                parserVersion = ENGINE_VERSION
            )
        }

        // Step 3: Extract Monetary Amounts & Account Balance
        val amounts = SmsAmountExtractor.extractAmounts(cleanBody)
        val txnAmount = amounts.transactionAmount

        if (txnAmount == null || txnAmount <= 0.0) {
            return SmsParseResult(
                status = ParseStatus.IGNORE,
                rejectionReason = "NO_VALID_TRANSACTION_AMOUNT",
                parserVersion = ENGINE_VERSION
            )
        }

        // Step 4: Classify Transaction Type (EXPENSE, INCOME, TRANSFER, REFUND, CASH_WITHDRAWAL)
        val txnType = SmsTypeClassifier.classifyType(cleanBody)

        // Step 5: Extract Merchant / Payee
        val merchant = SmsMerchantExtractor.extractMerchant(cleanBody)

        // Step 6: Extract Masked Account / Card Ending
        val maskedAccount = SmsAccountExtractor.extractMaskedAccount(cleanBody)

        // Step 7: Extract Reference / UTR Number
        val refNumber = SmsReferenceExtractor.extractReference(cleanBody)

        // Step 8: Determine Timestamp (preserving supplied SMS timestamp)
        val occurredTimestamp = SmsDateExtractor.determineTimestamp(cleanBody, input.smsTimestamp, zoneId)

        // Step 9: Score Confidence & Determine Final Routing Status
        val scoring = SmsConfidenceScorer.score(
            sender = cleanSender,
            body = cleanBody,
            type = txnType,
            amount = txnAmount,
            merchant = merchant,
            account = maskedAccount,
            refNumber = refNumber,
            timestamp = occurredTimestamp
        )

        // Step 10: Enforce Privacy Invariant on Raw Excerpt
        val rawExcerpt = if (scoring.status == ParseStatus.PENDING_REVIEW) {
            cleanBody.take(160)
        } else {
            null
        }

        return SmsParseResult(
            status = scoring.status,
            rejectionReason = null,
            transactionType = txnType,
            amount = txnAmount,
            currency = amounts.currency,
            merchant = merchant,
            referenceNumber = refNumber,
            maskedAccount = maskedAccount,
            occurredTimestamp = occurredTimestamp,
            balance = amounts.balanceAmount,
            confidence = scoring.score,
            confidenceLevel = scoring.level,
            evidenceTags = scoring.tags,
            rawSmsExcerpt = rawExcerpt,
            dedupHash = scoring.dedupHash,
            parserVersion = ENGINE_VERSION
        )
    }
}
