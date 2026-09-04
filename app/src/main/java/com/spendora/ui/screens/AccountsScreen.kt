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
import com.spendora.data.dao.AccountDao
import com.spendora.data.entity.AccountEntity
import com.spendora.data.model.AccountType
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountDao: AccountDao,
    onNavigateBack: () -> Unit = {}
) {
    var showArchived by remember { mutableStateOf(false) }
    val activeAccounts by accountDao.getAllActive().collectAsState(initial = emptyList())
    val archivedAccounts by accountDao.getAllArchived().collectAsState(initial = emptyList())
    val accountsToShow = if (showArchived) archivedAccounts else activeAccounts

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToArchive by remember { mutableStateOf<AccountEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showArchived) "Archived Accounts" else "Accounts Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(if (showArchived) "View Active" else "View Archived")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showArchived) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        }
    ) { padding ->
        if (accountsToShow.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text(if (showArchived) "No archived accounts" else "No active accounts configured", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to add a bank account, credit card, or wallet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(accountsToShow, key = { it.id }) { acc ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.titleMedium)
                                val masked = if (!acc.maskedNumber.isNullOrEmpty()) "•••• ${acc.maskedNumber}" else "No number"
                                Text(
                                    text = "${acc.institutionName ?: acc.type.name.replace("_", " ")} • $masked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Balance: ₹${String.format(Locale.US, "%,.2f", acc.currentBalance)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (!showArchived) {
                                Row {
                                    IconButton(onClick = { accountToEdit = acc }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { accountToArchive = acc }) {
                                        Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Account Dialog
        if (showAddDialog) {
            AccountFormDialog(
                title = "Add Account",
                initialAccount = null,
                onDismiss = { showAddDialog = false },
                onSave = { name, type, maskedNumber, instName, initialBal ->
                    coroutineScope.launch {
                        accountDao.insert(
                            AccountEntity(
                                name = name,
                                type = type,
                                maskedNumber = maskedNumber,
                                institutionName = instName,
                                initialBalance = initialBal,
                                currentBalance = initialBal,
                                isDefault = false,
                                isArchived = false
                            )
                        )
                        showAddDialog = false
                    }
                }
            )
        }

        // Edit Account Dialog
        if (accountToEdit != null) {
            AccountFormDialog(
                title = "Edit Account",
                initialAccount = accountToEdit,
                onDismiss = { accountToEdit = null },
                onSave = { name, type, maskedNumber, instName, _ ->
                    coroutineScope.launch {
                        val current = accountToEdit!!
                        accountDao.update(
                            current.copy(
                                name = name,
                                type = type,
                                maskedNumber = maskedNumber,
                                institutionName = instName,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        accountToEdit = null
                    }
                }
            )
        }

        // Archive Confirmation Dialog
        if (accountToArchive != null) {
            AlertDialog(
                onDismissRequest = { accountToArchive = null },
                title = { Text("Archive Account?") },
                text = {
                    Text("Archiving \"${accountToArchive!!.name}\" will hide it from new transaction pickers. All historical transactions and spending reports remain 100% intact.")
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            accountDao.archiveAccount(accountToArchive!!.id)
                            accountToArchive = null
                        }
                    }) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToArchive = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun AccountFormDialog(
    title: String,
    initialAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, maskedNumber: String?, instName: String?, initialBal: Double) -> Unit
) {
    var nameText by remember { mutableStateOf(initialAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAccount?.type ?: AccountType.BANK_ACCOUNT) }
    var maskedNumberText by remember { mutableStateOf(initialAccount?.maskedNumber ?: "") }
    var instNameText by remember { mutableStateOf(initialAccount?.institutionName ?: "") }
    var balanceText by remember { mutableStateOf(initialAccount?.initialBalance?.toString() ?: "0.0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Account Name*") }, singleLine = true)
                OutlinedTextField(value = instNameText, onValueChange = { instNameText = it }, label = { Text("Bank / Institution Name") }, singleLine = true)
                OutlinedTextField(value = maskedNumberText, onValueChange = { maskedNumberText = it.take(4) }, label = { Text("Last 4 Digits (e.g. 1234)") }, singleLine = true)
                if (initialAccount == null) {
                    OutlinedTextField(value = balanceText, onValueChange = { balanceText = it }, label = { Text("Initial Balance (₹)") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nameText.isBlank()) {
                    error = "Account name is required"
                    return@Button
                }
                val bal = balanceText.toDoubleOrNull() ?: 0.0
                onSave(
                    nameText.trim(),
                    selectedType,
                    maskedNumberText.trim().ifBlank { null },
                    instNameText.trim().ifBlank { null },
                    bal
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
