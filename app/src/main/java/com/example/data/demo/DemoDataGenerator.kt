package com.example.data.demo

import com.example.core.util.DateUtils
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.FraudAlertEntity
import com.example.data.local.entity.PaymentReminderEntity
import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import java.util.Calendar

object DemoDataGenerator {

    fun generateSampleTransactions(): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            TransactionEntity(
                fingerprint = "demo_tx_salary_1",
                amountPaise = 8500000L, // ₹85,000.00
                type = "CREDIT",
                status = "COMPLETED",
                timestamp = now - (12 * day),
                bankName = "HDFC",
                accountMasked = "XX4589",
                merchant = "Company Salary",
                category = "Salary & Income",
                paymentMode = "IMPS/NEFT",
                referenceNumber = "NEFT903482109",
                balanceAfterPaise = 11250000L,
                confidenceScore = 0.98f,
                notes = "Monthly payroll credit"
            ),
            TransactionEntity(
                fingerprint = "demo_tx_netflix_1",
                amountPaise = 64900L, // ₹649.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (10 * day),
                bankName = "HDFC",
                accountMasked = "XX4589",
                merchant = "Netflix",
                category = "Entertainment",
                paymentMode = "AUTO_PAY",
                referenceNumber = "UPI923841029",
                balanceAfterPaise = 11185100L,
                confidenceScore = 0.95f,
                isRecurring = true,
                notes = "AutoPay monthly premium plan"
            ),
            TransactionEntity(
                fingerprint = "demo_tx_swiggy_1",
                amountPaise = 42000L, // ₹420.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (1 * day),
                bankName = "SBI",
                accountMasked = "XX9102",
                merchant = "Swiggy",
                category = "Food & Dining",
                paymentMode = "UPI",
                referenceNumber = "UPI982301934",
                balanceAfterPaise = 4235000L,
                confidenceScore = 0.96f
            ),
            TransactionEntity(
                fingerprint = "demo_tx_groww_sip",
                amountPaise = 500000L, // ₹5,000.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (5 * day),
                bankName = "HDFC",
                accountMasked = "XX4589",
                merchant = "Groww Investment",
                category = "Investments",
                paymentMode = "AUTO_PAY",
                referenceNumber = "SIP48291039",
                balanceAfterPaise = 10685100L,
                confidenceScore = 0.97f,
                isRecurring = true,
                notes = "Index Fund SIP Auto-debit"
            ),
            TransactionEntity(
                fingerprint = "demo_tx_amazon_1",
                amountPaise = 249900L, // ₹2,499.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (3 * day),
                bankName = "ICICI",
                accountMasked = "XX3312",
                merchant = "Amazon",
                category = "Shopping",
                paymentMode = "CARD",
                referenceNumber = "TXN482019482",
                confidenceScore = 0.92f
            ),
            TransactionEntity(
                fingerprint = "demo_tx_airtel_bill",
                amountPaise = 99900L, // ₹999.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (7 * day),
                bankName = "SBI",
                accountMasked = "XX9102",
                merchant = "Airtel",
                category = "Bills & Utilities",
                paymentMode = "AUTO_PAY",
                referenceNumber = "UPI938210398",
                confidenceScore = 0.95f,
                isRecurring = true
            ),
            TransactionEntity(
                fingerprint = "demo_tx_bajaj_emi",
                amountPaise = 385000L, // ₹3,850.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (8 * day),
                bankName = "HDFC",
                accountMasked = "XX4589",
                merchant = "Bajaj Finserv EMI",
                category = "Loans & EMI",
                paymentMode = "AUTO_PAY",
                referenceNumber = "EMI82938102",
                confidenceScore = 0.98f,
                isRecurring = true,
                notes = "Laptop EMI installment 4 of 12"
            ),
            TransactionEntity(
                fingerprint = "demo_tx_zomato_1",
                amountPaise = 56000L, // ₹560.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - 7200000L, // 2 hours ago
                bankName = "SBI",
                accountMasked = "XX9102",
                merchant = "Zomato",
                category = "Food & Dining",
                paymentMode = "UPI",
                referenceNumber = "UPI982304910",
                confidenceScore = 0.94f
            ),
            TransactionEntity(
                fingerprint = "demo_tx_uber_1",
                amountPaise = 24000L, // ₹240.00
                type = "DEBIT",
                status = "COMPLETED",
                timestamp = now - (2 * day),
                bankName = "ICICI",
                accountMasked = "XX3312",
                merchant = "Uber",
                category = "Travel & Commute",
                paymentMode = "UPI",
                referenceNumber = "UPI948291039",
                confidenceScore = 0.90f
            ),
            TransactionEntity(
                fingerprint = "demo_tx_refund_1",
                amountPaise = 35000L, // ₹350.00
                type = "CREDIT",
                status = "REFUNDED",
                timestamp = now - (4 * day),
                bankName = "HDFC",
                accountMasked = "XX4589",
                merchant = "Swiggy",
                category = "Food & Dining",
                paymentMode = "UPI",
                referenceNumber = "REF98201948",
                confidenceScore = 0.94f,
                notes = "Cancelled order item refund"
            )
        )
    }

    fun generateSampleRecurring(): List<RecurringPaymentEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            RecurringPaymentEntity(
                merchantName = "Netflix",
                category = "Entertainment",
                expectedAmountPaise = 64900L,
                frequency = "MONTHLY",
                lastPaymentTimestamp = now - (10 * day),
                nextDueTimestamp = now + (20 * day),
                status = "ACTIVE",
                confidence = 0.96f,
                bankAccountMasked = "HDFC XX4589",
                isMandateVerified = false,
                autoDetected = true
            ),
            RecurringPaymentEntity(
                merchantName = "Groww Investment",
                category = "Investments",
                expectedAmountPaise = 500000L,
                frequency = "MONTHLY",
                lastPaymentTimestamp = now - (5 * day),
                nextDueTimestamp = now + (25 * day),
                status = "ACTIVE",
                confidence = 0.98f,
                bankAccountMasked = "HDFC XX4589",
                isMandateVerified = false,
                autoDetected = true
            ),
            RecurringPaymentEntity(
                merchantName = "Bajaj Finserv EMI",
                category = "Loans & EMI",
                expectedAmountPaise = 385000L,
                frequency = "MONTHLY",
                lastPaymentTimestamp = now - (8 * day),
                nextDueTimestamp = now + (22 * day),
                status = "ACTIVE",
                confidence = 0.99f,
                bankAccountMasked = "HDFC XX4589",
                isMandateVerified = false,
                autoDetected = true
            ),
            RecurringPaymentEntity(
                merchantName = "Airtel Fiber",
                category = "Bills & Utilities",
                expectedAmountPaise = 99900L,
                frequency = "MONTHLY",
                lastPaymentTimestamp = now - (7 * day),
                nextDueTimestamp = now + (23 * day),
                status = "ACTIVE",
                confidence = 0.92f,
                bankAccountMasked = "SBI XX9102",
                isMandateVerified = false,
                autoDetected = true
            ),
            RecurringPaymentEntity(
                merchantName = "Spotify",
                category = "Entertainment",
                expectedAmountPaise = 11900L,
                frequency = "MONTHLY",
                lastPaymentTimestamp = now - (28 * day),
                nextDueTimestamp = now + (2 * day),
                status = "ACTIVE",
                confidence = 0.94f,
                bankAccountMasked = "ICICI XX3312",
                isMandateVerified = false,
                autoDetected = true
            )
        )
    }

    fun generateSampleReminders(): List<PaymentReminderEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            PaymentReminderEntity(
                title = "Spotify Premium AutoPay",
                amountPaise = 11900L,
                dueDateTimestamp = now + (2 * day),
                reminderTimestamp = now + (1 * day),
                isPaid = false,
                notes = "Ensure at least ₹119 in ICICI account"
            ),
            PaymentReminderEntity(
                title = "Netflix Family Subscription",
                amountPaise = 64900L,
                dueDateTimestamp = now + (20 * day),
                reminderTimestamp = now + (18 * day),
                isPaid = false
            ),
            PaymentReminderEntity(
                title = "Bajaj Finserv Consumer EMI",
                amountPaise = 385000L,
                dueDateTimestamp = now + (22 * day),
                reminderTimestamp = now + (20 * day),
                isPaid = false
            )
        )
    }

    fun generateSampleFraudAlerts(): List<FraudAlertEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            FraudAlertEntity(
                senderAddress = "+919876543210",
                smsBodySnippet = "Dear Customer, Your Electricity power will be disconnected at 9:30 PM due to pending bill. Immediately call 9876543210 or install http://eb-update.apk",
                riskLevel = "HIGH_RISK",
                triggerReason = "Dangerous APK link + Coercive power cut threat",
                whySuspicious = "State electricity boards never demand APK downloads or threaten instant disconnects via unofficial mobile numbers.",
                recommendedAction = "Do NOT call the number or click the link. Disconnect and report as spam.",
                timestamp = now - (1 * day),
                isResolved = false
            ),
            FraudAlertEntity(
                senderAddress = "HP-KYCUP",
                smsBodySnippet = "URGENT: Your SBI account is blocked today due to pending PAN KYC. Update immediately at bit.ly/sbi-kyc-verify to avoid freeze.",
                riskLevel = "HIGH_RISK",
                triggerReason = "Lookalike phishing shortener + KYC block panic tactic",
                whySuspicious = "Banks do not send bit.ly short links to update KYC details. This page captures your NetBanking password and OTP.",
                recommendedAction = "Never enter your login credentials or OTP on shortened web links.",
                timestamp = now - (3 * day),
                isResolved = false
            )
        )
    }

    fun generateSampleBudgets(): List<BudgetEntity> {
        return listOf(
            BudgetEntity(category = "Food & Dining", monthlyLimitPaise = 1500000L, alertThresholdPercent = 80),
            BudgetEntity(category = "Shopping", monthlyLimitPaise = 1000000L, alertThresholdPercent = 80),
            BudgetEntity(category = "Entertainment", monthlyLimitPaise = 300000L, alertThresholdPercent = 85),
            BudgetEntity(category = "Bills & Utilities", monthlyLimitPaise = 600000L, alertThresholdPercent = 90),
            BudgetEntity(category = "Investments", monthlyLimitPaise = 2500000L, alertThresholdPercent = 95),
            BudgetEntity(category = "Travel & Commute", monthlyLimitPaise = 500000L, alertThresholdPercent = 80)
        )
    }
}
