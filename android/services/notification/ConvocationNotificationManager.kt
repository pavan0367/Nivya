package com.nivya.services.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nivya.MainActivity

/**
 * Dedicated Notification Manager for Convocation signals.
 * Strictly posts decoy notification: "Check your battery status".
 * Never reveals message contents.
 * Tapping opens Nivya normally without directly navigating to Convocation.
 */
object ConvocationNotificationManager {

    const val CHANNEL_ID = "nivya_device_status_sync"
    const val DECOY_TITLE = "Device Update"
    const val DECOY_MESSAGE = "Check your battery status"
    const val NOTIFICATION_ID = 8801

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Status Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic device status and maintenance reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts the decoy notification on Child device.
     */
    fun postDecoyNotification(context: Context) {
        initChannel(context)

        // Intent opens Nivya main screen normally (MainActivity without deep link flags)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(DECOY_TITLE)
            .setContentText(DECOY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Ignored if notification permission denied
        }
    }
}
