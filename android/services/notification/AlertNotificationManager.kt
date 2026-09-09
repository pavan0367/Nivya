package com.nivya.services.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nivya.MainActivity

/**
 * Android Notification Architecture manager:
 * Sets up priority notification channels, handles Android 13+ permissions safely,
 * and builds local notifications for safety, security, and hardware alerts.
 */
object AlertNotificationManager {

    const val CHANNEL_SAFETY_CRITICAL = "nivya_safety_critical"
    const val CHANNEL_GENERAL_ALERTS = "nivya_general_alerts"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Critical Safety & Security Channel
            val criticalChannel = NotificationChannel(
                CHANNEL_SAFETY_CRITICAL,
                "Safety & Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority safety notifications including low battery, offline status, and security alerts"
                enableVibration(true)
                enableLights(true)
            }

            // 2. General & Device Status Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ALERTS,
                "Device & Status Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General device status, connection recovery, and usage check-in notifications"
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        severity: String = "WARNING"
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val isCritical = "CRITICAL".equals(severity, ignoreCase = true)
        val channelId = if (isCritical) CHANNEL_SAFETY_CRITICAL else CHANNEL_GENERAL_ALERTS

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Safely ignored if permission was revoked concurrently
        }
    }
}
