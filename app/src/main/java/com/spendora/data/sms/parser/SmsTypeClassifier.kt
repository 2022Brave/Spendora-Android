package com.spendora.data.sms.parser

import com.spendora.data.model.TransactionType
import java.util.regex.Pattern

object SmsTypeClassifier {

    private val CASH_WITHDRAWAL_PATTERN = Pattern.compile(
        "(?i)\\b(atm\\s*withdrawal|cash\\s*withdrawal|withdrawn\\s*at\\s*atm|withdrawn\\s*from\\s*atm|cash\\s*withdrawn)\\b"
    )

    private val REFUND_PATTERN = Pattern.compile(
        "(?i)\\b(refund\\s*(?:of)?|refunded|reversal\\s*(?:of)?|reversed|amount\\s*reversed|transaction\\s*reversed)\\b"
    )

    private val TRANSFER_PATTERN = Pattern.compile(
        "(?i)\\b(transferred\\s*(?:to|from)|transfer\\s*of|self\\s*transfer|own\\s*account|inter-?bank\\s*transfer|imps\\s*transfer|neft\\s*transfer|rtgs\\s*transfer)\\b"
    )

    private val SALARY_OR_INCOME_PATTERN = Pattern.compile(
        "(?i)\\b(salary\\s*(?:of)?|credited\\s*with|credited\\s*to|deposited|received\\s*(?:from)?)\\b"
    )

    private val EXPENSE_PATTERN = Pattern.compile(
        "(?i)\\b(debited|spent|paid|withdrawn|charged|sent\\s*to|purchase\\s*of)\\b"
    )

    fun classifyType(body: String): TransactionType? {
        // 1. Check Cash Withdrawal first
        if (CASH_WITHDRAWAL_PATTERN.matcher(body).find()) {
            return TransactionType.CASH_WITHDRAWAL
        }

        // 2. Check Refund / Reversal
        if (REFUND_PATTERN.matcher(body).find()) {
            return TransactionType.REFUND
        }

        // 3. Check Transfer (between accounts or designated transfer channels)
        if (TRANSFER_PATTERN.matcher(body).find()) {
            return TransactionType.TRANSFER
        }

        // 4. Check Credit / Salary / Income
        val isCredit = SALARY_OR_INCOME_PATTERN.matcher(body).find()
        val isDebit = EXPENSE_PATTERN.matcher(body).find()

        return when {
            isDebit && !isCredit -> TransactionType.EXPENSE
            isCredit && !isDebit -> TransactionType.INCOME
            isCredit && isDebit -> {
                // If both are present, determine based on dominant placement or refund context
                if (body.contains("debited", ignoreCase = true)) TransactionType.EXPENSE else TransactionType.INCOME
            }
            else -> null
        }
    }
}
