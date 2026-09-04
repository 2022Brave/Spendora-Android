package com.spendora.data.sms.parser

import java.util.regex.Pattern

data class ParsedAmounts(
    val transactionAmount: Double?,
    val balanceAmount: Double?,
    val currency: String = "INR"
)

object SmsAmountExtractor {

    // Matches monetary amounts with Indian or international comma formatting
    // e.g. INR 1,25,000.50, Rs. 500, ₹500.00, Rs 1200
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:inr|rs\\.?|₹)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    )

    private val ACTION_AMOUNT_PATTERN = Pattern.compile(
        "(?i)\\b(?:debited\\s*(?:by|for)?|credited\\s*(?:with|by)?|spent|paid|withdrawn)\\s*(?:inr|rs\\.?|₹)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    )

    // Balance context indicators to prevent confusing account balance with transaction amount
    private val BALANCE_CONTEXT_PATTERN = Pattern.compile(
        "(?i)\\b(bal(?:ance)?|avl\\s*bal|avail(?:able)?\\s*bal(?:ance)?|total\\s*bal|clear\\s*bal|limit)\\b[:\\s]*(?:is)?[:\\s]*(?:inr|rs\\.?|₹)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    )

    fun extractAmounts(body: String): ParsedAmounts {
        var balance: Double? = null
        val balMatcher = BALANCE_CONTEXT_PATTERN.matcher(body)
        var balanceRangeStart = -1
        var balanceRangeEnd = -1

        if (balMatcher.find()) {
            val balStr = balMatcher.group(2)?.replace(",", "")
            balance = balStr?.toDoubleOrNull()
            balanceRangeStart = balMatcher.start()
            balanceRangeEnd = balMatcher.end()
        }

        val candidateAmounts = mutableListOf<Pair<Double, Int>>() // Pair(amount, matchStartIndex)

        // First, check explicit action-amount pattern (e.g. "debited by 450.0")
        val actionMatcher = ACTION_AMOUNT_PATTERN.matcher(body)
        while (actionMatcher.find()) {
            val startIdx = actionMatcher.start()
            if (balanceRangeStart != -1 && startIdx >= balanceRangeStart && startIdx <= balanceRangeEnd) {
                continue
            }
            val amtStr = actionMatcher.group(1)?.replace(",", "")
            val amt = amtStr?.toDoubleOrNull()
            if (amt != null && amt > 0.0) {
                candidateAmounts.add(Pair(amt, startIdx))
            }
        }

        // Then check general currency-prefixed pattern
        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        while (amountMatcher.find()) {
            val startIdx = amountMatcher.start()
            if (balanceRangeStart != -1 && startIdx >= balanceRangeStart && startIdx <= balanceRangeEnd) {
                continue
            }
            val amtStr = amountMatcher.group(1)?.replace(",", "")
            val amt = amtStr?.toDoubleOrNull()
            if (amt != null && amt > 0.0) {
                candidateAmounts.add(Pair(amt, startIdx))
            }
        }

        // The transaction amount is typically the first monetary amount mentioned in the SMS
        val txnAmount = candidateAmounts.firstOrNull()?.first

        return ParsedAmounts(
            transactionAmount = txnAmount,
            balanceAmount = balance,
            currency = "INR"
        )
    }
}
