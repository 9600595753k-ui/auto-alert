package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CurrencyUtils
import com.example.fraud.FraudAnalysisResult
import com.example.fraud.FraudDetector
import com.example.sms.parser.BankSmsParser
import com.example.sms.parser.ParsedSmsResult
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavySurfaceHigh

@Composable
fun SmsSandboxScreen(
    onCommitParsedTransaction: (ParsedSmsResult, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var smsText by remember {
        mutableStateOf(
            "Dear HDFC Bank User, A/c **4589 has been debited by Rs. 649.00 on 12-OCT-26 towards NETFLIX via UPI AutoPay. Avl Bal: Rs. 45,210.50. Ref: UPI923841029."
        )
    }
    var senderText by remember { mutableStateOf("AD-HDFCBK") }
    var parsedResult by remember { mutableStateOf<ParsedSmsResult?>(null) }
    var fraudResult by remember { mutableStateOf<FraudAnalysisResult?>(null) }
    var ingestSuccessMessage by remember { mutableStateOf(false) }

    val sampleTemplates = listOf(
        "HDFC Netflix AutoPay" to Pair(
            "AD-HDFCBK",
            "Dear HDFC Bank User, A/c **4589 has been debited by Rs. 649.00 on 12-OCT-26 towards NETFLIX via UPI AutoPay. Avl Bal: Rs. 45,210.50. Ref: UPI923841029."
        ),
        "SBI Swiggy UPI" to Pair(
            "VM-SBIINB",
            "Dear SBI User, your A/c ending 9102 is debited by Rs 340.00 on 12/10/2026 transferred to SWIGGY. Avl Bal Rs: 23,840.10. UPI Ref no 4829103912."
        ),
        "ICICI Salary Credit" to Pair(
            "VK-ICICIB",
            "Your ICICI Bank A/c ending 3312 is credited with INR 82,500.00 on 01-OCT-26 by COMPANY SALARY PAYROLL. Total Avail Bal INR 98,200.00. Ref: NEFT482910."
        ),
        "Electricity Bill Phishing Scam" to Pair(
            "+919876543210",
            "Dear consumer, your electricity will be disconnected tonight at 9:30 PM due to pending bill. Call officer immediately or update bill at bit.ly/power-eb-bill"
        ),
        "AnyDesk Remote Access Fraud" to Pair(
            "+919123456789",
            "Your bank account is frozen due to KYC non-compliance. To unlock, install AnyDesk app and share 9 digit code with customer executive immediately."
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("sms_sandbox_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SMS Parser & Security Sandbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Select a Preset Bank or Scam Template:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Template Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sampleTemplates.forEach { (label, pair) ->
                FilterChip(
                    selected = smsText == pair.second,
                    onClick = {
                        senderText = pair.first
                        smsText = pair.second
                        parsedResult = null
                        fraudResult = null
                        ingestSuccessMessage = false
                    },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sender input
        OutlinedTextField(
            value = senderText,
            onValueChange = { senderText = it },
            label = { Text("Sender Header / Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Body input
        OutlinedTextField(
            value = smsText,
            onValueChange = {
                smsText = it
                ingestSuccessMessage = false
            },
            label = { Text("SMS Message Body") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Analyze Button
        Button(
            onClick = {
                parsedResult = BankSmsParser.parse(smsText, senderText)
                fraudResult = FraudDetector.analyze(smsText, senderText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("run_sms_analysis_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Run analysis")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze Message", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Section
        if (parsedResult != null || fraudResult != null) {
            // Fraud Result Card
            fraudResult?.let { fraud ->
                val cardColor = when (fraud.riskLevel) {
                    "HIGH_RISK" -> CoralRed.copy(alpha = 0.15f)
                    "CAUTION" -> AmberWarning.copy(alpha = 0.15f)
                    else -> EmeraldGreen.copy(alpha = 0.15f)
                }
                val textColor = when (fraud.riskLevel) {
                    "HIGH_RISK" -> CoralRed
                    "CAUTION" -> AmberWarning
                    else -> EmeraldGreen
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = "Fraud Risk", tint = textColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fraud Shield Risk: ${fraud.riskLevel.replace("_", " ")}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = fraud.triggerReason,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fraud.whySuspicious,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Safe Action: ${fraud.recommendedAction}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transaction Parse Result Card
            parsedResult?.let { result ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurfaceHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (result.isTransaction) "Valid Transaction Detected" else "Not a Completed Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isTransaction) EmeraldGreen else AmberWarning
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (result.isTransaction) {
                            val rows = listOf(
                                "Amount" to CurrencyUtils.formatPaiseToRupees(result.amountPaise),
                                "Type" to result.type,
                                "Bank" to result.bankName,
                                "Account" to result.accountMasked,
                                "Merchant" to result.merchant,
                                "Category" to result.category,
                                "Payment Mode" to result.paymentMode,
                                "Reference" to (result.referenceNumber ?: "Not found"),
                                "Balance After" to (result.balanceAfterPaise?.let { CurrencyUtils.formatPaiseToRupees(it) } ?: "Not extracted"),
                                "Confidence" to "${(result.confidenceScore * 100).toInt()}%"
                            )

                            rows.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (ingestSuccessMessage) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.2f))
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Saved", tint = EmeraldGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transaction ingested to Smart Passbook!", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onCommitParsedTransaction(result, smsText, senderText)
                                        ingestSuccessMessage = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black)
                                ) {
                                    Text("Commit & Ingest to Passbook", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Reason: ${result.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
