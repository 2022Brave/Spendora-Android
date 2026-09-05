package com.spendora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.TransactionType
import com.spendora.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingReviewScreen(
    repository: TransactionRepository,
    onNavigateBack: () -> Unit = {}
) {
    val pendingItems by repository.getPendingReviewTransactions().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<Long>() }
    var itemToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Review (${pendingItems.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.bulkConfirm(selectedIds.toList())
                                selectedIds.clear()
                            }
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Confirm Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.bulkIgnore(selectedIds.toList())
                                selectedIds.clear()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ignore Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (pendingItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("All caught up!", style = MaterialTheme.typography.titleMedium)
                    Text("No transactions pending review", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(pendingItems, key = { it.id }) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    PendingItemCard(
                        item = item,
                        isSelected = isSelected,
                        onSelectToggle = {
                            if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                        },
                        onConfirm = {
                            coroutineScope.launch {
                                repository.confirmPendingTransaction(item.id)
                                selectedIds.remove(item.id)
                            }
                        },
                        onEdit = { itemToEdit = item },
                        onIgnore = {
                            coroutineScope.launch {
                                repository.ignorePendingTransaction(item.id)
                                selectedIds.remove(item.id)
                            }
                        }
                    )
                }
            }
        }

        // Edit Dialog
        if (itemToEdit != null) {
            EditTransactionDialog(
                transaction = itemToEdit!!,
                onDismiss = { itemToEdit = null },
                onConfirmEdit = { amount, type, merchant ->
                    coroutineScope.launch {
                        repository.editAndConfirmPendingTransaction(
                            transactionId = itemToEdit!!.id,
                            newAmount = amount,
                            newType = type,
                            newMerchant = merchant,
                            newCategoryId = itemToEdit!!.categoryId,
                            newAccountId = itemToEdit!!.accountId
                        )
                        selectedIds.remove(itemToEdit!!.id)
                        itemToEdit = null
                    }
                }
            )
        }
    }
}

@Composable
fun PendingItemCard(
    item: TransactionEntity,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.merchant.ifEmpty { "Unknown Merchant" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${item.maskedAccountIdentifier ?: "Card/Account"} • ${dateFormat.format(Date(item.occurredTimestamp))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", item.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Excerpt & reason
            if (!item.rawSmsExcerpt.isNullOrEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${item.rawSmsExcerpt}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            SuggestionChip(
                onClick = {},
                label = { Text("Proposed: ${item.transactionType.name}") }
            )

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onIgnore, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignore")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConfirm) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onConfirmEdit: (Double, TransactionType, String) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var merchantText by remember { mutableStateOf(transaction.merchant) }
    var selectedType by remember { mutableStateOf(transaction.transactionType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Confirm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Payee") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { selectedType = TransactionType.TRANSFER },
                        label = { Text("Transfer") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: transaction.amount
                onConfirmEdit(amt, selectedType, merchantText.trim())
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
