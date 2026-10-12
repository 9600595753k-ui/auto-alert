package com.example.sms.parser

data class ParsedSmsResult(
    val isFinancial: Boolean,
    val isTransaction: Boolean, // false if OTP, balance-only, promotion, or marketing
    val amountPaise: Long = 0L,
    val type: String = "DEBIT", // "DEBIT", "CREDIT"
    val status: String = "COMPLETED", // "COMPLETED", "FAILED", "REVERSED", "REFUNDED", "PENDING"
    val bankName: String = "Bank",
    val accountMasked: String = "Account",
    val merchant: String = "Unknown Merchant",
    val category: String = "Miscellaneous",
    val paymentMode: String = "UPI", // "UPI", "CARD", "NET_BANKING", "ATM", "AUTO_PAY", "IMPS/NEFT"
    val referenceNumber: String? = null,
    val balanceAfterPaise: Long? = null,
    val parsedTimestamp: Long? = null,
    val confidenceScore: Float = 0.0f,
    val fingerprint: String = "",
    val reason: String = "" // Diagnostic message or why classified as such
)
