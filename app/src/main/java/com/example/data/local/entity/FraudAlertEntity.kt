package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fraud_alerts")
data class FraudAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderAddress: String,
    val smsBodySnippet: String,
    val riskLevel: String, // "INFORMATIONAL", "CAUTION", "HIGH_RISK"
    val triggerReason: String,
    val whySuspicious: String,
    val recommendedAction: String,
    val timestamp: Long,
    val isResolved: Boolean = false
)
