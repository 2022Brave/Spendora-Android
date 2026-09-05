package com.spendora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    categoryName: String = "Uncategorized",
    accountName: String? = null,
    onClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    val isExpense = transaction.transactionType == TransactionType.EXPENSE || transaction.transactionType == TransactionType.CASH_WITHDRAWAL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$categoryName • ${accountName ?: transaction.maskedAccountIdentifier ?: "Account"} • ${dateFormat.format(Date(transaction.occurredTimestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = transaction.transactionType.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
