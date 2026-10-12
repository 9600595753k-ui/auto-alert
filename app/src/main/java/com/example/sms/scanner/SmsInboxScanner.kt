package com.example.sms.scanner

import android.content.Context
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FraudAlertEntity
import com.example.data.local.entity.PaymentReminderEntity
import com.example.data.local.entity.TransactionEntity
import com.example.fraud.FraudDetector
import com.example.recurring.RecurringDetector
import com.example.sms.parser.BankSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanSummary(
    val totalScanned: Int,
    val transactionsFound: Int,
    val debitsCount: Int,
    val creditsCount: Int,
    val newAdded: Int,
    val fraudAlertsDetected: Int
)

object SmsInboxScanner {

    /**
     * Reads all bank and financial SMS messages from the device inbox,
     * categorizes them, extracts exact amounts (in paise), and inserts them
     * into the local Room database with deduplication.
     */
    suspend fun scanAndSyncInbox(context: Context): ScanSummary = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val transactionDao = database.transactionDao()
        val fraudDao = database.fraudAlertDao()
        val recurringDao = database.recurringPaymentDao()
        val reminderDao = database.paymentReminderDao()

        var totalScanned = 0
        var transactionsFound = 0
        var debitsCount = 0
        var creditsCount = 0
        var newAdded = 0
        var fraudAlertsDetected = 0

        val contentResolver = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    totalScanned++
                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isBlank()) continue

                    // 1. Scan for Fraud / Phishing / Scams
                    val fraudResult = FraudDetector.analyze(body, address)
                    if (fraudResult.isSuspicious) {
                        fraudAlertsDetected++
                        val alert = FraudAlertEntity(
                            senderAddress = address,
                            smsBodySnippet = body.take(150),
                            riskLevel = fraudResult.riskLevel,
                            triggerReason = fraudResult.triggerReason,
                            whySuspicious = fraudResult.whySuspicious,
                            recommendedAction = fraudResult.recommendedAction,
                            timestamp = date
                        )
                        fraudDao.insert(alert)
                    }

                    // 2. Parse Financial Transaction
                    val parsed = BankSmsParser.parse(body, address, date)
                    if (parsed.isTransaction && parsed.amountPaise > 0L) {
                        transactionsFound++
                        if (parsed.type == "DEBIT") debitsCount++ else creditsCount++

                        // Deduplication check via unique fingerprint
                        val existing = transactionDao.findByFingerprint(parsed.fingerprint)
                        if (existing == null) {
                            val entity = TransactionEntity(
                                fingerprint = parsed.fingerprint,
                                amountPaise = parsed.amountPaise,
                                type = parsed.type,
                                status = parsed.status,
                                timestamp = date,
                                bankName = parsed.bankName,
                                accountMasked = parsed.accountMasked,
                                merchant = parsed.merchant,
                                category = parsed.category,
                                paymentMode = parsed.paymentMode,
                                referenceNumber = parsed.referenceNumber,
                                balanceAfterPaise = parsed.balanceAfterPaise,
                                rawSmsBody = body,
                                senderAddress = address,
                                confidenceScore = parsed.confidenceScore,
                                isRecurring = parsed.paymentMode == "AUTO_PAY"
                            )
                            val insertedId = transactionDao.insert(entity)
                            if (insertedId > 0) {
                                newAdded++
                            }
                        }
                    }
                }
            }

        } catch (e: SecurityException) {
            // Permission not granted by user yet
        } catch (e: Exception) {
            // Other reading errors
        }

        ScanSummary(
            totalScanned = totalScanned,
            transactionsFound = transactionsFound,
            debitsCount = debitsCount,
            creditsCount = creditsCount,
            newAdded = newAdded,
            fraudAlertsDetected = fraudAlertsDetected
        )
    }
}
