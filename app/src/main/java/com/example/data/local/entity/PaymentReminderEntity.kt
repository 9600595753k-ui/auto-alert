package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_reminders",
    indices = [
        Index(value = ["dueDateTimestamp"]),
        Index(value = ["recurringPaymentId"])
    ]
)
data class PaymentReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recurringPaymentId: Long = 0L,
    val title: String,
    val amountPaise: Long,
    val dueDateTimestamp: Long,
    val reminderTimestamp: Long,
    val isSnoozed: Boolean = false,
    val isPaid: Boolean = false,
    val isNotified: Boolean = false,
    val notes: String? = null
)
