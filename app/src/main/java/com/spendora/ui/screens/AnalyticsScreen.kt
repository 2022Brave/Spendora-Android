package com.spendora.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendora.data.dao.AccountDao
import com.spendora.data.dao.CategoryDao
import com.spendora.data.engine.FinancialCycleCalculator
import com.spendora.data.model.*
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
import com.spendora.ui.screens.components.CategoryDonutChart
import com.spendora.ui.screens.components.SpendingTrendChart
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    cycleRepository: FinancialCycleRepository,
    transactionRepository: TransactionRepository,
    categoryDao: CategoryDao,
    accountDao: AccountDao
) {
    val zone = remember { ZoneId.systemDefault() }
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.CURRENT_CYCLE) }

    // Custom Date Range state
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

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

    // Calculate previous cycle using Stage 2 FinancialCycleCalculator
    val previousCycle = remember(currentCycle) {
        FinancialCycleCalculator.getPreviousCycle(currentCycle, zone)
    }

    // Determine effective AnalyticsTimeRange
    val timeRange = remember(selectedPeriod, currentCycle, previousCycle, customStartDate, customEndDate) {
        when (selectedPeriod) {
            AnalyticsPeriod.CURRENT_CYCLE -> AnalyticsTimeRange(
                period = AnalyticsPeriod.CURRENT_CYCLE,
                startTimestamp = currentCycle.startTimestamp,
                endTimestamp = currentCycle.endTimestamp,
                displayLabel = currentCycle.displayLabel,
                isMultiCycle = false
            )
            AnalyticsPeriod.PREVIOUS_CYCLE -> AnalyticsTimeRange(
                period = AnalyticsPeriod.PREVIOUS_CYCLE,
                startTimestamp = previousCycle.startTimestamp,
                endTimestamp = previousCycle.endTimestamp,
                displayLabel = previousCycle.displayLabel,
                isMultiCycle = false
            )
            AnalyticsPeriod.LAST_3_CYCLES -> {
                val cycleMinus1 = FinancialCycleCalculator.getPreviousCycle(currentCycle, zone)
                val cycleMinus2 = FinancialCycleCalculator.getPreviousCycle(cycleMinus1, zone)
                AnalyticsTimeRange(
                    period = AnalyticsPeriod.LAST_3_CYCLES,
                    startTimestamp = cycleMinus2.startTimestamp,
                    endTimestamp = currentCycle.endTimestamp,
                    displayLabel = "Last 3 Cycles (${cycleMinus2.startDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))} – ${currentCycle.endDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))})",
                    isMultiCycle = true
                )
            }
            AnalyticsPeriod.LAST_6_CYCLES -> {
                var c = currentCycle
                repeat(5) { c = FinancialCycleCalculator.getPreviousCycle(c, zone) }
                AnalyticsTimeRange(
                    period = AnalyticsPeriod.LAST_6_CYCLES,
                    startTimestamp = c.startTimestamp,
                    endTimestamp = currentCycle.endTimestamp,
                    displayLabel = "Last 6 Cycles (${c.startDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))} – ${currentCycle.endDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))})",
                    isMultiCycle = true
                )
            }
            AnalyticsPeriod.LAST_12_CYCLES -> {
                var c = currentCycle
                repeat(11) { c = FinancialCycleCalculator.getPreviousCycle(c, zone) }
                AnalyticsTimeRange(
                    period = AnalyticsPeriod.LAST_12_CYCLES,
                    startTimestamp = c.startTimestamp,
                    endTimestamp = currentCycle.endTimestamp,
                    displayLabel = "Last 12 Cycles (${c.startDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))} – ${currentCycle.endDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))})",
                    isMultiCycle = true
                )
            }
            AnalyticsPeriod.CUSTOM_RANGE -> {
                val startTs = customStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val endTs = customEndDate.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                val days = java.time.temporal.ChronoUnit.DAYS.between(customStartDate, customEndDate)
                AnalyticsTimeRange(
                    period = AnalyticsPeriod.CUSTOM_RANGE,
                    startTimestamp = startTs,
                    endTimestamp = endTs,
                    displayLabel = "Custom (${customStartDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))} – ${customEndDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))})",
                    isMultiCycle = days > 45
                )
            }
        }
    }

    // Build temporary synthetic FinancialCycle representation for repository calls
    val reportingCycle = remember(timeRange) {
        FinancialCycle(
            cycleId = "REPORTING_${timeRange.period.name}",
            cycleYearMonth = currentCycle.cycleYearMonth,
            startDate = currentCycle.startDate,
            endDate = currentCycle.endDate,
            startTimestamp = timeRange.startTimestamp,
            endTimestamp = timeRange.endTimestamp,
            displayLabel = timeRange.displayLabel,
            cycleStartDay = currentCycle.cycleStartDay
        )
    }

    val dashboardSummary by transactionRepository.getDashboardSummary(reportingCycle).collectAsState(
        initial = DashboardSummary(
            cycle = reportingCycle,
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
            daysRemaining = 0
        )
    )

    val cycleComparison by transactionRepository.getCycleComparison(currentCycle, previousCycle).collectAsState(
        initial = CycleComparison(
            currentSpending = 0.0,
            previousSpending = 0.0,
            absoluteDifference = 0.0,
            percentageChange = null,
            hasPreviousData = false
        )
    )

    val spendingTrend by transactionRepository.getSpendingTrendForTimeRange(timeRange).collectAsState(initial = emptyList())
    val categorySpending by transactionRepository.getCategorySpendingItems(reportingCycle, categoryDao).collectAsState(initial = emptyList())
    val accountSpending by transactionRepository.getAccountSpendingItems(reportingCycle, accountDao).collectAsState(initial = emptyList())
    val txnTypeBreakdown by transactionRepository.getTransactionTypeBreakdown(reportingCycle).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Financial Analytics") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Horizontal Scrollable Period Selector Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsPeriod.values().forEach { p ->
                        FilterChip(
                            selected = selectedPeriod == p,
                            onClick = {
                                if (p == AnalyticsPeriod.CUSTOM_RANGE) {
                                    showCustomDateDialog = true
                                }
                                selectedPeriod = p
                            },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            // Period Overview Card
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(timeRange.displayLabel, style = MaterialTheme.typography.titleMedium)
                            if (selectedPeriod == AnalyticsPeriod.CUSTOM_RANGE) {
                                IconButton(onClick = { showCustomDateDialog = true }) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = "Edit Custom Range", modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

            // GAP A: User-Facing Spending Trend Chart
            item {
                SpendingTrendChart(trendPoints = spendingTrend)
            }

            // Current vs Previous Cycle Comparison (When viewing Current Cycle)
            if (selectedPeriod == AnalyticsPeriod.CURRENT_CYCLE) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Cycle-over-Cycle Comparison", style = MaterialTheme.typography.titleMedium)
                            }

                            if (!cycleComparison.hasPreviousData) {
                                Text("No previous cycle spending recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                val diff = cycleComparison.absoluteDifference
                                val pct = cycleComparison.percentageChange
                                Text(
                                    text = "Difference: ${if (diff >= 0) "+" else ""}₹${String.format(Locale.US, "%,.2f", diff)} (${if (pct != null && pct >= 0) "+" else ""}${pct?.let { String.format(Locale.US, "%.1f", it) } ?: "0"}% vs last cycle)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Income vs Expense Visual Bars
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Income vs Spending Ratio", style = MaterialTheme.typography.titleMedium)

                        val maxVal = maxOf(dashboardSummary.totalIncome, dashboardSummary.totalSpending, 1.0)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Income", style = MaterialTheme.typography.bodySmall)
                                Text("₹${String.format(Locale.US, "%,.0f", dashboardSummary.totalIncome)}", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = (dashboardSummary.totalIncome / maxVal).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spending (Net)", style = MaterialTheme.typography.bodySmall)
                                Text("₹${String.format(Locale.US, "%,.0f", dashboardSummary.totalSpending)}", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = (dashboardSummary.totalSpending / maxVal).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Category Spending Donut Chart
            item {
                CategoryDonutChart(
                    items = categorySpending,
                    totalSpending = dashboardSummary.totalSpending
                )
            }

            // Category Breakdown Table
            if (categorySpending.isNotEmpty()) {
                item {
                    Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                }

                items(categorySpending) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.categoryName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${item.transactionCount} transaction${if (item.transactionCount > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", item.netAmount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", item.percentageOfTotal)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Account-level Breakdown
            if (accountSpending.isNotEmpty()) {
                item {
                    Text("Account Breakdown", style = MaterialTheme.typography.titleMedium)
                }

                items(accountSpending) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.accountName, style = MaterialTheme.typography.bodyMedium)
                                if (item.maskedNumber != null) {
                                    Text(item.maskedNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Spent: ₹${String.format(Locale.US, "%,.2f", item.totalSpent)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (item.totalIncome > 0) {
                                    Text(
                                        text = "Income: ₹${String.format(Locale.US, "%,.2f", item.totalIncome)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transaction-Type Breakdown
            if (txnTypeBreakdown.isNotEmpty()) {
                item {
                    Text("Transaction-Type Breakdown", style = MaterialTheme.typography.titleMedium)
                }

                items(txnTypeBreakdown) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.type.name.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", item.totalAmount)} (${item.count})",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        // Custom Date Range Dialog
        if (showCustomDateDialog) {
            CustomDateRangeDialog(
                currentStart = customStartDate,
                currentEnd = customEndDate,
                onDismiss = { showCustomDateDialog = false },
                onConfirm = { start, end ->
                    customStartDate = start
                    customEndDate = end
                    showCustomDateDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomDateRangeDialog(
    currentStart: LocalDate,
    currentEnd: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var startText by remember { mutableStateOf(currentStart.toString()) }
    var endText by remember { mutableStateOf(currentEnd.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Custom Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text("Start Date (YYYY-MM-DD):", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = startText, onValueChange = { startText = it; error = null }, singleLine = true)

                Text("End Date (YYYY-MM-DD):", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = endText, onValueChange = { endText = it; error = null }, singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            startText = LocalDate.now().minusDays(30).toString()
                            endText = LocalDate.now().toString()
                        },
                        label = { Text("Last 30d") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            startText = LocalDate.now().minusMonths(3).toString()
                            endText = LocalDate.now().toString()
                        },
                        label = { Text("Last 90d") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            startText = LocalDate.now().minusYears(1).toString()
                            endText = LocalDate.now().toString()
                        },
                        label = { Text("Last 1y") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = runCatching { LocalDate.parse(startText) }.getOrNull()
                val e = runCatching { LocalDate.parse(endText) }.getOrNull()
                if (s == null || e == null) {
                    error = "Please enter valid dates formatted as YYYY-MM-DD"
                    return@Button
                }
                if (s.isAfter(e)) {
                    error = "Start date cannot be after end date"
                    return@Button
                }
                onConfirm(s, e)
            }) {
                Text("Apply Filter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
