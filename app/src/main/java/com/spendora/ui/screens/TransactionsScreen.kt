package com.spendora.ui.screens

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
import com.spendora.data.entity.AccountEntity
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
import com.spendora.data.repository.TransactionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactionRepository: TransactionRepository,
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    var selectedSource by remember { mutableStateOf<TransactionSource?>(null) }

    // Room-backed filtered transactions Flow
    val transactions by transactionRepository.searchAndFilterTransactions(
        searchQuery = searchQuery,
        type = selectedType,
        source = selectedSource,
        limit = 100
    ).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            placeholder = { Text("Search by merchant, ref, or note...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("All Types") }
            )
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = { selectedType = if (selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE },
                label = { Text("Expenses") }
            )
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = { selectedType = if (selectedType == TransactionType.INCOME) null else TransactionType.INCOME },
                label = { Text("Income") }
            )
            FilterChip(
                selected = selectedType == TransactionType.TRANSFER,
                onClick = { selectedType = if (selectedType == TransactionType.TRANSFER) null else TransactionType.TRANSFER },
                label = { Text("Transfers") }
            )
        }

        // Transaction List
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (searchQuery.isNotEmpty() || selectedType != null) "No transactions match your filters"
                        else "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    TransactionRowItem(
                        transaction = txn,
                        onClick = { onTransactionClick(txn) }
                    )
                }
            }
        }
    }
}
