package com.spendora.data.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.sms.model.RawSmsInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SmsBroadcastReceiver
 *
 * Dedicated native Android BroadcastReceiver listening for incoming SMS.
 * Operates completely offline without requiring any UI, Activity, WebView, or JavaScript runtime.
 *
 * Lifecycle:
 * - Triggered by android.provider.Telephony.SMS_RECEIVED
 * - Uses goAsync() to execute parsing and Room database persistence asynchronously on Dispatchers.IO
 * - Combines multipart SMS messages belonging to the same sender and timestamp
 * - Hands raw segments directly to SmsEngine through TransactionRepository
 * - Guarantees pendingResult.finish() in finally block, ensuring zero receiver leaks or ANRs
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SpendoraDatabase.getInstance(appContext)
                val repository = TransactionRepository(
                    transactionDao = db.transactionDao(),
                    smsAuditDao = db.smsAuditDao(),
                    accountDao = db.accountDao()
                )

                // Group by originating address and timestamp to reconstruct multipart messages
                val groupedMessages = messages.groupBy { sms ->
                    val sender = sms.displayOriginatingAddress 
                        ?: sms.originatingAddress 
                        ?: "UNKNOWN"
                    sender to sms.timestampMillis
                }

                for (((sender, timestamp), parts) in groupedMessages) {
                    val fullBody = parts.joinToString(separator = "") { part ->
                        part.displayMessageBody ?: part.messageBody ?: ""
                    }

                    if (fullBody.isBlank()) continue

                    val input = RawSmsInput(
                        sender = sender,
                        body = fullBody,
                        smsTimestamp = timestamp,
                        androidSmsRowId = null // Row ID not available in live broadcast intent
                    )

                    // Execute atomic parsing, deduplication, and Room persistence
                    val result = repository.processLiveSms(input)
                    Log.d(TAG, "Processed live SMS from sender prefix: ${sender.take(6)}... Result: ${result.javaClass.simpleName}")
                }
            } catch (e: Exception) {
                // Fail-safe: log only the exception type, never log raw SMS body, card numbers, or OTPs
                Log.e(TAG, "Safe failure during live SMS processing: ${e.javaClass.simpleName}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
