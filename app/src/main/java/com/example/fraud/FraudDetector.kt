package com.example.fraud

import java.util.Locale
import java.util.regex.Pattern

data class FraudAnalysisResult(
    val isSuspicious: Boolean,
    val riskLevel: String, // "INFORMATIONAL", "CAUTION", "HIGH_RISK"
    val triggerReason: String,
    val whySuspicious: String,
    val recommendedAction: String
)

object FraudDetector {

    // Remote access application keywords
    private val REMOTE_ACCESS_APPS = listOf(
        "anydesk", "teamviewer", "rustdesk", "quicksupport", "screen share", "airdroid"
    )

    // Urgent coercive threats
    private val URGENT_THREATS = listOf(
        "blocked immediately", "suspended within", "deactivated within", "kyc pending",
        "update pan immediately", "sim blocked", "electricity will be disconnected",
        "power cut at 9:30", "account will be frozen", "urgent action required",
        "last reminder to update", "eb bill pending"
    )

    // Suspicious link patterns (URL shorteners, raw IP, apk downloads, untrusted TLDs)
    private val LINK_PATTERN = Pattern.compile(
        """(https?://[^\s]+|bit\.ly/[^\s]+|tinyurl\.com/[^\s]+|t\.co/[^\s]+|is\.gd/[^\s]+|goo\.gl/[^\s]+|[a-zA-Z0-9-]+\.(?:xyz|top|info|live|club|download|vip|site|online)/[^\s]*)""",
        Pattern.CASE_INSENSITIVE
    )

    // Phishing credential requests
    private val CREDENTIAL_REQUESTS = listOf(
        "enter otp to verify", "share your pin", "send 4 digit code",
        "verify password", "upi pin required to receive", "click link to receive money"
    )

    fun analyze(smsBody: String, sender: String = ""): FraudAnalysisResult {
        val lowerBody = smsBody.lowercase(Locale.ENGLISH)
        val upperSender = sender.uppercase(Locale.ENGLISH)

        // 1. High Risk: Remote access tools requested
        for (app in REMOTE_ACCESS_APPS) {
            if (lowerBody.contains(app)) {
                return FraudAnalysisResult(
                    isSuspicious = true,
                    riskLevel = "HIGH_RISK",
                    triggerReason = "Dangerous remote screen-sharing app mentioned ('$app')",
                    whySuspicious = "Legitimate banks and utility boards NEVER ask you to install AnyDesk, TeamViewer, or remote access tools. Scammers use these to take control of your phone and siphon bank accounts.",
                    recommendedAction = "DO NOT install any app or allow screen sharing. Disconnect immediately and delete this message."
                )
            }
        }

        // 2. High Risk: Phishing APK or Untrusted Download Link
        if (lowerBody.contains(".apk") || lowerBody.contains("install our update")) {
            return FraudAnalysisResult(
                isSuspicious = true,
                riskLevel = "HIGH_RISK",
                triggerReason = "Suspicious Android APK package download detected",
                whySuspicious = "Banks only distribute apps through Google Play Store, never via direct SMS links or APK file downloads.",
                recommendedAction = "NEVER click APK links or grant install permission. Block the sender immediately."
            )
        }

        // 3. High Risk: Fake PIN to receive money / Credential harvesting
        for (cred in CREDENTIAL_REQUESTS) {
            if (lowerBody.contains(cred)) {
                return FraudAnalysisResult(
                    isSuspicious = true,
                    riskLevel = "HIGH_RISK",
                    triggerReason = "Request for PIN, OTP, or password to receive funds",
                    whySuspicious = "In UPI, you NEVER need to enter your UPI PIN or share OTP to receive money. Entering a PIN always debits money from your account.",
                    recommendedAction = "DO NOT enter your PIN or click any accept button. Report this fraudulent request."
                )
            }
        }

        // 4. Caution: Urgent KYC / Power cut with link
        val linkMatcher = LINK_PATTERN.matcher(smsBody)
        val hasSuspiciousLink = linkMatcher.find()
        val hasUrgentThreat = URGENT_THREATS.any { lowerBody.contains(it) }

        if (hasUrgentThreat && hasSuspiciousLink) {
            return FraudAnalysisResult(
                isSuspicious = true,
                riskLevel = "HIGH_RISK",
                triggerReason = "Urgent coercive threat paired with an unverified external link",
                whySuspicious = "Scammers create artificial panic (e.g. KYC expiry, electricity power cut, bank account freeze) to rush victims into clicking malicious credential-stealing links.",
                recommendedAction = "Do NOT click the link. Visit your official bank branch or call the registered customer care number found on the back of your debit card."
            )
        }

        // 5. Caution: Urgent threat without link (e.g. call a private mobile number)
        if (hasUrgentThreat) {
            return FraudAnalysisResult(
                isSuspicious = true,
                riskLevel = "CAUTION",
                triggerReason = "Urgent coercive threat detected in message text",
                whySuspicious = "Banks do not suddenly deactivate accounts or disconnect services via generic SMS without prior formal communication.",
                recommendedAction = "Verify status only through official banking portals or the verified banking app."
            )
        }

        // 6. Caution: Unknown sender with external link
        if (hasSuspiciousLink && !upperSender.endsWith("BK") && !upperSender.endsWith("INB")) {
            return FraudAnalysisResult(
                isSuspicious = true,
                riskLevel = "CAUTION",
                triggerReason = "Shortened or non-standard web link from unverified sender",
                whySuspicious = "Shortened URLs (bit.ly, tinyurl) mask the true destination and are commonly used in phishing campaigns.",
                recommendedAction = "Do not open links from unverified numbers or unfamiliar headers."
            )
        }

        // 7. Informational / Clean
        return FraudAnalysisResult(
            isSuspicious = false,
            riskLevel = "INFORMATIONAL",
            triggerReason = "No common phishing, remote-access, or coercive patterns found",
            whySuspicious = "Standard notification pattern without immediate threat vectors.",
            recommendedAction = "Always maintain caution and never share OTP or banking passwords."
        )
    }
}
