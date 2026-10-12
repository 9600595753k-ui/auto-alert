package com.example.sms.parser

import com.example.core.util.CurrencyUtils
import com.example.core.util.DateUtils
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

object BankSmsParser {

    private val BANK_HEADER_PATTERNS = mapOf(
        "HDFC" to listOf("HDFCBK", "HDFC", "HDFCLT"),
        "SBI" to listOf("SBIINB", "SBIPSG", "SBICRD", "SBIUPI", "STATEB"),
        "ICICI" to listOf("ICICIB", "ICICI", "ICICIT"),
        "Axis" to listOf("AXISBK", "AXIS", "AXISIN"),
        "Kotak" to listOf("KOTAKB", "KOTAK", "KMBL"),
        "Paytm" to listOf("PAYTM", "PAYTMB"),
        "PNB" to listOf("PNBSMS", "PUNJAB"),
        "BOB" to listOf("BOBTXN", "BARODA"),
        "IndusInd" to listOf("INDUSB", "INDUS"),
        "Canara" to listOf("CANBNK", "CANARA"),
        "Federal" to listOf("FEDBNK", "FEDRL"),
        "IDFC" to listOf("IDFCFB", "IDFC")
    )

    private val OTP_PATTERNS = listOf(
        "otp", "one time password", "verification code", "secret code", "security code"
    )

    private val PROMO_PATTERNS = listOf(
        "apply now", "pre-approved", "congratulations", "win up to", "offer valid till",
        "flat off", "exclusive offer", "loan up to", "upgrade your card"
    )

    // Regex for amounts: Rs. 1,499.00 or INR 500 or Rs 45.50 or ₹ 250
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?))|(?:([\d,]+(?:\.\d{1,2})?)\s*(?:inr|rs\.?|₹))""",
        Pattern.CASE_INSENSITIVE
    )

    // Masked account pattern: A/c **1234 or a/c ending 1234 or card ending 5678 or XX1234
    private val ACCOUNT_PATTERN = Pattern.compile(
        """(?:(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*(?:with)?)?[:\s]*([xX*]*\d{3,4}))|(?:(?:ending\s*with\s*)(\d{3,4}))""",
        Pattern.CASE_INSENSITIVE
    )

    // Reference pattern: UPI Ref: 123456789 or Ref no: 1234 or txn 12345
    private val REF_PATTERN = Pattern.compile(
        """(?:(?:ref(?:\s*no\.?)?|rrn|txn(?:\s*id)?|upi\s*ref(?:erence)?)\s*[:\-]?\s*([a-zA-Z0-9]{6,20}))""",
        Pattern.CASE_INSENSITIVE
    )

    // Balance pattern: Avl Bal Rs. 12,345.00 or Bal: INR 500
    private val BALANCE_PATTERN = Pattern.compile(
        """(?:(?:avl|avail|available|total|ledger)\s*(?:bal|balance)\s*(?:is)?\s*[:\-]?\s*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?))""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, sender: String? = "", messageTimestamp: Long = System.currentTimeMillis()): ParsedSmsResult {
        val lowerBody = smsBody.lowercase(Locale.ENGLISH)
        val upperSender = (sender ?: "").uppercase(Locale.ENGLISH)

        // 1. Filter out OTPs
        if (OTP_PATTERNS.any { lowerBody.contains(it) } && !lowerBody.contains("debited") && !lowerBody.contains("credited")) {
            return ParsedSmsResult(
                isFinancial = true,
                isTransaction = false,
                reason = "Filtered OTP verification message"
            )
        }

        // 2. Filter out Marketing / Promotional alerts
        if (PROMO_PATTERNS.any { lowerBody.contains(it) } && !lowerBody.contains("debited") && !lowerBody.contains("credited")) {
            return ParsedSmsResult(
                isFinancial = false,
                isTransaction = false,
                reason = "Filtered promotional marketing SMS"
            )
        }

        // 3. Determine if message is financial transaction
        val isDebit = lowerBody.contains("debited") || lowerBody.contains("spent") ||
                lowerBody.contains("paid") || lowerBody.contains("withdrawn") ||
                lowerBody.contains("purchase") || lowerBody.contains("transferred")

        val isCredit = lowerBody.contains("credited") || lowerBody.contains("received") ||
                lowerBody.contains("deposited") || lowerBody.contains("refund") || lowerBody.contains("reversed")

        if (!isDebit && !isCredit) {
            return ParsedSmsResult(
                isFinancial = false,
                isTransaction = false,
                reason = "No transaction indicators found"
            )
        }

        // 4. Status Check: Failed, Reversed, Refunded, or Completed
        var status = "COMPLETED"
        if (lowerBody.contains("failed") || lowerBody.contains("declined") || lowerBody.contains("unsuccessful")) {
            status = "FAILED"
        } else if (lowerBody.contains("refund")) {
            status = "REFUNDED"
        } else if (lowerBody.contains("reversed") || lowerBody.contains("reversal")) {
            status = "REVERSED"
        } else if (lowerBody.contains("pending") || lowerBody.contains("in process")) {
            status = "PENDING"
        }

        // 5. Extract Balance if present, so we don't accidentally treat balance as transaction amount
        var balancePaise: Long? = null
        var balanceMatchedStr: String? = null
        val balMatcher = BALANCE_PATTERN.matcher(smsBody)
        if (balMatcher.find()) {
            val balAmountStr = balMatcher.group(1)
            if (balAmountStr != null) {
                balancePaise = CurrencyUtils.rupeesStringToPaise(balAmountStr)
                balanceMatchedStr = balMatcher.group(0)
            }
        }

        // 6. Extract Transaction Amount
        // If balance was found, remove or skip that section from amount matcher
        val bodyForAmount = if (balanceMatchedStr != null) {
            smsBody.replace(balanceMatchedStr, " [BAL_STRIPPED] ")
        } else {
            smsBody
        }

        var amountPaise = 0L
        val amountMatcher = AMOUNT_PATTERN.matcher(bodyForAmount)
        while (amountMatcher.find()) {
            val matchedAmount = amountMatcher.group(1) ?: amountMatcher.group(2)
            if (matchedAmount != null) {
                val candidatePaise = CurrencyUtils.rupeesStringToPaise(matchedAmount)
                if (candidatePaise > 0L) {
                    amountPaise = candidatePaise
                    break // First valid amount preceding or following debit/credit
                }
            }
        }

        if (amountPaise <= 0L) {
            return ParsedSmsResult(
                isFinancial = true,
                isTransaction = false,
                reason = "Could not extract valid transaction amount"
            )
        }

        // 7. Detect Bank from Sender or Body
        var detectedBank = "Bank"
        for ((bank, patterns) in BANK_HEADER_PATTERNS) {
            if (patterns.any { upperSender.contains(it) || smsBody.contains(it, ignoreCase = true) }) {
                detectedBank = bank
                break
            }
        }

        // 8. Extract Masked Account / Card
        var accountMasked = "XX"
        val acctMatcher = ACCOUNT_PATTERN.matcher(smsBody)
        if (acctMatcher.find()) {
            val num = acctMatcher.group(1) ?: acctMatcher.group(2)
            if (!num.isNullOrBlank()) {
                accountMasked = "XX" + num.takeLast(4)
            }
        }

        // 9. Payment Mode
        val paymentMode = when {
            lowerBody.contains("autopay") || lowerBody.contains("mandate") || lowerBody.contains("standing instruction") -> "AUTO_PAY"
            lowerBody.contains("upi") || lowerBody.contains("vpa") -> "UPI"
            lowerBody.contains("card") || lowerBody.contains("pos") || lowerBody.contains("ecom") -> "CARD"
            lowerBody.contains("atm") || lowerBody.contains("cash wdl") -> "ATM"
            lowerBody.contains("neft") || lowerBody.contains("rtgs") || lowerBody.contains("imps") -> "IMPS/NEFT"
            lowerBody.contains("netbanking") || lowerBody.contains("net banking") -> "NET_BANKING"
            else -> "OTHER"
        }

        // 10. Extract Reference / Txn ID
        var refNumber: String? = null
        val refMatcher = REF_PATTERN.matcher(smsBody)
        if (refMatcher.find()) {
            refNumber = refMatcher.group(1)
        }

        // 11. Extract Merchant / Beneficiary
        val merchant = extractMerchant(smsBody, isDebit)
        val category = categorizeMerchant(merchant, lowerBody)

        // 12. Calculate Confidence Score
        var confidence = 0.65f
        if (detectedBank != "Bank") confidence += 0.15f
        if (accountMasked != "XX") confidence += 0.10f
        if (refNumber != null) confidence += 0.05f
        if (merchant != "Unknown Merchant") confidence += 0.05f
        val finalConfidence = confidence.coerceIn(0.5f, 1.0f)

        // 13. Create Unique Deduplication Fingerprint
        val refPart = refNumber ?: smsBody.take(20).trim()
        val rawKey = "$detectedBank-$accountMasked-$amountPaise-$refPart-${messageTimestamp / 86400000}"
        val fingerprint = md5(rawKey)

        val txType = if (isCredit && status != "FAILED") "CREDIT" else "DEBIT"

        return ParsedSmsResult(
            isFinancial = true,
            isTransaction = true,
            amountPaise = amountPaise,
            type = txType,
            status = status,
            bankName = detectedBank,
            accountMasked = accountMasked,
            merchant = merchant,
            category = category,
            paymentMode = paymentMode,
            referenceNumber = refNumber,
            balanceAfterPaise = balancePaise,
            parsedTimestamp = messageTimestamp,
            confidenceScore = finalConfidence,
            fingerprint = fingerprint,
            reason = "Successfully parsed transaction"
        )
    }

    private fun extractMerchant(body: String, isDebit: Boolean): String {
        // Try common patterns:
        // "at SWIGGY on"
        // "to ZOMATO on"
        // "info: NETFLIX"
        // "VPA swiggy@icici"
        // "towards BESCOM"
        val patterns = listOf(
            Pattern.compile("""(?:at|to|towards|for)\s+([a-zA-Z0-9\s&.\-']{2,25})\s+(?:on|ref|avl|bal|using|via|through)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:vpa|info|vpa:)\s*([a-zA-Z0-9@.\-_]{3,30})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:transferred to|paid to)\s+([a-zA-Z0-9\s&.\-']{2,25})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:from)\s+([a-zA-Z0-9\s&.\-']{2,25})\s+(?:on|ref|avl)""", Pattern.CASE_INSENSITIVE)
        )

        for (p in patterns) {
            val m = p.matcher(body)
            if (m.find()) {
                val candidate = m.group(1)?.trim()
                if (!candidate.isNullOrBlank() && candidate.length > 2 && !candidate.equals("your", true) && !candidate.equals("a/c", true)) {
                    // Clean up trailing punctuation or VPA handles
                    val cleaned = candidate.substringBefore("@").replace(Regex("""[^a-zA-Z0-9\s&]"""), "").trim()
                    if (cleaned.isNotBlank()) return formatMerchantName(cleaned)
                }
            }
        }

        return if (isDebit) "Debit Transaction" else "Credit Transfer"
    }

    private fun formatMerchantName(name: String): String {
        val lower = name.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("swiggy") -> "Swiggy"
            lower.contains("zomato") -> "Zomato"
            lower.contains("netflix") -> "Netflix"
            lower.contains("spotify") -> "Spotify"
            lower.contains("amazon") -> "Amazon"
            lower.contains("flipkart") -> "Flipkart"
            lower.contains("uber") -> "Uber"
            lower.contains("ola") -> "Ola Cabs"
            lower.contains("rapido") -> "Rapido"
            lower.contains("airtel") -> "Airtel"
            lower.contains("jio") -> "Jio"
            lower.contains("bescom") -> "BESCOM Electricity"
            lower.contains("tata power") -> "Tata Power"
            lower.contains("hotstar") || lower.contains("disney") -> "Disney+ Hotstar"
            lower.contains("youtube") -> "YouTube Premium"
            lower.contains("groww") -> "Groww Investment"
            lower.contains("zerodha") -> "Zerodha"
            lower.contains("lic") -> "LIC Insurance"
            lower.contains("bajaj") -> "Bajaj Finserv EMI"
            lower.contains("salary") -> "Company Salary"
            else -> name.split(" ").filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }.take(25)
        }
    }

    fun categorizeMerchant(merchant: String, fullBody: String): String {
        val lower = "$merchant $fullBody".lowercase(Locale.ENGLISH)
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") ||
                    lower.contains("restaurant") || lower.contains("mcdonald") || lower.contains("starbucks") ||
                    lower.contains("kfc") || lower.contains("cafe") || lower.contains("pizza") -> "Food & Dining"

            lower.contains("netflix") || lower.contains("spotify") || lower.contains("hotstar") ||
                    lower.contains("prime") || lower.contains("youtube") || lower.contains("zee5") ||
                    lower.contains("cinema") || lower.contains("movie") || lower.contains("bookmyshow") -> "Entertainment"

            lower.contains("airtel") || lower.contains("jio") || lower.contains("vi ") || lower.contains("broadband") ||
                    lower.contains("electricity") || lower.contains("bescom") || lower.contains("tata power") ||
                    lower.contains("billdesk") || lower.contains("gas") || lower.contains("water bill") ||
                    lower.contains("utility") -> "Bills & Utilities"

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("meesho") || lower.contains("zara") || lower.contains("shopping") ||
                    lower.contains("supermarket") || lower.contains("store") -> "Shopping"

            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("irctc") || lower.contains("makemytrip") || lower.contains("indigo") ||
                    lower.contains("metro") || lower.contains("fuel") || lower.contains("petrol") -> "Travel & Commute"

            lower.contains("groww") || lower.contains("zerodha") || lower.contains("mutual fund") ||
                    lower.contains("sip") || lower.contains("kuvera") || lower.contains("upstox") ||
                    lower.contains("invest") || lower.contains("nps") -> "Investments"

            lower.contains("apollo") || lower.contains("pharmeasy") || lower.contains("1mg") ||
                    lower.contains("pharmacy") || lower.contains("hospital") || lower.contains("doctor") ||
                    lower.contains("med") || lower.contains("clinic") -> "Health & Medical"

            lower.contains("salary") || lower.contains("payroll") || lower.contains("stipend") ||
                    lower.contains("incentive") || lower.contains("bonus") -> "Salary & Income"

            lower.contains("emi") || lower.contains("loan") || lower.contains("bajaj finserv") ||
                    lower.contains("interest") || lower.contains("finance") -> "Loans & EMI"

            else -> "Miscellaneous"
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digested = md.digest(input.toByteArray())
        return digested.joinToString("") { "%02x".format(it) }
    }
}
