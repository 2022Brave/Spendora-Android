package com.spendora.data.sms.parser

import java.util.Locale
import java.util.regex.Pattern

object SmsMerchantExtractor {

    // Known common merchants for clean normalization
    private val KNOWN_MERCHANTS = mapOf(
        "swiggy" to "Swiggy",
        "zomato" to "Zomato",
        "amazon" to "Amazon",
        "flipkart" to "Flipkart",
        "uber" to "Uber",
        "ola" to "Ola",
        "blinkit" to "Blinkit",
        "zepto" to "Zepto",
        "instamart" to "Instamart",
        "myntra" to "Myntra",
        "bigbasket" to "BigBasket",
        "netflix" to "Netflix",
        "spotify" to "Spotify",
        "starbucks" to "Starbucks",
        "dominos" to "Domino's",
        "mcdonald" to "McDonald's",
        "makemytrip" to "MakeMyTrip"
    )

    // Stopwords that terminate a merchant name extraction
    private val STOPWORDS = setOf(
        "on", "ref", "ref no", "upi", "upi ref", "rrn", "utr", "avl", "bal", "avail",
        "balance", "limit", "a/c", "acct", "card", "dated", "thru", "through", "using"
    )

    // Patterns extracting merchant after "at", "to", "paid to", "info/"
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:paid\\s+to|transfer\\s+to|sent\\s+to|vpa)\\s+([A-Za-z0-9\\s._@&-]+?)(?:\\s+(?:on|ref|upi|avl|bal|avail|dated|\\.)|$)"),
        Pattern.compile("(?i)(?:at|info/)\\s+([A-Za-z0-9\\s._@&-]+?)(?:\\s+(?:on|ref|upi|avl|bal|avail|dated|\\.)|$)"),
        Pattern.compile("(?i)(?:in\\s+favor\\s+of)\\s+([A-Za-z0-9\\s._@&-]+?)(?:\\s+(?:on|ref|upi|avl|bal|avail|dated|\\.)|$)"),
        Pattern.compile("(?i)(?:from|by\\s+transfer\\s+from|received\\s+from)\\s+([A-Za-z0-9\\s._@&-]+?)(?:\\s+(?:on|ref|upi|avl|bal|avail|dated|\\.)|$)")
    )

    fun extractMerchant(body: String): String? {
        // Quick check against known merchants
        val lowerBody = body.lowercase(Locale.US)
        for ((key, normalizedName) in KNOWN_MERCHANTS) {
            if (Pattern.compile("\\b" + Pattern.quote(key) + "\\b").matcher(lowerBody).find()) {
                return normalizedName
            }
        }

        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: continue
                val cleaned = cleanMerchantCandidate(candidate)
                if (isValidMerchant(cleaned)) {
                    return cleaned
                }
            }
        }

        return null
    }

    private fun cleanMerchantCandidate(raw: String): String {
        var token = raw.trim().replace(Regex("[.,/\-]+$"), "").trim()
        // If candidate contains a stopword, truncate before it
        for (stop in STOPWORDS) {
            val idx = token.indexOf(" $stop ", ignoreCase = true)
            if (idx != -1) {
                token = token.substring(0, idx).trim()
            }
        }
        return token.take(40)
    }

    private fun isValidMerchant(candidate: String): Boolean {
        if (candidate.length < 2) return false
        val lower = candidate.lowercase(Locale.US)
        // Reject if candidate is purely a bank keyword, account keyword, or number
        if (lower.matches(Regex("^(bank|a/c|account|card|atm|upi|cash|ref|utr|rrn|inr|rs)$"))) return false
        if (lower.matches(Regex("^[0-9]+$"))) return false
        return true
    }
}
