package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.util.CurrencyUtils

object NotificationHelper {

    const val CHANNEL_PAYMENTS = "channel_payments"
    const val CHANNEL_FRAUD = "channel_fraud"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Upcoming Payments & AutoPay Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled subscriptions, EMIs, and recurring payments."
                enableVibration(true)
            }

            val fraudChannel = NotificationChannel(
                CHANNEL_FRAUD,
                "Fraud Shield Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when a suspicious or phishing SMS is detected."
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(paymentChannel)
            manager.createNotificationChannel(fraudChannel)
        }
    }

    fun showPaymentReminder(
        context: Context,
        notificationId: Int,
        merchantName: String,
        amountPaise: Long,
        dueDate: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = CurrencyUtils.formatPaiseToRupees(amountPaise)

        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming Payment: $merchantName")
            .setContentText("Expected deduction of $amountStr on $dueDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("An upcoming payment of $amountStr for $merchantName is scheduled for $dueDate. Ensure sufficient account balance.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun showFraudAlert(
        context: Context,
        notificationId: Int,
        sender: String,
        reason: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRAUD)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fraud Shield Alert: Suspicious SMS")
            .setContentText("Flagged message from $sender: $reason")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("AutoPay Alert flagged a suspicious SMS from $sender. Reason: $reason. Do NOT click unknown links or share your OTP / UPI PIN.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
