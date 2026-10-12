package com.example.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FraudAlertEntity
import com.example.data.local.entity.TransactionEntity
import com.example.fraud.FraudDetector
import com.example.notification.NotificationHelper
import com.example.sms.parser.BankSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sb = StringBuilder()
        var senderAddress = ""
        var timestamp = System.currentTimeMillis()

        for (msg in messages) {
            senderAddress = msg.originatingAddress ?: senderAddress
            sb.append(msg.messageBody)
            timestamp = msg.timestampMillis
        }

        val fullBody = sb.toString()
        val database = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Analyze for Fraud
            val fraudResult = FraudDetector.analyze(fullBody, senderAddress)
            if (fraudResult.isSuspicious) {
                val alert = FraudAlertEntity(
                    senderAddress = senderAddress,
                    smsBodySnippet = fullBody.take(150),
                    riskLevel = fraudResult.riskLevel,
                    triggerReason = fraudResult.triggerReason,
                    whySuspicious = fraudResult.whySuspicious,
                    recommendedAction = fraudResult.recommendedAction,
                    timestamp = timestamp
                )
                database.fraudAlertDao().insert(alert)
                NotificationHelper.showFraudAlert(
                    context = context,
                    notificationId = alert.hashCode(),
                    sender = senderAddress,
                    reason = fraudResult.triggerReason
                )
            }

            // 2. Parse for Financial Bank Transaction
            val parseResult = BankSmsParser.parse(fullBody, senderAddress, timestamp)
            if (parseResult.isTransaction && parseResult.amountPaise > 0) {
                // Check deduplication via unique fingerprint
                val existing = database.transactionDao().findByFingerprint(parseResult.fingerprint)
                if (existing == null) {
                    val entity = TransactionEntity(
                        fingerprint = parseResult.fingerprint,
                        amountPaise = parseResult.amountPaise,
                        type = parseResult.type,
                        status = parseResult.status,
                        timestamp = timestamp,
                        bankName = parseResult.bankName,
                        accountMasked = parseResult.accountMasked,
                        merchant = parseResult.merchant,
                        category = parseResult.category,
                        paymentMode = parseResult.paymentMode,
                        referenceNumber = parseResult.referenceNumber,
                        balanceAfterPaise = parseResult.balanceAfterPaise,
                        rawSmsBody = fullBody,
                        senderAddress = senderAddress,
                        confidenceScore = parseResult.confidenceScore,
                        isRecurring = parseResult.paymentMode == "AUTO_PAY"
                    )
                    database.transactionDao().insert(entity)
                }
            }
        }
    }
}
