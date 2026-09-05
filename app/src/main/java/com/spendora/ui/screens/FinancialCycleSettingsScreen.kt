package com.spendora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendora.data.repository.FinancialCycleRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCycleSettingsScreen(
    cycleRepository: FinancialCycleRepository,
    onNavigateBack: () -> Unit = {}
) {
    val configuredDay by cycleRepository.getCycleStartDay().collectAsState(initial = 1)
    val currentCycle by cycleRepository.getCurrentCycle().collectAsState(initial = null)
    var inputDayText by remember { mutableStateOf(configuredDay.toString()) }
    var saveSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(configuredDay) {
        inputDayText = configuredDay.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Cycle Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "SPENDORA reporting periods align with your salary cycle. Changing your start day is completely non-destructive: zero transactions are ever modified, moved, or deleted.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (currentCycle != null) {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Active Reporting Period", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
                        Text(currentCycle!!.displayLabel, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "From ${currentCycle!!.startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))} to ${currentCycle!!.endDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configure Cycle Start Day", style = MaterialTheme.typography.titleMedium)
                    Text("Enter day of the month (1 to 31):", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = inputDayText,
                        onValueChange = {
                            inputDayText = it
                            errorMessage = null
                            saveSuccess = false
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (saveSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reporting period updated successfully", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = {
                            val day = inputDayText.toIntOrNull()
                            if (day == null || day !in 1..31) {
                                errorMessage = "Please enter a valid day between 1 and 31"
                                return@Button
                            }
                            coroutineScope.launch {
                                cycleRepository.startNewCycle(
                                    cycleStartDay = day,
                                    effectiveFromYearMonth = String.format(Locale.US, "%04d-%02d", LocalDate.now().year, LocalDate.now().monthValue),
                                    notes = "Configured via Settings"
                                )
                                saveSuccess = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Update Reporting Period")
                    }
                }
            }
        }
    }
}
