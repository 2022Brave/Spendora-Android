package com.spendora.data.sms.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.regex.Pattern

object SmsDateExtractor {

    // Common Indian bank date patterns:
    // e.g. "on 04-SEP-26", "on 04/09/2026", "on 04-09-2026 14:30:00"
    private val DATE_REGEX = Pattern.compile(
        "(?i)\\b(?:on\\s+)?([0-9]{1,2}[-/.][A-Za-z]{3}[-/.]20?[0-9]{2}|[0-9]{1,2}[-/.][0-9]{1,2}[-/.]20?[0-9]{2})(?:\\s+at\\s+([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?))?\\b"
    )

    fun determineTimestamp(
        body: String,
        suppliedSmsTimestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val matcher = DATE_REGEX.matcher(body)
        if (matcher.find()) {
            val dateStr = matcher.group(1)
            val timeStr = matcher.group(2)
            val parsedMillis = tryParseDate(dateStr, timeStr, zoneId)
            if (parsedMillis != null) {
                return parsedMillis
            }
        }
        // Fallback: strictly preserve the carrier SMS timestamp (never System.currentTimeMillis())
        return suppliedSmsTimestamp
    }

    private fun tryParseDate(dateStr: String?, timeStr: String?, zoneId: ZoneId): Long? {
        if (dateStr == null) return null
        return try {
            val normalizedDate = dateStr.replace("/", "-").replace(".", "-")
            val parts = normalizedDate.split("-")
            if (parts.size != 3) return null

            val day = parts[0].toIntOrNull() ?: return null
            val year = when (parts[2].length) {
                2 -> 2000 + (parts[2].toIntOrNull() ?: return null)
                4 -> parts[2].toIntOrNull() ?: return null
                else -> return null
            }

            val month = if (parts[1].matches(Regex("^[0-9]+$"))) {
                parts[1].toIntOrNull() ?: return null
            } else {
                parseMonthName(parts[1]) ?: return null
            }

            val localDate = LocalDate.of(year, month, day)
            val localTime = if (timeStr != null) {
                val tParts = timeStr.split(":")
                val h = tParts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = tParts.getOrNull(1)?.toIntOrNull() ?: 0
                val s = tParts.getOrNull(2)?.toIntOrNull() ?: 0
                java.time.LocalTime.of(h, m, s)
            } else {
                java.time.LocalTime.of(12, 0, 0)
            }

            LocalDateTime.of(localDate, localTime).atZone(zoneId).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMonthName(name: String): Int? {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.startsWith("jan") -> 1
            lower.startsWith("feb") -> 2
            lower.startsWith("mar") -> 3
            lower.startsWith("apr") -> 4
            lower.startsWith("may") -> 5
            lower.startsWith("jun") -> 6
            lower.startsWith("jul") -> 7
            lower.startsWith("aug") -> 8
            lower.startsWith("sep") -> 9
            lower.startsWith("oct") -> 10
            lower.startsWith("nov") -> 11
            lower.startsWith("dec") -> 12
            else -> null
        }
    }
}
