package com.example

import com.example.fraud.FraudDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FraudDetectorTest {

    @Test
    fun `test dangerous remote access app flagged as HIGH_RISK`() {
        val scamSms = "Your SBI account is suspended. Install AnyDesk immediately and provide access to executive to unfreeze."
        val result = FraudDetector.analyze(scamSms, "+919876543210")

        assertTrue(result.isSuspicious)
        assertEquals("HIGH_RISK", result.riskLevel)
        assertTrue(result.triggerReason.contains("remote screen-sharing", ignoreCase = true))
    }

    @Test
    fun `test malicious APK download link flagged as HIGH_RISK`() {
        val apkSms = "Important KYC update required. Please download official update from http://sbi-secure.apk"
        val result = FraudDetector.analyze(apkSms, "+919876543210")

        assertTrue(result.isSuspicious)
        assertEquals("HIGH_RISK", result.riskLevel)
        assertTrue(result.triggerReason.contains("APK", ignoreCase = true))
    }

    @Test
    fun `test fake UPI PIN request to receive money flagged as HIGH_RISK`() {
        val pinSms = "Congratulations! You have received ₹5,000 reward. Share your PIN or enter OTP to verify credit."
        val result = FraudDetector.analyze(pinSms, "+919876543210")

        assertTrue(result.isSuspicious)
        assertEquals("HIGH_RISK", result.riskLevel)
    }

    @Test
    fun `test normal bank transaction not flagged as fraud`() {
        val legitimateSms = "Your A/c **4589 has been debited by Rs. 450.00 at Swiggy on 12-OCT-26. Avl Bal: Rs. 14,200."
        val result = FraudDetector.analyze(legitimateSms, "HDFCBK")

        assertFalse("Legitimate bank transaction should not be suspicious", result.isSuspicious)
        assertEquals("INFORMATIONAL", result.riskLevel)
    }
}
