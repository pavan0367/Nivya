package com.nivya.services.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nivya.NivyaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging Service for the Nivya application.
 *
 * Responsibilities:
 * - Listens for token refreshes and securely synchronizes with the backend.
 * - Handles incoming push messages:
 *   - Convocation messages: strictly posts decoy notification "Check your battery status".
 *     Never reveals secret message payload. Tapping opens Nivya normally.
 *   - Safety, hardware, battery, and security alerts: dispatches via AlertNotificationManager.
 */
class NivyaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token received: ${token.take(8)}...")

        serviceScope.launch {
            try {
                val app = applicationContext as? NivyaApp
                val container = app?.container
                if (container != null) {
                    // Save token locally
                    container.preferencesDataStore.saveFcmToken(token)

                    // If user is authenticated, sync with backend immediately
                    if (container.authRepository.isLoggedIn()) {
                        Log.i(TAG, "User authenticated; synchronizing push token with backend...")
                        container.authRepository.syncPushToken(token)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist/synchronize new FCM token: ${e.message}", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val messageType = data["type"] ?: remoteMessage.notification?.tag ?: "GENERAL"

        when (messageType) {
            "CONVOCATION" -> {
                // STRICT REQUIREMENT: Convocation notification on Child must display EXACT text:
                // "Check your battery status", never reveal actual message payload,
                // and tapping it must open Nivya normally (MainActivity).
                Log.i(TAG, "Received Convocation push. Dispatching decoy notification.")
                ConvocationNotificationManager.postDecoyNotification(applicationContext)
            }

            "ALERT", "LOW_BATTERY", "OFFLINE", "RECONNECTED" -> {
                val title = data["title"]
                    ?: remoteMessage.notification?.title
                    ?: "Alert"
                val message = data["message"]
                    ?: data["body"]
                    ?: remoteMessage.notification?.body
                    ?: "Device status update received"
                val severity = data["severity"] ?: "WARNING"
                val alertType = data["alertType"] ?: messageType

                val notificationId = (alertType + title).hashCode()
                AlertNotificationManager.postNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    message = message,
                    severity = severity
                )
            }

            else -> {
                // Generic safety/status notification fallback
                val title = data["title"] ?: remoteMessage.notification?.title ?: "Nivya Alert"
                val message = data["message"] ?: data["body"] ?: remoteMessage.notification?.body ?: "New notification"
                AlertNotificationManager.postNotification(
                    context = applicationContext,
                    notificationId = 8800,
                    title = title,
                    message = message,
                    severity = "INFO"
                )
            }
        }
    }

    companion object {
        private const val TAG = "NivyaFCMService"
    }
}
