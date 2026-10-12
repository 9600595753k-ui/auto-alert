package com.example

import com.example.sms.parser.BankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun `test debit SMS parsing with available balance distinction`() {
        val sms = "Dear HDFC Bank User, A/c **4589 has been debited by Rs. 649.00 on 12-OCT-26 towards NETFLIX. Avl Bal: Rs. 45,210.50. Ref: UPI923841029."
        val result = BankSmsParser.parse(sms, "AD-HDFCBK")

        assertTrue(result.isFinancial)
        assertTrue(result.isTransaction)
        assertEquals("DEBIT", result.type)
        assertEquals(64900L, result.amountPaise) // ₹649.00
        assertEquals(4521050L, result.balanceAfterPaise) // ₹45,210.50
        assertEquals("HDFC", result.bankName)
        assertEquals("XX4589", result.accountMasked)
        assertEquals("Netflix", result.merchant)
        assertEquals("Entertainment", result.category)
        assertEquals("COMPLETED", result.status)
        assertEquals("UPI923841029", result.referenceNumber)
    }

    @Test
    fun `test salary credit SMS parsing`() {
        val sms = "Your ICICI Bank A/c ending 3312 is credited with INR 82,500.00 on 01-OCT-26 by COMPANY SALARY PAYROLL. Total Avail Bal INR 98,200.00. Ref: NEFT482910."
        val result = BankSmsParser.parse(sms, "VK-ICICIB")

        assertTrue(result.isTransaction)
        assertEquals("CREDIT", result.type)
        assertEquals(8250000L, result.amountPaise) // ₹82,500.00
        assertEquals("ICICI", result.bankName)
        assertEquals("XX3312", result.accountMasked)
        assertEquals("Salary & Income", result.category)
    }

    @Test
    fun `test refund and reversal detection`() {
        val sms = "Refund of Rs 350.00 processed for your transaction at SWIGGY to A/c ending 4589. Ref: REF98201948."
        val result = BankSmsParser.parse(sms, "HDFCBK")

        assertTrue(result.isTransaction)
        assertEquals("REFUNDED", result.status)
        assertEquals("CREDIT", result.type)
        assertEquals(35000L, result.amountPaise)
    }

    @Test
    fun `test OTP and promotional filtering out`() {
        val otpSms = "Your OTP for login to netbanking is 482910. Do not share this OTP with anyone."
        val otpResult = BankSmsParser.parse(otpSms, "HDFCBK")
        assertFalse("OTP message should not be treated as a transaction", otpResult.isTransaction)

        val promoSms = "Congratulations! You have pre-approved personal loan up to Rs. 5,00,000. Apply now."
        val promoResult = BankSmsParser.parse(promoSms, "SBILON")
        assertFalse("Promotional message should not be treated as a transaction", promoResult.isTransaction)
    }

    @Test
    fun `test deduplication fingerprint stability`() {
        val sms1 = "A/c **4589 debited by Rs. 200.00 to SWIGGY. Ref: 12345."
        val timestamp = 1770000000000L
        val res1 = BankSmsParser.parse(sms1, "HDFC", timestamp)
        val res2 = BankSmsParser.parse(sms1, "HDFC", timestamp)

        assertEquals("Same message must generate identical fingerprint", res1.fingerprint, res2.fingerprint)

        val smsDifferentAmount = "A/c **4589 debited by Rs. 350.00 to SWIGGY. Ref: 12345."
        val res3 = BankSmsParser.parse(smsDifferentAmount, "HDFC", timestamp)
        assertNotEquals("Different amount must produce different fingerprint", res1.fingerprint, res3.fingerprint)
    }
}
