package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.CurrencyUtils
import com.example.core.util.DateUtils
import com.example.data.demo.DemoDataGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.FraudAlertEntity
import com.example.data.local.entity.PaymentReminderEntity
import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BudgetRepository
import com.example.data.repository.FraudRepository
import com.example.data.repository.RecurringRepository
import com.example.data.repository.TransactionRepository
import com.example.fraud.FraudAnalysisResult
import com.example.fraud.FraudDetector
import com.example.recurring.RecurringDetector
import com.example.sms.parser.BankSmsParser
import com.example.sms.parser.ParsedSmsResult
import com.example.sms.scanner.ScanSummary
import com.example.sms.scanner.SmsInboxScanner
import com.example.voice.VoiceAlertManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar

data class CategorySpend(
    val category: String,
    val totalPaise: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class DashboardMetrics(
    val estimatedBalancePaise: Long = 0L,
    val monthlyIncomePaise: Long = 0L,
    val monthlyExpensePaise: Long = 0L,
    val netSavingsPaise: Long = 0L,
    val upcomingDeductionsCount: Int = 0,
    val activeSubscriptionsCount: Int = 0,
    val activeEmisCount: Int = 0,
    val monthlyRecurringCommitmentPaise: Long = 0L,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val topMerchants: List<Pair<String, Long>> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionRepo = TransactionRepository(database.transactionDao())
    private val recurringRepo = RecurringRepository(database.recurringPaymentDao(), database.paymentReminderDao())
    private val fraudRepo = FraudRepository(database.fraudAlertDao())
    private val budgetRepo = BudgetRepository(database.budgetDao())
    val userPrefs = UserPreferencesRepository(application)
    private val voiceAlertManager = VoiceAlertManager(application)
    val authManager = com.example.auth.AuthManager(application)
    val firestoreRepo = com.example.data.firestore.FirestoreRepository(application)

    val currentUser = authManager.currentUser
    val isCloudSyncing = MutableStateFlow(false)

    // Raw reactive flows
    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.allActiveTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurring: StateFlow<List<RecurringPaymentEntity>> = recurringRepo.allRecurringPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReminders: StateFlow<List<PaymentReminderEntity>> = recurringRepo.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fraudAlerts: StateFlow<List<FraudAlertEntity>> = fraudRepo.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unresolvedFraudCount: StateFlow<Int> = fraudRepo.unresolvedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val budgets: StateFlow<List<BudgetEntity>> = budgetRepo.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preferences flows
    val hideAmounts: StateFlow<Boolean> = userPrefs.hideAmounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isOnboardingCompleted: StateFlow<Boolean> = userPrefs.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDemoDataLoaded: StateFlow<Boolean> = userPrefs.demoDataLoaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedPeriod: StateFlow<String> = userPrefs.selectedPeriod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "THIS_MONTH")

    val ttsLanguage: StateFlow<String> = userPrefs.ttsLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val ttsSpeechRate: StateFlow<Float> = userPrefs.ttsSpeechRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val appLockEnabled: StateFlow<Boolean> = userPrefs.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // SMS Scanning & Sync states
    val isScanningSms = MutableStateFlow(false)
    val lastScanSummary = MutableStateFlow<ScanSummary?>(null)

    // Filter states for Passbook
    val passbookSearchQuery = MutableStateFlow("")
    val passbookSelectedType = MutableStateFlow("ALL") // "ALL", "DEBIT", "CREDIT"
    val passbookSelectedCategory = MutableStateFlow("ALL")
    val passbookSelectedBank = MutableStateFlow("ALL")

    // Filtered transactions for passbook
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        passbookSearchQuery,
        passbookSelectedType,
        passbookSelectedCategory,
        passbookSelectedBank
    ) { txs, query, type, cat, bank ->
        txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.merchant.contains(query, ignoreCase = true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    (tx.referenceNumber?.contains(query, ignoreCase = true) == true) ||
                    tx.bankName.contains(query, ignoreCase = true)

            val matchesType = type == "ALL" || tx.type.equals(type, ignoreCase = true)
            val matchesCategory = cat == "ALL" || tx.category.equals(cat, ignoreCase = true)
            val matchesBank = bank == "ALL" || tx.bankName.equals(bank, ignoreCase = true)

            matchesQuery && matchesType && matchesCategory && matchesBank
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Metrics Calculation
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allTransactions,
        allRecurring,
        pendingReminders,
        selectedPeriod
    ) { txs, recurring, reminders, period ->
        calculateMetrics(txs, recurring, reminders, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        // Automatically check if first launch or needs recurring scan
        viewModelScope.launch {
            userPrefs.isOnboardingCompleted.collect { completed ->
                if (!completed) {
                    // Prepopulate budgets if empty
                    budgetRepo.insertAll(DemoDataGenerator.generateSampleBudgets())
                }
            }
        }
    }

    private fun calculateMetrics(
        txs: List<TransactionEntity>,
        recurring: List<RecurringPaymentEntity>,
        reminders: List<PaymentReminderEntity>,
        period: String
    ): DashboardMetrics {
        val now = System.currentTimeMillis()
        val startTime = when (period) {
            "TODAY" -> DateUtils.getStartOfDay(now)
            "THIS_WEEK" -> DateUtils.getStartOfWeek(now)
            "THIS_MONTH" -> DateUtils.getStartOfMonth(now)
            else -> 0L
        }

        val periodTxs = if (startTime > 0L) {
            txs.filter { it.timestamp >= startTime }
        } else {
            txs
        }

        var totalIncome = 0L
        var totalExpense = 0L
        val categoryMap = mutableMapOf<String, Long>()
        val categoryCountMap = mutableMapOf<String, Int>()
        val merchantExpenseMap = mutableMapOf<String, Long>()

        // Estimated balance: sum of all credits minus all debits
        var netBalance = 0L
        for (tx in txs) {
            if (tx.status == "COMPLETED" && !tx.isExcluded) {
                if (tx.type == "CREDIT") {
                    netBalance += tx.amountPaise
                } else if (tx.type == "DEBIT") {
                    netBalance -= tx.amountPaise
                }
            }
        }

        for (tx in periodTxs) {
            if (tx.status == "COMPLETED" && !tx.isExcluded) {
                if (tx.type == "CREDIT") {
                    totalIncome += tx.amountPaise
                } else if (tx.type == "DEBIT") {
                    totalExpense += tx.amountPaise
                    categoryMap[tx.category] = (categoryMap[tx.category] ?: 0L) + tx.amountPaise
                    categoryCountMap[tx.category] = (categoryCountMap[tx.category] ?: 0) + 1
                    merchantExpenseMap[tx.merchant] = (merchantExpenseMap[tx.merchant] ?: 0L) + tx.amountPaise
                }
            }
        }

        val categoryBreakdown = categoryMap.map { (cat, amount) ->
            val percentage = if (totalExpense > 0L) (amount.toFloat() / totalExpense.toFloat()) * 100f else 0f
            CategorySpend(
                category = cat,
                totalPaise = amount,
                transactionCount = categoryCountMap[cat] ?: 1,
                percentage = percentage
            )
        }.sortedByDescending { it.totalPaise }

        val topMerchants = merchantExpenseMap.toList().sortedByDescending { it.second }.take(5)

        val activeSubs = recurring.count { it.status == "ACTIVE" && it.category == "Entertainment" }
        val activeEmis = recurring.count { it.status == "ACTIVE" && it.category == "Loans & EMI" }
        val recurringCommitment = recurring.filter { it.status == "ACTIVE" }.sumOf { it.expectedAmountPaise }

        return DashboardMetrics(
            estimatedBalancePaise = netBalance,
            monthlyIncomePaise = totalIncome,
            monthlyExpensePaise = totalExpense,
            netSavingsPaise = totalIncome - totalExpense,
            upcomingDeductionsCount = reminders.count { !it.isPaid },
            activeSubscriptionsCount = activeSubs,
            activeEmisCount = activeEmis,
            monthlyRecurringCommitmentPaise = recurringCommitment,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants
        )
    }

    fun loadDemoData() {
        viewModelScope.launch {
            transactionRepo.insertAll(DemoDataGenerator.generateSampleTransactions())
            recurringRepo.insertAllRecurring(DemoDataGenerator.generateSampleRecurring())
            recurringRepo.insertAllReminders(DemoDataGenerator.generateSampleReminders())
            fraudRepo.insertAll(DemoDataGenerator.generateSampleFraudAlerts())
            budgetRepo.insertAll(DemoDataGenerator.generateSampleBudgets())
            userPrefs.setDemoDataLoaded(true)
        }
    }

    fun scanDeviceSmsInbox(context: Context) {
        viewModelScope.launch {
            isScanningSms.value = true
            try {
                val summary = SmsInboxScanner.scanAndSyncInbox(context)
                lastScanSummary.value = summary
                triggerRecurringDetection()
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                isScanningSms.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepo.deleteAll()
            recurringRepo.deleteAll()
            fraudRepo.deleteAll()
            userPrefs.setDemoDataLoaded(false)
        }
    }

    fun addManualTransaction(
        amountRupees: Double,
        type: String,
        merchant: String,
        category: String,
        bankName: String,
        accountMasked: String,
        notes: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong()
            val fingerprint = "manual_${System.currentTimeMillis()}_${merchant.hashCode()}"
            val entity = TransactionEntity(
                fingerprint = fingerprint,
                amountPaise = amountPaise,
                type = type,
                status = "COMPLETED",
                timestamp = timestamp,
                bankName = bankName.ifBlank { "Personal" },
                accountMasked = accountMasked.ifBlank { "Cash/Manual" },
                merchant = merchant.ifBlank { "Manual Entry" },
                category = category,
                paymentMode = "MANUAL",
                notes = notes,
                confidenceScore = 1.0f,
                isManuallyAdded = true
            )
            transactionRepo.insert(entity)
            triggerRecurringDetection()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.update(transaction)
            triggerRecurringDetection()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.delete(transaction)
            triggerRecurringDetection()
        }
    }

    fun toggleExcludeTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.update(transaction.copy(isExcluded = !transaction.isExcluded))
        }
    }

    fun testParseSms(body: String, sender: String = ""): ParsedSmsResult {
        return BankSmsParser.parse(body, sender)
    }

    fun commitParsedSms(result: ParsedSmsResult, rawBody: String, sender: String) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                fingerprint = result.fingerprint.ifBlank { "import_${System.currentTimeMillis()}" },
                amountPaise = result.amountPaise,
                type = result.type,
                status = result.status,
                timestamp = result.parsedTimestamp ?: System.currentTimeMillis(),
                bankName = result.bankName,
                accountMasked = result.accountMasked,
                merchant = result.merchant,
                category = result.category,
                paymentMode = result.paymentMode,
                referenceNumber = result.referenceNumber,
                balanceAfterPaise = result.balanceAfterPaise,
                rawSmsBody = rawBody,
                senderAddress = sender,
                confidenceScore = result.confidenceScore,
                isRecurring = result.paymentMode == "AUTO_PAY"
            )
            transactionRepo.insert(entity)
            triggerRecurringDetection()
        }
    }

    fun testFraudAnalysis(body: String, sender: String = ""): FraudAnalysisResult {
        return FraudDetector.analyze(body, sender)
    }

    fun addFraudAlert(alert: FraudAlertEntity) {
        viewModelScope.launch {
            fraudRepo.insert(alert)
        }
    }

    fun resolveFraudAlert(id: Long) {
        viewModelScope.launch {
            fraudRepo.resolveAlert(id)
        }
    }

    fun markReminderPaid(id: Long) {
        viewModelScope.launch {
            recurringRepo.markReminderPaid(id)
        }
    }

    fun snoozeReminder(id: Long, days: Int = 2) {
        viewModelScope.launch {
            val newTime = System.currentTimeMillis() + (days * 86400000L)
            recurringRepo.snoozeReminder(id, newTime)
        }
    }

    fun addRecurringPayment(
        merchantName: String,
        category: String,
        amountRupees: Double,
        frequency: String,
        bankAccount: String = "All Accounts"
    ) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong()
            val now = System.currentTimeMillis()
            val nextDue = RecurringDetector.calculateNextDueDate(now, frequency)
            val entity = RecurringPaymentEntity(
                merchantName = merchantName,
                category = category,
                expectedAmountPaise = amountPaise,
                frequency = frequency,
                lastPaymentTimestamp = now,
                nextDueTimestamp = nextDue,
                status = "ACTIVE",
                confidence = 1.0f,
                bankAccountMasked = bankAccount,
                autoDetected = false
            )
            val newId = recurringRepo.insertRecurring(entity)
            // Schedule reminder
            val reminder = PaymentReminderEntity(
                recurringPaymentId = newId,
                title = "$merchantName ($frequency)",
                amountPaise = amountPaise,
                dueDateTimestamp = nextDue,
                reminderTimestamp = nextDue - (2 * 86400000L),
                isPaid = false
            )
            recurringRepo.insertReminder(reminder)
        }
    }

    fun updateRecurringStatus(recurring: RecurringPaymentEntity, newStatus: String) {
        viewModelScope.launch {
            recurringRepo.updateRecurring(recurring.copy(status = newStatus))
        }
    }

    fun deleteRecurring(recurring: RecurringPaymentEntity) {
        viewModelScope.launch {
            recurringRepo.deleteRecurring(recurring)
        }
    }

    fun setBudget(category: String, amountRupees: Double) {
        viewModelScope.launch {
            val limitPaise = (amountRupees * 100).toLong()
            budgetRepo.setBudget(category, limitPaise)
        }
    }

    fun toggleHideAmounts() {
        viewModelScope.launch {
            userPrefs.toggleHideAmounts()
        }
    }

    fun setSelectedPeriod(period: String) {
        viewModelScope.launch {
            userPrefs.setSelectedPeriod(period)
        }
    }

    fun setTtsLanguage(code: String) {
        viewModelScope.launch {
            userPrefs.setTtsLanguage(code)
        }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            userPrefs.setTtsSpeechRate(rate)
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setTtsEnabled(enabled)
        }
    }

    fun setAppLock(enabled: Boolean, pin: String = "1234") {
        viewModelScope.launch {
            userPrefs.setAppLock(enabled, pin)
        }
    }

    fun testVoiceReminder(merchant: String, amountPaise: Long, lang: String = "en", rate: Float = 1.0f) {
        voiceAlertManager.speakReminder(
            merchantName = merchant,
            amountPaise = amountPaise,
            daysRemaining = 2,
            languageCode = lang,
            speechRate = rate
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPrefs.setOnboardingCompleted(true)
        }
    }

    fun triggerRecurringDetection() {
        viewModelScope.launch {
            val txs = allTransactions.value
            val detected = RecurringDetector.detectRecurringPayments(txs)
            for (rec in detected) {
                // If not already in DB, add it
                val existing = recurringRepo.allRecurringPayments.stateIn(viewModelScope).value
                if (existing.none { it.merchantName.equals(rec.merchantName, ignoreCase = true) }) {
                    val id = recurringRepo.insertRecurring(rec)
                    recurringRepo.insertReminder(
                        PaymentReminderEntity(
                            recurringPaymentId = id,
                            title = "${rec.merchantName} (${rec.frequency})",
                            amountPaise = rec.expectedAmountPaise,
                            dueDateTimestamp = rec.nextDueTimestamp,
                            reminderTimestamp = rec.nextDueTimestamp - (2 * 86400000L),
                            isPaid = false
                        )
                    )
                }
            }
        }
    }

    // CSV Export & Import (100% on-device private data portability)
    suspend fun exportCsv(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val txs = allTransactions.value
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write("Date,Time,Type,Amount_INR,Merchant,Category,Bank,Account,PaymentMode,Reference,Status,Notes\n")
                    for (tx in txs) {
                        val rupees = tx.amountPaise.toDouble() / 100.0
                        val dateStr = DateUtils.formatDate(tx.timestamp)
                        val timeStr = DateUtils.formatDateTime(tx.timestamp)
                        writer.write(
                            "\"$dateStr\",\"$timeStr\",\"${tx.type}\",${rupees},\"${tx.merchant}\",\"${tx.category}\",\"${tx.bankName}\",\"${tx.accountMasked}\",\"${tx.paymentMode}\",\"${tx.referenceNumber ?: ""}\",\"${tx.status}\",\"${tx.notes ?: ""}\"\n"
                        )
                    }
                }
            }
            Result.success(txs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportCsvAsync(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportCsv(context, uri)
        }
    }

    fun signInWithGoogle(activity: android.app.Activity, onError: (String) -> Unit) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                viewModelScope.launch {
                    firestoreRepo.saveUserProfile(user)
                    syncAllToCloud()
                }
            },
            onError = onError
        )
    }

    fun signOutGoogle() {
        authManager.signOut(viewModelScope)
    }

    fun syncAllToCloud() {
        viewModelScope.launch {
            if (currentUser.value != null) {
                isCloudSyncing.value = true
                try {
                    firestoreRepo.syncAllLocalToCloud(
                        allTransactions.value,
                        allRecurring.value,
                        budgets.value
                    )
                } catch (e: Exception) {
                    // Handled gracefully
                } finally {
                    isCloudSyncing.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAlertManager.shutdown()
        authManager.cleanup()
    }
}
