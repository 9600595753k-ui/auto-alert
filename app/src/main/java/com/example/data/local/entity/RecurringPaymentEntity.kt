package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_payments",
    indices = [
        Index(value = ["merchantName"]),
        Index(value = ["nextDueTimestamp"])
    ]
)
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val category: String,
    val expectedAmountPaise: Long,
    val frequency: String, // "WEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY"
    val lastPaymentTimestamp: Long,
    val nextDueTimestamp: Long,
    val status: String = "ACTIVE", // "ACTIVE", "PAUSED", "CANCELLED", "NEEDS_REVIEW"
    val confidence: Float = 0.9f,
    val bankAccountMasked: String = "All Accounts",
    val reminderDaysBefore: Int = 2,
    val isMandateVerified: Boolean = false, // Distinguish observed recurring deduction vs verified mandate
    val autoDetected: Boolean = true
)
