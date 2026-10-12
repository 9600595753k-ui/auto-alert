package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.notification.NotificationHelper
import com.example.ui.components.FintechBottomBar
import com.example.ui.components.FintechTopBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddRecurringModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditBudgetDialog
import com.example.ui.screens.FraudShieldScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ManualAddTransactionModal
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PassbookScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsSandboxScreen
import com.example.ui.screens.TransactionDetailSheet
import com.example.ui.theme.AutoPayAlertTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels for payment reminders and fraud alerts
        NotificationHelper.createNotificationChannels(this)

        setContent {
            AutoPayAlertTheme(darkTheme = true) {
                AutoPayAlertApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AutoPayAlertApp(viewModel: MainViewModel) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val hideAmounts by viewModel.hideAmounts.collectAsStateWithLifecycle()
    val isDemoDataLoaded by viewModel.isDemoDataLoaded.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val recurringPayments by viewModel.allRecurring.collectAsStateWithLifecycle()
    val pendingReminders by viewModel.pendingReminders.collectAsStateWithLifecycle()
    val fraudAlerts by viewModel.fraudAlerts.collectAsStateWithLifecycle()
    val unresolvedFraudCount by viewModel.unresolvedFraudCount.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    val ttsLanguage by viewModel.ttsLanguage.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val isScanningSms by viewModel.isScanningSms.collectAsStateWithLifecycle()
    val lastScanSummary by viewModel.lastScanSummary.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val autoPayAlertsEnabled by viewModel.autoPayAlertsEnabled.collectAsStateWithLifecycle()
    val fraudAlertsEnabled by viewModel.fraudAlertsEnabled.collectAsStateWithLifecycle()
    val highValueAlertsEnabled by viewModel.highValueAlertsEnabled.collectAsStateWithLifecycle()
    val cloudAutoSyncEnabled by viewModel.cloudAutoSyncEnabled.collectAsStateWithLifecycle()

    val searchQuery by viewModel.passbookSearchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.passbookSelectedType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.passbookSelectedCategory.collectAsStateWithLifecycle()
    val selectedBank by viewModel.passbookSelectedBank.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val anyGranted = readGranted || receiveGranted
        hasSmsPermission = anyGranted

        if (anyGranted) {
            viewModel.scanDeviceSmsInbox(context)
        }
    }

    // Automatically request SMS runtime permissions on launch if not yet granted
    LaunchedEffect(Unit) {
        val readCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
        val receiveCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
        if (readCheck != PackageManager.PERMISSION_GRANTED || receiveCheck != PackageManager.PERMISSION_GRANTED) {
            val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            hasSmsPermission = true
            if (recentTransactions.isEmpty()) {
                viewModel.scanDeviceSmsInbox(context)
            }
        }
    }

    // Navigation and Sheet states
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showManualAddModal by remember { mutableStateOf(false) }
    var showAddRecurringModal by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf<Pair<String, Long>?>(null) }

    // Intercept back button to return to Dashboard from secondary screens
    if (currentScreen != Screen.Dashboard && isOnboardingCompleted) {
        BackHandler {
            currentScreen = Screen.Dashboard
        }
    }

    if (!isOnboardingCompleted) {
        val activity = context as? android.app.Activity
        OnboardingScreen(
            currentUser = currentUser,
            onGoogleSignIn = {
                activity?.let { act ->
                    viewModel.signInWithGoogle(act) { _ -> }
                }
            },
            onCompleteOnboarding = { enableDemoData ->
                if (enableDemoData) {
                    viewModel.loadDemoData()
                } else {
                    viewModel.scanDeviceSmsInbox(context)
                }
                viewModel.completeOnboarding()
            },
            selectedLanguage = ttsLanguage,
            onSelectLanguage = { viewModel.setTtsLanguage(it) }
        )
    } else {
        Scaffold(
            topBar = {
                if (currentScreen != Screen.Settings && currentScreen != Screen.SmsSandbox) {
                    FintechTopBar(
                        currentScreen = currentScreen,
                        hideAmounts = hideAmounts,
                        unresolvedFraudCount = unresolvedFraudCount,
                        onToggleHideAmounts = { viewModel.toggleHideAmounts() },
                        onNavigateToSandbox = { currentScreen = Screen.SmsSandbox },
                        onNavigateToSettings = { currentScreen = Screen.Settings }
                    )
                }
            },
            bottomBar = {
                if (currentScreen != Screen.Settings && currentScreen != Screen.SmsSandbox) {
                    FintechBottomBar(
                        currentScreen = currentScreen,
                        unresolvedFraudCount = unresolvedFraudCount,
                        onNavigate = { currentScreen = it }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Dashboard -> {
                        DashboardScreen(
                            metrics = metrics,
                            recentTransactions = recentTransactions,
                            hideAmounts = hideAmounts,
                            selectedPeriod = selectedPeriod,
                            unresolvedFraudCount = unresolvedFraudCount,
                            isDemoDataLoaded = isDemoDataLoaded,
                            isScanningSms = isScanningSms,
                            lastScanSummary = lastScanSummary,
                            onPeriodSelected = { viewModel.setSelectedPeriod(it) },
                            onToggleHideAmounts = { viewModel.toggleHideAmounts() },
                            onLoadDemoData = { viewModel.loadDemoData() },
                            onScanSmsRequested = { viewModel.scanDeviceSmsInbox(context) },
                            onOpenAddTransaction = { showManualAddModal = true },
                            onNavigateToPassbook = { currentScreen = Screen.Passbook },
                            onNavigateToPayments = { currentScreen = Screen.Payments },
                            onNavigateToFraud = { currentScreen = Screen.FraudShield },
                            onNavigateToSandbox = { currentScreen = Screen.SmsSandbox },
                            onTransactionClicked = { selectedTransactionForDetail = it }
                        )
                    }

                    Screen.Passbook -> {
                        val distinctBanks = recentTransactions.map { it.bankName }.distinct().filter { it.isNotBlank() }
                        val distinctCategories = recentTransactions.map { it.category }.distinct().filter { it.isNotBlank() }

                        PassbookScreen(
                            transactions = filteredTransactions,
                            searchQuery = searchQuery,
                            selectedType = selectedType,
                            selectedCategory = selectedCategory,
                            selectedBank = selectedBank,
                            distinctBanks = distinctBanks,
                            distinctCategories = distinctCategories,
                            hideAmounts = hideAmounts,
                            onSearchQueryChange = { viewModel.passbookSearchQuery.value = it },
                            onTypeSelected = { viewModel.passbookSelectedType.value = it },
                            onCategorySelected = { viewModel.passbookSelectedCategory.value = it },
                            onBankSelected = { viewModel.passbookSelectedBank.value = it },
                            onTransactionClicked = { selectedTransactionForDetail = it },
                            onOpenAddTransaction = { showManualAddModal = true },
                            onScanSmsRequested = { viewModel.scanDeviceSmsInbox(context) }
                        )
                    }

                    Screen.Insights -> {
                        InsightsScreen(
                            metrics = metrics,
                            budgets = budgets,
                            hideAmounts = hideAmounts,
                            onOpenEditBudget = { category, limit ->
                                editingBudgetCategory = Pair(category, limit)
                            }
                        )
                    }

                    Screen.Payments -> {
                        PaymentsScreen(
                            reminders = pendingReminders,
                            recurringPayments = recurringPayments,
                            hideAmounts = hideAmounts,
                            onMarkPaid = { viewModel.markReminderPaid(it) },
                            onSnooze = { viewModel.snoozeReminder(it) },
                            onSpeakReminder = { merchant, amount ->
                                viewModel.testVoiceReminder(merchant, amount, ttsLanguage, ttsSpeechRate)
                            },
                            onOpenAddRecurring = { showAddRecurringModal = true },
                            onRecurringClicked = { /* Details or pause/resume toggle */ }
                        )
                    }

                    Screen.FraudShield -> {
                        FraudShieldScreen(
                            alerts = fraudAlerts,
                            onResolveAlert = { viewModel.resolveFraudAlert(it) },
                            onOpenSmsSandbox = { currentScreen = Screen.SmsSandbox }
                        )
                    }

                    Screen.Settings -> {
                        val activity = context as? android.app.Activity
                        SettingsScreen(
                            hideAmounts = hideAmounts,
                            ttsEnabled = true,
                            ttsLanguage = ttsLanguage,
                            ttsSpeechRate = ttsSpeechRate,
                            appLockEnabled = appLockEnabled,
                            isDemoDataLoaded = isDemoDataLoaded,
                            autoPayAlertsEnabled = autoPayAlertsEnabled,
                            fraudAlertsEnabled = fraudAlertsEnabled,
                            highValueAlertsEnabled = highValueAlertsEnabled,
                            cloudAutoSyncEnabled = cloudAutoSyncEnabled,
                            currentUser = currentUser,
                            isCloudSyncing = isCloudSyncing,
                            onGoogleSignIn = {
                                activity?.let { act ->
                                    viewModel.signInWithGoogle(act) { errorMsg -> }
                                }
                            },
                            onGoogleSignOut = { viewModel.signOutGoogle() },
                            onCloudSyncNow = { viewModel.syncAllToCloud() },
                            onToggleHideAmounts = { viewModel.toggleHideAmounts() },
                            onToggleTtsEnabled = { viewModel.setTtsEnabled(it) },
                            onSelectTtsLanguage = { viewModel.setTtsLanguage(it) },
                            onSelectTtsSpeechRate = { viewModel.setTtsSpeechRate(it) },
                            onTestVoice = { lang, rate ->
                                viewModel.testVoiceReminder("Netflix AutoPay", 64900L, lang, rate)
                            },
                            onToggleAppLock = { viewModel.setAppLock(it, "1234") },
                            onToggleAutoPayAlerts = { viewModel.setAutoPayAlertsEnabled(it) },
                            onToggleFraudAlerts = { viewModel.setFraudAlertsEnabled(it) },
                            onToggleHighValueAlerts = { viewModel.setHighValueAlertsEnabled(it) },
                            onToggleCloudAutoSync = { viewModel.setCloudAutoSyncEnabled(it) },
                            onLoadDemoData = { viewModel.loadDemoData() },
                            onClearAllData = { viewModel.clearAllData() },
                            onExportCsv = { uri ->
                                viewModel.exportCsvAsync(viewModel.getApplication(), uri)
                            },
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }

                    Screen.SmsSandbox -> {
                        SmsSandboxScreen(
                            onCommitParsedTransaction = { result, body, sender ->
                                viewModel.commitParsedSms(result, body, sender)
                            },
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }

                    else -> {}
                }
            }
        }

        // Modals & Bottom Sheets
        if (showManualAddModal) {
            ManualAddTransactionModal(
                onDismiss = { showManualAddModal = false },
                onSave = { amount, type, merchant, category, bank, account, notes ->
                    viewModel.addManualTransaction(amount, type, merchant, category, bank, account, notes)
                }
            )
        }

        selectedTransactionForDetail?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                hideAmounts = hideAmounts,
                onDismiss = { selectedTransactionForDetail = null },
                onSaveTransaction = { updatedTx ->
                    viewModel.updateTransaction(updatedTx)
                    selectedTransactionForDetail = updatedTx
                },
                onToggleExclude = { viewModel.toggleExcludeTransaction(it) },
                onDelete = {
                    viewModel.deleteTransaction(it)
                    selectedTransactionForDetail = null
                },
                onUpdateNotes = { entity, notes ->
                    viewModel.updateTransaction(entity.copy(notes = notes))
                }
            )
        }

        editingBudgetCategory?.let { (category, currentLimit) ->
            EditBudgetDialog(
                category = category,
                currentLimitPaise = currentLimit,
                onDismiss = { editingBudgetCategory = null },
                onSave = { cat, newAmount ->
                    viewModel.setBudget(cat, newAmount)
                }
            )
        }

        if (showAddRecurringModal) {
            AddRecurringModal(
                onDismiss = { showAddRecurringModal = false },
                onSave = { merchant, category, amount, freq ->
                    viewModel.addRecurringPayment(merchant, category, amount, freq)
                }
            )
        }
    }
}
