package com.nivya

import android.app.Application
import androidx.work.Configuration
import com.nivya.core.di.AppContainer
import com.nivya.core.di.DefaultAppContainer
import com.nivya.services.sync.BatterySyncWorker
import com.nivya.services.sync.DeviceHealthSyncWorker
import com.nivya.services.sync.LocationSyncWorker
import com.nivya.services.sync.NetworkSyncWorker
import com.nivya.services.sync.UsageSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Root Application class initializing the DI container and WorkManager configuration.
 */
class NivyaApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        BatterySyncWorker.schedulePeriodic(this)
        NetworkSyncWorker.schedulePeriodic(this)
        UsageSyncWorker.schedulePeriodic(this)
        LocationSyncWorker.schedulePeriodic(this)
        DeviceHealthSyncWorker.schedulePeriodic(this)
        com.nivya.services.notification.AlertNotificationManager.initChannels(this)

        // Query current cached FCM token from Firebase if available
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrBlank()) {
                            android.util.Log.i("NivyaApp", "FCM token retrieved: ${token.take(8)}...")
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    container.preferencesDataStore.saveFcmToken(token)
                                    if (container.authRepository.isLoggedIn()) {
                                        android.util.Log.i("NivyaApp", "Active session found; synchronizing stored push token...")
                                        val result = container.authRepository.syncStoredPushToken(force = false)
                                        android.util.Log.i("NivyaApp", "Push token sync result: $result")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("NivyaApp", "Push token sync error: ${e.message}")
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("NivyaApp", "Fetching FCM registration token failed: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("NivyaApp", "FirebaseMessaging token exception: ${e.message}")
        }
    }



    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG else android.util.Log.ERROR
            )
            .build()
}
