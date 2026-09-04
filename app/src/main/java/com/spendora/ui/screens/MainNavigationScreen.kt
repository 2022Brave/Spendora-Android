package com.spendora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spendora.data.backup.DataBackupManager
import com.spendora.data.dao.AccountDao
import com.spendora.data.dao.CategoryDao
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.repository.BudgetRepository
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.security.BiometricAuthManager
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    TRANSACTIONS,
    BUDGETS,
    ANALYTICS,
    MORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    transactionRepository: TransactionRepository,
    cycleRepository: FinancialCycleRepository,
    budgetRepository: BudgetRepository,
    categoryDao: CategoryDao,
    accountDao: AccountDao,
    biometricManager: BiometricAuthManager,
    backupManager: DataBackupManager,
    onNavigateToPendingReview: () -> Unit = {},
    onNavigateToHistoricalImport: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToCycleSettings: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onToggleTheme: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.DASHBOARD,
                    onClick = { selectedTab = AppTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TRANSACTIONS,
                    onClick = { selectedTab = AppTab.TRANSACTIONS },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Transactions") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.BUDGETS,
                    onClick = { selectedTab = AppTab.BUDGETS },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                    label = { Text("Budgets") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.ANALYTICS,
                    onClick = { selectedTab = AppTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.MORE,
                    onClick = { selectedTab = AppTab.MORE },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == AppTab.DASHBOARD || selectedTab == AppTab.TRANSACTIONS) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    transactionRepository = transactionRepository,
                    cycleRepository = cycleRepository,
                    categoryDao = categoryDao,
                    budgetRepository = budgetRepository,
                    onNavigateToPendingReview = onNavigateToPendingReview,
                    onNavigateToTransactions = { selectedTab = AppTab.TRANSACTIONS },
                    onNavigateToBudgets = { selectedTab = AppTab.BUDGETS },
                    onNavigateToAnalytics = { selectedTab = AppTab.ANALYTICS },
                    onTransactionClick = { selectedTransactionForDetail = it }
                )
                AppTab.TRANSACTIONS -> TransactionsScreen(
                    transactionRepository = transactionRepository,
                    onTransactionClick = { selectedTransactionForDetail = it }
                )
                AppTab.BUDGETS -> BudgetsScreen(
                    budgetRepository = budgetRepository,
                    cycleRepository = cycleRepository,
                    categoryDao = categoryDao
                )
                AppTab.ANALYTICS -> AnalyticsScreen(
                    cycleRepository = cycleRepository,
                    transactionRepository = transactionRepository,
                    categoryDao = categoryDao,
                    accountDao = accountDao
                )
                AppTab.MORE -> MoreSettingsScreen(
                    cycleRepository = cycleRepository,
                    transactionRepository = transactionRepository,
                    biometricManager = biometricManager,
                    backupManager = backupManager,
                    onNavigateToPendingReview = onNavigateToPendingReview,
                    onNavigateToHistoricalImport = onNavigateToHistoricalImport,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToCycleSettings = onNavigateToCycleSettings,
                    onNavigateToPrivacy = onNavigateToPrivacy,
                    onToggleTheme = onToggleTheme
                )
            }
        }

        // Add Transaction Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { amount, type, merchant, catId, accId, notes, lat, lon, locLabel ->
                    coroutineScope.launch {
                        transactionRepository.createManualTransaction(
                            amount = amount,
                            type = type,
                            merchant = merchant,
                            categoryId = catId,
                            accountId = accId,
                            occurredTimestamp = System.currentTimeMillis(),
                            notes = notes,
                            latitude = lat,
                            longitude = lon,
                            locationLabel = locLabel
                        )
                        showAddDialog = false
                    }
                }
            )
        }

        // Detail Dialog
        if (selectedTransactionForDetail != null) {
            TransactionDetailDialog(
                transaction = selectedTransactionForDetail!!,
                onDismiss = { selectedTransactionForDetail = null },
                onEdit = {
                    selectedTransactionForDetail = null
                },
                onDelete = {
                    coroutineScope.launch {
                        transactionRepository.deleteTransaction(selectedTransactionForDetail!!.id)
                        selectedTransactionForDetail = null
                    }
                },
                onRemoveLocation = {
                    coroutineScope.launch {
                        transactionRepository.removeTransactionLocation(selectedTransactionForDetail!!.id)
                        selectedTransactionForDetail = null
                    }
                }
            )
        }
    }
}
