package com.spendora.data.sms.classifier

import com.spendora.data.sms.model.RawSmsInput
import java.util.regex.Pattern

data class EligibilityResult(
    val isEligible: Boolean,
    val rejectionReason: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * SmsEligibilityClassifier
 *
 * Deterministic first-stage filter that screens incoming and historical SMS.
 * Aggressively eliminates OTPs, promotional spam, loan ads, failed alerts,
 * and pure balance enquiries without calling any external network or AI model.
 */
object SmsEligibilityClassifier {

    // 1. Strict OTP & Authentication Rejection Patterns
    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one\\s*time\\s*password|verification\\s*code|security\\s*code|login\\s*code|secret\\s*code)\\b"),
        Pattern.compile("(?i)\\b(is\\s*your\\s*otp|use\\s*\\d{4,8}\\s*to\\s*verify|valid\\s*for\\s*\\d+\\s*(?:mins?|minutes?)|do\\s*not\\s*share\\s*(?:this)?\\s*otp)\\b"),
        Pattern.compile("(?i)\\b(never\\s*share\\s*your\\s*otp|authentication\\s*code|cvv|mpin\\b)"),
        Pattern.compile("(?i)\\b(enter\\s*code\\s*\\d{4,8}\\b|authorization\\s*code\\b)")
    )

    // 2. Promotional, Loan & Marketing Ad Patterns
    private val PROMO_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(pre-?approved\\s*(?:personal)?\\s*loan|instant\\s*loan|apply\\s*for\\s*loan|credit\\s*card\\s*limit\\s*increase)\\b"),
        Pattern.compile("(?i)\\b(congratulations!?\\s*(?:you\\s*are\\s*eligible)?|special\\s*offer|limited\\s*period\\s*offer|exclusive\\s*deal)\\b"),
        Pattern.compile("(?i)\\b(apply\\s*now|click\\s*here\\s*to\\s*(?:avail|apply|claim)|call\\s*now\\s*to\\s*avail|claim\\s*now)\\b"),
        Pattern.compile("(?i)\\b(flat\\s*\\d+%/\\s*off|discount\\s*on|shop\\s*now\\s*and\\s*get|save\\s*up\\s*to)\\b"),
        Pattern.compile("(?i)\\b(get\\s*(?:up\\s*to\\s*)?(?:inr|rs\\.?|₹)\\s*[\\d,]+(?:\\.\\d{1,2})?\\s*cashback\\b)"), // "Get ₹500 cashback"
        Pattern.compile("(?i)\\b(avail\\s*(?:personal)?\\s*loan|pre-?qualified|insurance\\s*cover\\s*of|life\\s*insurance\\s*plan)\\b"),
        Pattern.compile("(?i)\\b(stand\\s*a\\s*chance\\s*to\\s*win|zero\\s*interest\\s*emi\\b)")
    )

    // 3. Failed & Non-Financial Action Patterns
    private val FAILED_OR_SERVICE_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(declined\\s*(?:due\\s*to|for)?|transaction\\s*failed|payment\\s*failed|unsuccessful)\\b"),
        Pattern.compile("(?i)\\b(insufficient\\s*(?:funds|balance)|exceeded\\s*limit|incorrect\\s*pin)\\b"),
        Pattern.compile("(?i)\\b(e-?mandate\\s*(?:registered|created|modified)|standing\\s*instruction\\s*registered)\\b"),
        Pattern.compile("(?i)\\b(bill\\s*generated|statement\\s*has\\s*been\\s*sent|card\\s*dispatched|cheque\\s*book\\s*dispatched)\\b"),
        Pattern.compile("(?i)\\b(auto-?debit\\s*scheduled|payment\\s*due\\s*(?:date|on)|due\\s*by\\b)")
    )

    // 4. Financial Currency Indicator
    private val CURRENCY_PATTERN = Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*[\\d,]+(?:\\.\\d{1,2})?")

    // 5. Financial Action Verb Indicators
    private val FINANCIAL_VERBS = Pattern.compile(
        "(?i)\\b(debited|spent|paid|withdrawn|credited|received|transferred|transfer|sent|deposited|refunded|refund|reversed|reversal|cash\\s*withdrawal|atm\\s*withdrawal)\\b"
    )

    // 6. Balance-only enquiry pattern (no transactional action verb)
    private val BALANCE_ONLY_PATTERN = Pattern.compile(
        "(?i)^[^a-zA-Z]*(?:your)?\\s*(?:avail(?:able)?|clear|current)?\\s*bal(?:ance)?\\s*(?:is|in|of)?[:\\s]*(?:inr|rs\\.?|₹)?\\s*[\\d,]+(?:\\.\\d{1,2})?[^a-zA-Z]*$"
    )

    fun classify(input: RawSmsInput): EligibilityResult {
        val body = input.body.trim()
        if (body.isEmpty()) {
            return EligibilityResult(isEligible = false, rejectionReason = "EMPTY_BODY")
        }

        // 1. Reject OTPs
        for (pattern in OTP_PATTERNS) {
            if (pattern.matcher(body).find()) {
                return EligibilityResult(isEligible = false, rejectionReason = "OTP_OR_SECURITY_CODE", tags = listOf("OTP"))
            }
        }

        // 2. Reject Promotional Messages
        // Note exception: If cashback is explicitly CREDITED (e.g. "₹50 cashback credited to a/c"), it is financial!
        val isCashbackCredited = body.contains("cashback", ignoreCase = true) && 
                (body.contains("credited", ignoreCase = true) || body.contains("received", ignoreCase = true))

        if (!isCashbackCredited) {
            for (pattern in PROMO_PATTERNS) {
                if (pattern.matcher(body).find()) {
                    return EligibilityResult(isEligible = false, rejectionReason = "PROMOTIONAL_OR_MARKETING", tags = listOf("PROMOTION"))
                }
            }
        }

        // 3. Reject Failed Transactions or Informational Services
        for (pattern in FAILED_OR_SERVICE_PATTERNS) {
            if (pattern.matcher(body).find()) {
                return EligibilityResult(isEligible = false, rejectionReason = "FAILED_OR_SERVICE_ALERT", tags = listOf("NON_FINANCIAL"))
            }
        }

        // 4. Must contain a valid monetary amount
        if (!CURRENCY_PATTERN.matcher(body).find()) {
            return EligibilityResult(isEligible = false, rejectionReason = "NO_MONETARY_AMOUNT")
        }

        // 5. Must contain at least one financial movement verb
        if (!FINANCIAL_VERBS.matcher(body).find()) {
            return EligibilityResult(isEligible = false, rejectionReason = "NO_FINANCIAL_ACTION_VERB")
        }

        // 6. Check for pure balance enquiry
        if (BALANCE_ONLY_PATTERN.matcher(body).matches()) {
            return EligibilityResult(isEligible = false, rejectionReason = "BALANCE_ONLY_NOTIFICATION")
        }

        return EligibilityResult(
            isEligible = true,
            tags = listOf("ELIGIBLE_TRANSACTION")
        )
    }
}
