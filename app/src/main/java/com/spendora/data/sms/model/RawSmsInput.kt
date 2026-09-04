package com.spendora.data.sms.model

/**
 * Raw input representation of an SMS message.
 * Used uniformly across live broadcast reception and historical provider scans.
 */
data class RawSmsInput(
    val sender: String,
    val body: String,
    val smsTimestamp: Long, // Epoch millis from telecom carrier
    val androidSmsRowId: Long? = null // Non-null when sourced from content://sms
)
