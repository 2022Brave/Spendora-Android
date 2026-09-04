package com.spendora.data.sms.parser

import java.util.regex.Pattern

object SmsReferenceExtractor {

    // Matches transaction reference numbers: UPI Ref, UTR, Txn ID, RRN, IMPS Ref
    private val REF_PATTERNS = listOf(
        Pattern.compile("(?i)(?:upi\\s*(?:ref(?:erence)?\\s*(?:no\\.?|id)?)?)[:\\s]+([0-9]{8,14})\\b"),
        Pattern.compile("(?i)(?:utr\\s*(?:no\\.?|id)?|rrn)[:\\s]+([A-Za-z0-9]{8,22})\\b"),
        Pattern.compile("(?i)(?:txn\\s*(?:id|no\\.?)?|ref\\s*(?:no\\.?|id)?)[:\\s]+([A-Za-z0-9]{6,22})\\b"),
        Pattern.compile("(?i)(?:imps\\s*(?:ref(?:erence)?\\s*(?:no\\.?)?)?)[:\\s]+([0-9]{8,14})\\b")
    )

    fun extractReference(body: String): String? {
        for (pattern in REF_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim()
                if (isValidReference(ref)) {
                    return ref
                }
            }
        }
        return null
    }

    private fun isValidReference(ref: String?): Boolean {
        if (ref.isNullOrEmpty()) return false
        // Exclude 6-digit OTP-like numbers if body is ambiguous
        if (ref.length < 6) return false
        // Exclude if purely letters
        if (ref.matches(Regex("^[a-zA-Z]+$"))) return false
        return true
    }
}
