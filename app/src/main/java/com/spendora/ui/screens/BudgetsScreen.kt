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
import com.spendora.data.dao.CategoryDao
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.model.BudgetItemWithProgress
import com.spendora.data.model.BudgetStatus
import com.spendora.data.model.FinancialCycle
import com.spendora.data.repository.BudgetRepository
import com.spendora.data.repository.FinancialCycleRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    budgetRepository: BudgetRepository,
    cycleRepository: FinancialCycleRepository,
    categoryDao: CategoryDao
) {
    val currentCycle by cycleRepository.getCurrentCycle().collectAsState(
        initial = FinancialCycle(
            cycleId = "CURRENT",
            cycleYearMonth = "2026-09",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(1),
            startTimestamp = 0L,
            endTimestamp = Long.MAX_VALUE,
            displayLabel = "Current Cycle",
            cycleStartDay = 1
        )
    )

    val budgetsWithProgress by budgetRepository.getBudgetsWithProgressForCycle(currentCycle).collectAsState(initial = emptyList())
    val activeCategories by categoryDao.getAllActive().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetItemWithProgress?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetItemWithProgress?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Budgets") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        if (budgetsWithProgress.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No budgets configured", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Set category spending limits for ${currentCycle.displayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create First Budget")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        text = "Budgets for ${currentCycle.displayLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(budgetsWithProgress, key = { it.budgetId }) { item ->
                    BudgetCard(
                        item = item,
                        onEdit = { budgetToEdit = item },
                        onDelete = { budgetToDelete = item }
                    )
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            BudgetFormDialog(
                title = "Create Budget",
                categories = activeCategories,
                initialCategory = null,
                initialAmount = 0.0,
                onDismiss = { showAddDialog = false },
                onSave = { catId, amt ->
                    coroutineScope.launch {
                        budgetRepository.createOrUpdateBudget(
                            categoryId = catId,
                            amount = amt,
                            cycleYearMonth = currentCycle.cycleYearMonth
                        )
                        showAddDialog = false
                    }
                }
            )
        }

        // Edit Dialog
        if (budgetToEdit != null) {
            BudgetFormDialog(
                title = "Edit Budget",
                categories = activeCategories,
                initialCategory = budgetToEdit!!.categoryId,
                initialAmount = budgetToEdit!!.budgetAmount,
                onDismiss = { budgetToEdit = null },
                onSave = { catId, amt ->
                    coroutineScope.launch {
                        budgetRepository.createOrUpdateBudget(
                            categoryId = catId,
                            amount = amt,
                            cycleYearMonth = currentCycle.cycleYearMonth
                        )
                        budgetToEdit = null
                    }
                }
            )
        }

        // Delete Dialog
        if (budgetToDelete != null) {
            AlertDialog(
                onDismissRequest = { budgetToDelete = null },
                title = { Text("Delete Budget?") },
                text = { Text("Are you sure you want to remove the budget for ${budgetToDelete!!.categoryName}? Historical transactions will not be affected.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                budgetRepository.deleteBudget(budgetToDelete!!.budgetId)
                                budgetToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { budgetToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun BudgetCard(
    item: BudgetItemWithProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Budget: ₹${String.format(Locale.US, "%,.2f", item.budgetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (item.status) {
                        BudgetStatus.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                        BudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = item.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (item.status) {
                            BudgetStatus.HEALTHY -> MaterialTheme.colorScheme.onPrimaryContainer
                            BudgetStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            LinearProgressIndicator(
                progress = (item.percentageConsumed / 100.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (item.status) {
                    BudgetStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                    BudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                    BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ₹${String.format(Locale.US, "%,.0f", item.spentAmount)} (${String.format(Locale.US, "%.1f", item.percentageConsumed)}%)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (item.status == BudgetStatus.EXCEEDED)
                        "Over by ₹${String.format(Locale.US, "%,.0f", item.spentAmount - item.budgetAmount)}"
                    else "Remaining: ₹${String.format(Locale.US, "%,.0f", item.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == BudgetStatus.EXCEEDED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun BudgetFormDialog(
    title: String,
    categories: List<CategoryEntity>,
    initialCategory: Long?,
    initialAmount: Double,
    onDismiss: () -> Unit,
    onSave: (Long?, Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(initialCategory) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Budget Limit (₹)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Category:", style = MaterialTheme.typography.labelMedium)
                // Filter chips for category selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("Overall Budget") }
                    )
                }
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(categories.take(15)) { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    error = "Enter a valid positive budget amount"
                    return@Button
                }
                onSave(selectedCategoryId, amt)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
