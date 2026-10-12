package com.example.data.repository

import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.FraudAlertDao
import com.example.data.local.dao.PaymentReminderDao
import com.example.data.local.dao.RecurringPaymentDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.FraudAlertEntity
import com.example.data.local.entity.PaymentReminderEntity
import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allActiveTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllActiveTransactions()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val distinctBanks: Flow<List<String>> = transactionDao.getDistinctBanks()
    val distinctCategories: Flow<List<String>> = transactionDao.getDistinctCategories()

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    suspend fun insert(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> =
        transactionDao.insertAll(transactions)

    suspend fun update(transaction: TransactionEntity) =
        transactionDao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) =
        transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) =
        transactionDao.deleteById(id)

    suspend fun deleteAll() =
        transactionDao.deleteAll()
}

class RecurringRepository(
    private val recurringDao: RecurringPaymentDao,
    private val reminderDao: PaymentReminderDao
) {
    val allRecurringPayments: Flow<List<RecurringPaymentEntity>> = recurringDao.getAllRecurringPayments()
    val activeRecurringPayments: Flow<List<RecurringPaymentEntity>> = recurringDao.getActiveRecurringPayments()
    val pendingReminders: Flow<List<PaymentReminderEntity>> = reminderDao.getPendingReminders()
    val allReminders: Flow<List<PaymentReminderEntity>> = reminderDao.getAllReminders()
    val totalMonthlyCommitmentPaise: Flow<Long> = recurringDao.getTotalMonthlyRecurringCommitmentPaise()

    suspend fun insertRecurring(entity: RecurringPaymentEntity): Long = recurringDao.insert(entity)
    suspend fun insertAllRecurring(list: List<RecurringPaymentEntity>) = recurringDao.insertAll(list)
    suspend fun updateRecurring(entity: RecurringPaymentEntity) = recurringDao.update(entity)
    suspend fun deleteRecurring(entity: RecurringPaymentEntity) = recurringDao.delete(entity)
    suspend fun deleteRecurringById(id: Long) = recurringDao.deleteById(id)

    suspend fun insertReminder(reminder: PaymentReminderEntity): Long = reminderDao.insert(reminder)
    suspend fun insertAllReminders(list: List<PaymentReminderEntity>) = reminderDao.insertAll(list)
    suspend fun markReminderPaid(id: Long) = reminderDao.markAsPaid(id)
    suspend fun snoozeReminder(id: Long, newTime: Long) = reminderDao.snoozeReminder(id, newTime)
    suspend fun deleteReminder(reminder: PaymentReminderEntity) = reminderDao.delete(reminder)
    suspend fun deleteAll() {
        recurringDao.deleteAll()
        reminderDao.deleteAll()
    }
}

class FraudRepository(private val fraudAlertDao: FraudAlertDao) {
    val allAlerts: Flow<List<FraudAlertEntity>> = fraudAlertDao.getAllAlerts()
    val unresolvedAlerts: Flow<List<FraudAlertEntity>> = fraudAlertDao.getUnresolvedAlerts()
    val unresolvedCount: Flow<Int> = fraudAlertDao.getUnresolvedAlertCount()

    suspend fun insert(alert: FraudAlertEntity): Long = fraudAlertDao.insert(alert)
    suspend fun insertAll(alerts: List<FraudAlertEntity>) = fraudAlertDao.insertAll(alerts)
    suspend fun resolveAlert(id: Long) = fraudAlertDao.resolveAlert(id)
    suspend fun delete(alert: FraudAlertEntity) = fraudAlertDao.delete(alert)
    suspend fun deleteAll() = fraudAlertDao.deleteAll()
}

class BudgetRepository(private val budgetDao: BudgetDao) {
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun setBudget(category: String, limitPaise: Long, alertThreshold: Int = 80) {
        budgetDao.insertOrUpdate(BudgetEntity(category, limitPaise, alertThreshold))
    }

    suspend fun insertAll(budgets: List<BudgetEntity>) = budgetDao.insertAll(budgets)
    suspend fun deleteByCategory(category: String) = budgetDao.deleteByCategory(category)
    suspend fun deleteAll() = budgetDao.deleteAll()
}
