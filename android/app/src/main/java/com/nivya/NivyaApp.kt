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
    }



    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG else android.util.Log.ERROR
            )
            .build()
}
