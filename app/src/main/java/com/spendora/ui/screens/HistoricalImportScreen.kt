package com.spendora.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.spendora.data.sms.import.HistoricalSmsImportWorker
import com.spendora.data.sms.import.ImportState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalImportScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val workInfos by workManager.getWorkInfosForUniqueWorkLiveData(
        HistoricalSmsImportWorker.UNIQUE_WORK_NAME
    ).observeAsState()

    val currentWorkInfo = workInfos?.firstOrNull()

    // Determine current progress state from WorkManager
    val isWorkRunning = currentWorkInfo?.state == WorkInfo.State.RUNNING
    val isWorkFinished = currentWorkInfo?.state == WorkInfo.State.SUCCEEDED || currentWorkInfo?.state == WorkInfo.State.FAILED

    val progressData = if (isWorkRunning) currentWorkInfo?.progress else currentWorkInfo?.outputData
    val examinedCount = progressData?.getInt(HistoricalSmsImportWorker.KEY_TOTAL_EXAMINED, 0) ?: 0
    val importedCount = progressData?.getInt(HistoricalSmsImportWorker.KEY_IMPORTED_COUNT, 0) ?: 0
    val duplicateCount = progressData?.getInt(HistoricalSmsImportWorker.KEY_DUPLICATE_COUNT, 0) ?: 0
    val ignoredCount = progressData?.getInt(HistoricalSmsImportWorker.KEY_IGNORED_COUNT, 0) ?: 0
    val pendingReviewCount = progressData?.getInt(HistoricalSmsImportWorker.KEY_PENDING_REVIEW_COUNT, 0) ?: 0

    // Selected start date (default 30 days ago)
    var selectedDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            errorMessage = "READ_SMS permission is required to import historical bank SMS. Financial data remains 100% on your device."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import SMS History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "SPENDORA operates 100% offline. Your historical SMS messages are parsed locally on your device. Zero data is ever sent to the cloud.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Date Configuration Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Import Range", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "From: ${selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US))} 00:00:00\nTo: Today (Current Time)",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedDate == LocalDate.now().minusDays(30),
                            onClick = { selectedDate = LocalDate.now().minusDays(30) },
                            label = { Text("Last 30 Days") }
                        )
                        FilterChip(
                            selected = selectedDate == LocalDate.now().minusMonths(3),
                            onClick = { selectedDate = LocalDate.now().minusMonths(3) },
                            label = { Text("Last 3 Months") }
                        )
                        FilterChip(
                            selected = selectedDate == LocalDate.now().minusYears(1),
                            onClick = { selectedDate = LocalDate.now().minusYears(1) },
                            label = { Text("Last 1 Year") }
                        )
                    }
                }
            }

            // Progress or Summary Card
            if (isWorkRunning || isWorkFinished) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentWorkInfo?.state == WorkInfo.State.SUCCEEDED)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isWorkRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Importing Historical SMS...", style = MaterialTheme.typography.titleMedium)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Import Complete", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Divider(Modifier.padding(vertical = 4.dp))

                        Text("Total Examined: $examinedCount", style = MaterialTheme.typography.bodyMedium)
                        Text("Transactions Imported: $importedCount", style = MaterialTheme.typography.bodyMedium)
                        Text("Duplicates Skipped: $duplicateCount", style = MaterialTheme.typography.bodyMedium)
                        Text("Pending Review: $pendingReviewCount", style = MaterialTheme.typography.bodyMedium)
                        Text("Ignored / Non-Financial: $ignoredCount", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Action Buttons
            if (isWorkRunning) {
                OutlinedButton(
                    onClick = { workManager.cancelUniqueWork(HistoricalSmsImportWorker.UNIQUE_WORK_NAME) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Import")
                }
            } else {
                Button(
                    onClick = {
                        errorMessage = null
                        if (selectedDate.isAfter(LocalDate.now())) {
                            errorMessage = "Cannot select a future start date."
                            return@Button
                        }
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                            return@Button
                        }

                        val zone = ZoneId.systemDefault()
                        val startTimestamp = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
                        val endTimestamp = System.currentTimeMillis()

                        val workRequest = OneTimeWorkRequestBuilder<HistoricalSmsImportWorker>()
                            .setInputData(
                                workDataOf(
                                    HistoricalSmsImportWorker.KEY_START_TIMESTAMP to startTimestamp,
                                    HistoricalSmsImportWorker.KEY_END_TIMESTAMP to endTimestamp
                                )
                            )
                            .build()

                        workManager.enqueueUniqueWork(
                            HistoricalSmsImportWorker.UNIQUE_WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (hasPermission) "Start Historical Import" else "Grant Permission & Start")
                }
            }
        }
    }
}
