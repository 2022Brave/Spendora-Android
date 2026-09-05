package com.spendora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.spendora.data.backup.DataBackupManager
import com.spendora.data.repository.BudgetRepository
import com.spendora.data.repository.FinancialCycleRepository
import com.spendora.data.repository.TransactionRepository
import com.spendora.data.security.BiometricAuthManager
import com.spendora.data.security.BiometricAuthResult
import com.spendora.ui.screens.*
import com.spendora.ui.theme.SpendoraTheme
import kotlinx.coroutines.launch

enum class ScreenState {
    MAIN_NAV,
    PENDING_REVIEW,
    HISTORICAL_IMPORT,
    ACCOUNTS,
    CATEGORIES,
    CYCLE_SETTINGS,
    PRIVACY_POLICY
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SpendoraApplication
        val database = app.database

        val transactionRepo = TransactionRepository(
            transactionDao = database.transactionDao(),
            smsAuditDao = database.smsAuditDao(),
            accountDao = database.accountDao()
        )
        val cycleRepo = FinancialCycleRepository(
            financialCycleDao = database.financialCycleDao(),
            appSettingDao = database.appSettingDao()
        )
        val budgetRepo = BudgetRepository(
            budgetDao = database.budgetDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao()
        )
        val biometricManager = BiometricAuthManager(database.appSettingDao())
        val backupManager = DataBackupManager(database)

        setContent {
            val appThemeSetting by database.appSettingDao().observe("app_theme").collectAsState(initial = "DARK")
            val isDarkTheme = appThemeSetting != "LIGHT"
            val isAppLockEnabled by biometricManager.isAppLockEnabled().collectAsState(initial = false)
            var isUnlocked by remember { mutableStateOf(!isAppLockEnabled) }
            var currentScreen by remember { mutableStateOf(ScreenState.MAIN_NAV) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(isAppLockEnabled) {
                if (isAppLockEnabled && !isUnlocked) {
                    biometricManager.authenticate(this@MainActivity) { result ->
                        if (result is BiometricAuthResult.Success) {
                            isUnlocked = true
                        }
                    }
                } else if (!isAppLockEnabled) {
                    isUnlocked = true
                }
            }

            SpendoraTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLockEnabled && !isUnlocked) {
                        // Locked Screen: Prevents financial data leakage before authentication
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("SPENDORA Locked", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Biometric authentication required",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Button(onClick = {
                                    biometricManager.authenticate(this@MainActivity) { result ->
                                        if (result is BiometricAuthResult.Success) {
                                            isUnlocked = true
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Unlock")
                                }
                            }
                        }
                    } else {
                        // Authorized Application Navigation
                        when (currentScreen) {
                            ScreenState.MAIN_NAV -> MainNavigationScreen(
                                transactionRepository = transactionRepo,
                                cycleRepository = cycleRepo,
                                budgetRepository = budgetRepo,
                                categoryDao = database.categoryDao(),
                                accountDao = database.accountDao(),
                                biometricManager = biometricManager,
                                backupManager = backupManager,
                                onNavigateToPendingReview = { currentScreen = ScreenState.PENDING_REVIEW },
                                onNavigateToHistoricalImport = { currentScreen = ScreenState.HISTORICAL_IMPORT },
                                onNavigateToAccounts = { currentScreen = ScreenState.ACCOUNTS },
                                onNavigateToCategories = { currentScreen = ScreenState.CATEGORIES },
                                onNavigateToCycleSettings = { currentScreen = ScreenState.CYCLE_SETTINGS },
                                onNavigateToPrivacy = { currentScreen = ScreenState.PRIVACY_POLICY },
                                onToggleTheme = {
                                    coroutineScope.launch {
                                        val newTheme = if (isDarkTheme) "LIGHT" else "DARK"
                                        database.appSettingDao().set(
                                            com.spendora.data.entity.AppSettingEntity(
                                                key = "app_theme",
                                                value = newTheme,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }
                            )
                            ScreenState.PENDING_REVIEW -> PendingReviewScreen(
                                repository = transactionRepo,
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                            ScreenState.HISTORICAL_IMPORT -> HistoricalImportScreen(
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                            ScreenState.ACCOUNTS -> AccountsScreen(
                                accountDao = database.accountDao(),
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                            ScreenState.CATEGORIES -> CategoriesScreen(
                                categoryDao = database.categoryDao(),
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                            ScreenState.CYCLE_SETTINGS -> FinancialCycleSettingsScreen(
                                cycleRepository = cycleRepo,
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                            ScreenState.PRIVACY_POLICY -> PrivacyPolicyScreen(
                                onNavigateBack = { currentScreen = ScreenState.MAIN_NAV }
                            )
                        }
                    }
                }
            }
        }
    }
}
