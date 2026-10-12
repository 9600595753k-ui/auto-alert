package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        TransactionEntity::class,
        RecurringPaymentEntity::class,
        PaymentReminderEntity::class,
        FraudAlertEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun paymentReminderDao(): PaymentReminderDao
    abstract fun fraudAlertDao(): FraudAlertDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autopay_alert_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
