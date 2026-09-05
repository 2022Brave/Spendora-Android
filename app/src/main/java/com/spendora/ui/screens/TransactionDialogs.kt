package com.spendora.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spendora.data.entity.AccountEntity
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.location.TransactionLocation
import com.spendora.data.location.TransactionLocationManager
import com.spendora.data.model.TransactionType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: TransactionType, merchant: String, categoryId: Long?, accountId: Long?, notes: String?, latitude: Double?, longitude: Double?, locationLabel: String?) -> Unit
) {
    val context = LocalContext.current
    val locationManager = remember { TransactionLocationManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var attachedLocation by remember { mutableStateOf<TransactionLocation?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            coroutineScope.launch {
                val fix = locationManager.getOneShotLocation()
                isLocating = false
                if (fix != null) {
                    attachedLocation = fix
                } else {
                    errorMessage = "Could not acquire location fix. Continuing without location."
                }
            }
        } else {
            errorMessage = "Location permission denied. Transaction can still be created without location."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMessage = null },
                    label = { Text("Amount (₹)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it; errorMessage = null },
                    label = { Text("Merchant / Payee*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Transaction-Level Location Tagging
                if (attachedLocation != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "📍 ${attachedLocation!!.latitude}, ${attachedLocation!!.longitude}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { attachedLocation = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Location", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (locationManager.hasLocationPermission()) {
                                isLocating = true
                                coroutineScope.launch {
                                    val fix = locationManager.getOneShotLocation()
                                    isLocating = false
                                    if (fix != null) {
                                        attachedLocation = fix
                                    } else {
                                        errorMessage = "Could not acquire location fix. Continuing without location."
                                    }
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Getting one-shot location...")
                        } else {
                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Location (Optional)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    errorMessage = "Enter a valid amount greater than 0"
                    return@Button
                }
                if (merchantText.isBlank()) {
                    errorMessage = "Enter a merchant or payee"
                    return@Button
                }
                onSave(
                    amt,
                    selectedType,
                    merchantText.trim(),
                    selectedCategoryId,
                    selectedAccountId,
                    notesText.ifBlank { null },
                    attachedLocation?.latitude,
                    attachedLocation?.longitude,
                    attachedLocation?.label
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    categoryName: String = "Uncategorized",
    accountName: String? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddLocation: () -> Unit = {},
    onRemoveLocation: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.US) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.merchant.ifEmpty { "Transaction Details" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount: ₹${String.format(Locale.US, "%,.2f", transaction.amount)}", style = MaterialTheme.typography.titleMedium)
                Text("Type: ${transaction.transactionType.name.replace("_", " ")}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: $categoryName", style = MaterialTheme.typography.bodyMedium)
                Text("Account: ${accountName ?: transaction.maskedAccountIdentifier ?: "Not linked"}", style = MaterialTheme.typography.bodyMedium)
                Text("Date: ${dateFormat.format(Date(transaction.occurredTimestamp))}", style = MaterialTheme.typography.bodyMedium)
                Text("Source: ${transaction.source.name.replace("_", " ")}", style = MaterialTheme.typography.bodyMedium)
                if (!transaction.transactionReferenceNumber.isNullOrEmpty()) {
                    Text("Ref: ${transaction.transactionReferenceNumber}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!transaction.notes.isNullOrEmpty()) {
                    Text("Notes: ${transaction.notes}", style = MaterialTheme.typography.bodyMedium)
                }

                // Location details
                if (transaction.latitude != null && transaction.longitude != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "📍 ${transaction.latitude}, ${transaction.longitude}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = onRemoveLocation) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) { Text("Edit") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )

    // Destructive Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction for ₹${String.format(Locale.US, "%,.2f", transaction.amount)}? This action is safe and non-destructive to underlying SMS data.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
