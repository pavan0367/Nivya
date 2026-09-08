package com.nivya

import android.app.Application
import androidx.work.Configuration
import com.nivya.core.di.AppContainer
import com.nivya.core.di.DefaultAppContainer
import com.nivya.services.sync.BatterySyncWorker

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
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG else android.util.Log.ERROR
            )
            .build()
}
