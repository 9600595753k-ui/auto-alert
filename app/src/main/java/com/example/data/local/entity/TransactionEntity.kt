package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["bankName"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fingerprint: String, // Unique hash for deduplication
    val amountPaise: Long, // Exact integer paise (e.g. ₹500.00 = 50000L)
    val type: String, // "DEBIT", "CREDIT"
    val status: String = "COMPLETED", // "COMPLETED", "PENDING", "FAILED", "REVERSED", "REFUNDED"
    val timestamp: Long,
    val bankName: String, // e.g. "HDFC", "SBI", "ICICI", "Axis", "Kotak", "Paytm", "Other"
    val accountMasked: String, // e.g. "XX1234"
    val merchant: String, // e.g. "Swiggy", "Netflix", "Amazon"
    val category: String, // "Food & Dining", "Bills & Utilities", etc.
    val paymentMode: String = "UPI", // "UPI", "CARD", "NET_BANKING", "ATM", "AUTO_PAY", "IMPS/NEFT", "MANUAL"
    val referenceNumber: String? = null,
    val balanceAfterPaise: Long? = null,
    val rawSmsBody: String? = null,
    val senderAddress: String? = null,
    val confidenceScore: Float = 1.0f,
    val notes: String? = null,
    val tags: String? = null,
    val isExcluded: Boolean = false,
    val isRecurring: Boolean = false,
    val isManuallyAdded: Boolean = false
)
