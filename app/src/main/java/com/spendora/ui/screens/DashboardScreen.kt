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
import com.spendora.data.dao.BudgetDao
import com.spendora.data.dao.CategoryDao
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.DashboardSummary
import com.spendora.data.model.FinancialCycle
import com.spendora.data.repository.BudgetRepository
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
import com.spendora.ui.screens.components.CategoryDonutChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionRepository: TransactionRepository,
    cycleRepository: FinancialCycleRepository,
    categoryDao: CategoryDao,
    budgetRepository: BudgetRepository,
    onNavigateToPendingReview: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {}
) {
    val currentCycle by cycleRepository.getCurrentCycle().collectAsState(
        initial = FinancialCycle(
            cycleId = "CYCLE_CURRENT",
            cycleYearMonth = "2026-09",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(1).minusDays(1),
            startTimestamp = 0L,
            endTimestamp = Long.MAX_VALUE,
            displayLabel = "Current Financial Cycle",
            cycleStartDay = 1
        )
    )

    val dashboardSummary by transactionRepository.getDashboardSummary(currentCycle).collectAsState(
        initial = DashboardSummary(
            cycle = currentCycle,
            totalSpending = 0.0,
            totalIncome = 0.0,
            totalRefunds = 0.0,
            totalTransfers = 0.0,
            totalCashWithdrawals = 0.0,
            netCashFlow = 0.0,
            confirmedTransactionCount = 0,
            pendingReviewCount = 0,
            activeBudgetCount = 0,
            totalBudgetAmount = 0.0,
            totalBudgetSpent = 0.0,
            cycleProgressFraction = 0f,
            daysElapsed = 0,
            daysRemaining = 30
        )
    )

    val categorySpending by transactionRepository.getCategorySpendingItems(currentCycle, categoryDao).collectAsState(initial = emptyList())
    val recentTransactions by transactionRepository.getRecentTransactions(5).collectAsState(initial = emptyList())
    val budgetsWithProgress by budgetRepository.getBudgetsWithProgressForCycle(currentCycle).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPENDORA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Personal Financial Command Center",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("100% Offline", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Financial Cycle Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Reporting Cycle",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${dashboardSummary.daysRemaining} days left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = currentCycle.displayLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    LinearProgressIndicator(
                        progress = dashboardSummary.cycleProgressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "From ${currentCycle.startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))} to ${currentCycle.endDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Cycle Summary Metrics Card
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cycle Financial Overview", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onNavigateToAnalytics) {
                            Text("Analytics")
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", dashboardSummary.totalSpending)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (dashboardSummary.totalRefunds > 0) {
                                Text(
                                    text = "incl. -₹${String.format(Locale.US, "%,.0f", dashboardSummary.totalRefunds)} refunds",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", dashboardSummary.totalIncome)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cash Withdrawals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", dashboardSummary.totalCashWithdrawals)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Cash Flow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "${if (dashboardSummary.netCashFlow >= 0) "+" else ""}₹${String.format(Locale.US, "%,.2f", dashboardSummary.netCashFlow)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (dashboardSummary.netCashFlow >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Budget Summary Card
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Active Budgets (${budgetsWithProgress.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onNavigateToBudgets) {
                            Text("View All")
                        }
                    }

                    if (budgetsWithProgress.isEmpty()) {
                        Text(
                            text = "No category budgets set for this cycle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        budgetsWithProgress.take(3).forEach { b ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(b.categoryName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.0f", b.spentAmount)} / ₹${String.format(Locale.US, "%,.0f", b.budgetAmount)} (${String.format(Locale.US, "%.0f", b.percentageConsumed)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (b.percentageConsumed >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = (b.percentageConsumed / 100f).toFloat().coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = when {
                                        b.percentageConsumed >= 100 -> MaterialTheme.colorScheme.error
                                        b.percentageConsumed >= 80 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Spending by Category (Donut Chart)
        item {
            CategoryDonutChart(
                items = categorySpending,
                totalSpending = dashboardSummary.totalSpending
            )
        }

        // Pending Review Banner
        if (dashboardSummary.pendingReviewCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPendingReview() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${dashboardSummary.pendingReviewCount} Transaction${if (dashboardSummary.pendingReviewCount > 1) "s" else ""} Pending Review",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Tap to review ambiguous SMS alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Confirmed Activity", style = MaterialTheme.typography.titleMedium)
                if (recentTransactions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("View all")
                    }
                }
            }
        }

        // Recent Transactions List or Clean Empty State
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("No transactions yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Add manual expenses or import SMS history in Settings to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { txn ->
                TransactionRowItem(
                    transaction = txn,
                    onClick = { onTransactionClick(txn) }
                )
            }
        }
    }
}
