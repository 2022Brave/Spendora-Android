package com.spendora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Privacy Guarantee") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Zero Cloud. Zero Telemetry. 100% On-Device.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Text("1. Strict Offline Operation", style = MaterialTheme.typography.titleMedium)
                Text("SPENDORA is built without INTERNET permission. The application cannot communicate with any remote server, cloud database, or third-party tracking network.", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                Text("2. On-Device SMS Processing", style = MaterialTheme.typography.titleMedium)
                Text("All financial transaction parsing takes place locally on your phone using deterministic pattern matching. Full raw SMS bodies are never permanently retained for confirmed transactions.", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                Text("3. SMS Permissions", style = MaterialTheme.typography.titleMedium)
                Text("RECEIVE_SMS allows real-time transaction detection when bank alerts arrive. READ_SMS is only requested when you explicitly choose to import previous SMS history.", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                Text("4. Local Biometric Security", style = MaterialTheme.typography.titleMedium)
                Text("App Lock uses Android BiometricPrompt. Biometric templates remain securely inside the device hardware security module and are never accessible to SPENDORA.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
