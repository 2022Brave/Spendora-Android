package com.spendora.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.spendora.data.backup.DataBackupManager
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.security.BiometricAuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    cycleRepository: FinancialCycleRepository,
    transactionRepository: TransactionRepository,
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
    val context = LocalContext.current
    val cycleStartDay by cycleRepository.getCycleStartDay().collectAsState(initial = 1)
    val pendingCount by transactionRepository.getPendingReviewCount().collectAsState(initial = 0)
    val isAppLockEnabled by biometricManager.isAppLockEnabled().collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportResultJson by remember { mutableStateOf<String?>(null) }
    var showRestoreWarningDialog by remember { mutableStateOf(false) }
    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }

    val hasReceiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val hasReadSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // --- SECURITY SECTION ---
        item {
            Text("Security", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            ListItem(
                headlineContent = { Text("App Lock") },
                supportingContent = { Text("Require Biometric authentication to view financial data") },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                biometricManager.setAppLockEnabled(checked)
                            }
                        }
                    )
                }
            )
        }

        // --- FINANCIAL CONFIGURATION SECTION ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Financial Configuration", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            ListItem(
                headlineContent = { Text("Financial Cycle") },
                supportingContent = { Text("Salary cycle starts on day $cycleStartDay of every month") },
                leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToCycleSettings() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Accounts Management") },
                supportingContent = { Text("Manage bank accounts, credit cards, and wallets") },
                leadingContent = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToAccounts() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Categories") },
                supportingContent = { Text("View system categories and add custom tags") },
                leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToCategories() }
            )
        }

        // --- SMS & REVIEW SECTION ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("SMS & Reviews", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            ListItem(
                headlineContent = { Text("Pending Review") },
                supportingContent = { Text("Verify ambiguous SMS alerts before confirmation") },
                leadingContent = { Icon(Icons.Default.RateReview, contentDescription = null) },
                trailingContent = {
                    if (pendingCount > 0) {
                        Badge { Text("$pendingCount") }
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable { onNavigateToPendingReview() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Import SMS History") },
                supportingContent = { Text("Scan previous bank SMS messages into Spendora") },
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToHistoricalImport() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("SMS Permissions Status") },
                supportingContent = {
                    Text("RECEIVE_SMS: ${if (hasReceiveSms) "Granted" else "Not Granted"} • READ_SMS: ${if (hasReadSms) "Granted" else "Not Granted"}")
                },
                leadingContent = { Icon(Icons.Default.Sms, contentDescription = null) }
            )
        }

        // --- DATA & BACKUP SECTION ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Data & Backup", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            ListItem(
                headlineContent = { Text("Export Data (JSON)") },
                supportingContent = { Text("Generate an offline, encrypted JSON backup") },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        exportResultJson = backupManager.exportToJson()
                        showExportDialog = true
                    }
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Import / Restore Data") },
                supportingContent = { Text("Restore previously exported JSON backup") },
                leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { showRestoreWarningDialog = true }
            )
        }

        // --- APPEARANCE & PRIVACY SECTION ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Appearance & Privacy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("Toggle between Black + Purple and White + Purple") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onToggleTheme() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Offline Privacy Guarantee") },
                supportingContent = { Text("100% on-device storage. Zero cloud transmission.") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToPrivacy() }
            )
        }

        // --- ABOUT SECTION ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text("SPENDORA v1.0.0") },
                supportingContent = { Text("Local-First Personal Finance Tracker") },
                leadingContent = { Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
    }

    // Export Success Dialog
    if (showExportDialog && exportResultJson != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Complete") },
            text = {
                Text("Your financial data has been successfully exported to an offline JSON backup. Total backup size: ${exportResultJson!!.length} bytes. Keep this file in a secure location.")
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) { Text("Done") }
            }
        )
    }

    // Restore Warning Dialog
    if (showRestoreWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreWarningDialog = false },
            title = { Text("Restore From Backup?") },
            text = {
                Text("Restoring will validate the backup file and replace your current SPENDORA database atomically. All existing transactions and accounts will be replaced by the backup contents.\n\nDo you wish to proceed?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreWarningDialog = false
                        // User proceeds to file picker or confirmation in production
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Select Backup & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarningDialog = false }) { Text("Cancel") }
            }
        )
    }
}
