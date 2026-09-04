package com.spendora.data.sms.parser

import java.util.regex.Pattern

object SmsAccountExtractor {

    // Matches account or card indicators followed by account/card numbers or masked masks
    // Privacy guarantee: Always extracts only the trailing 3-4 digits, never full card numbers
    private val ACCOUNT_PATTERN = Pattern.compile(
        "(?i)(?:a/c|acct|account|credit\\s*card|debit\\s*card|card)\\s*(?:no\\.?|number|ending\\s*(?:in)?)?\\s*([0-9x*\\s-]{3,25})\\b"
    )

    fun extractMaskedAccount(body: String): String? {
        val matcher = ACCOUNT_PATTERN.matcher(body)
        if (matcher.find()) {
            val candidate = matcher.group(1) ?: return null
            val digits = candidate.replace(Regex("[^0-9]"), "")
            if (digits.length >= 3) {
                val last4 = digits.takeLast(4)
                return "XX$last4"
            }
        }
        return null
    }
}
