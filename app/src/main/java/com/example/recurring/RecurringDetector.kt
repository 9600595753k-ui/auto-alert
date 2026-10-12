package com.example.recurring

import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import java.util.Calendar

object RecurringDetector {

    /**
     * Scans transaction history and detects recurring payment patterns based on merchant similarity,
     * amount similarity, intervals (weekly, monthly, quarterly, annual), and AutoPay tags.
     */
    fun detectRecurringPayments(transactions: List<TransactionEntity>): List<RecurringPaymentEntity> {
        val debits = transactions.filter { it.type == "DEBIT" && it.status == "COMPLETED" && !it.isExcluded }
        val groupedByMerchant = debits.groupBy { it.merchant.trim().lowercase() }
        val detectedList = mutableListOf<RecurringPaymentEntity>()

        for ((_, merchantTxs) in groupedByMerchant) {
            if (merchantTxs.isEmpty()) continue
            val sorted = merchantTxs.sortedByDescending { it.timestamp }
            val latest = sorted.first()
            val merchantDisplayName = latest.merchant

            // If an explicit AutoPay keyword was found in transaction mode
            val hasAutoPayTag = sorted.any { it.paymentMode == "AUTO_PAY" }

            if (sorted.size >= 2) {
                // Calculate time differences between consecutive occurrences
                val intervalsDays = mutableListOf<Long>()
                for (i in 0 until sorted.size - 1) {
                    val diffMillis = sorted[i].timestamp - sorted[i + 1].timestamp
                    val days = diffMillis / (1000 * 60 * 60 * 24)
                    if (days > 0) intervalsDays.add(days)
                }

                if (intervalsDays.isNotEmpty()) {
                    val avgIntervalDays = intervalsDays.average()
                    val frequency = determineFrequency(avgIntervalDays)
                    val nextDueDate = calculateNextDueDate(latest.timestamp, frequency)

                    // Average amount or latest amount
                    val expectedAmount = latest.amountPaise
                    val confidence = calculateConfidence(sorted.size, intervalsDays, hasAutoPayTag)

                    detectedList.add(
                        RecurringPaymentEntity(
                            merchantName = merchantDisplayName,
                            category = latest.category,
                            expectedAmountPaise = expectedAmount,
                            frequency = frequency,
                            lastPaymentTimestamp = latest.timestamp,
                            nextDueTimestamp = nextDueDate,
                            status = "ACTIVE",
                            confidence = confidence,
                            bankAccountMasked = latest.accountMasked,
                            reminderDaysBefore = 2,
                            isMandateVerified = false, // Always mark mandate as observed, not bank-verified
                            autoDetected = true
                        )
                    )
                }
            } else if (hasAutoPayTag || isKnownSubscriptionMerchant(merchantDisplayName)) {
                // Single transaction, but flagged as AutoPay or known subscription merchant (e.g. Netflix, Spotify)
                val frequency = "MONTHLY"
                val nextDueDate = calculateNextDueDate(latest.timestamp, frequency)
                detectedList.add(
                    RecurringPaymentEntity(
                        merchantName = merchantDisplayName,
                        category = latest.category,
                        expectedAmountPaise = latest.amountPaise,
                        frequency = frequency,
                        lastPaymentTimestamp = latest.timestamp,
                        nextDueTimestamp = nextDueDate,
                        status = "NEEDS_REVIEW",
                        confidence = if (hasAutoPayTag) 0.85f else 0.75f,
                        bankAccountMasked = latest.accountMasked,
                        reminderDaysBefore = 2,
                        isMandateVerified = false,
                        autoDetected = true
                    )
                )
            }
        }

        return detectedList
    }

    private fun determineFrequency(avgIntervalDays: Double): String {
        return when {
            avgIntervalDays in 5.0..10.0 -> "WEEKLY"
            avgIntervalDays in 25.0..35.0 -> "MONTHLY"
            avgIntervalDays in 75.0..105.0 -> "QUARTERLY"
            avgIntervalDays in 330.0..400.0 -> "ANNUALLY"
            avgIntervalDays < 20.0 -> "WEEKLY"
            else -> "MONTHLY"
        }
    }

    fun calculateNextDueDate(lastTimestamp: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = lastTimestamp }
        when (frequency) {
            "WEEKLY" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "QUARTERLY" -> cal.add(Calendar.MONTH, 3)
            "ANNUALLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        val now = System.currentTimeMillis()
        // If next due is in the past, roll it forward until it is in the future
        while (cal.timeInMillis < now) {
            when (frequency) {
                "WEEKLY" -> cal.add(Calendar.DAY_OF_YEAR, 7)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "QUARTERLY" -> cal.add(Calendar.MONTH, 3)
                "ANNUALLY" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun calculateConfidence(txCount: Int, intervals: List<Long>, hasAutoPayTag: Boolean): Float {
        var score = 0.70f
        if (txCount >= 3) score += 0.15f
        if (hasAutoPayTag) score += 0.10f
        // Check regularity of intervals
        val minInterval = intervals.minOrNull() ?: 0
        val maxInterval = intervals.maxOrNull() ?: 0
        if (maxInterval - minInterval <= 4) score += 0.10f
        return score.coerceIn(0.5f, 0.98f)
    }

    private fun isKnownSubscriptionMerchant(merchant: String): Boolean {
        val lower = merchant.lowercase()
        val subscriptions = listOf(
            "netflix", "spotify", "prime", "hotstar", "youtube", "airtel",
            "jio", "bescom", "tata power", "act fibernet", "apple", "google storage",
            "groww", "zerodha", "lic", "bajaj finserv"
        )
        return subscriptions.any { lower.contains(it) }
    }
}
