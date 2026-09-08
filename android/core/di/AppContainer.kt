package com.nivya.core.di

import android.content.Context
import com.nivya.BuildConfig
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.network.AuthInterceptor
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NivyaApiService
import com.nivya.core.security.SecureTokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import com.nivya.data.repository.AuthRepository
import com.nivya.data.repository.BatteryRepository
import com.nivya.data.repository.PairingRepository
import com.nivya.data.repository.RoleRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dependency Injection container providing app-wide singletons.
 */
interface AppContainer {
    val tokenStorage: SecureTokenStorage
    val preferencesDataStore: UserPreferencesDataStore
    val database: NivyaDatabase
    val networkMonitor: NetworkMonitor
    val apiService: NivyaApiService
    val authRepository: AuthRepository
    val roleRepository: RoleRepository
    val pairingRepository: PairingRepository
    val batteryRepository: BatteryRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val tokenStorage: SecureTokenStorage by lazy {
        SecureTokenStorage(context)
    }

    override val preferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val database: NivyaDatabase by lazy {
        NivyaDatabase.getInstance(context)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(tokenStorage)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        // Enable logging ONLY for debug builds to safeguard production privacy
        if (BuildConfig.ENABLE_LOGGING) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    override val apiService: NivyaApiService by lazy {
        retrofit.create(NivyaApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, tokenStorage, preferencesDataStore, database)
    }

    override val roleRepository: RoleRepository by lazy {
        RoleRepository(apiService, tokenStorage, preferencesDataStore)
    }

    override val pairingRepository: PairingRepository by lazy {
        PairingRepository(apiService, tokenStorage, database, networkMonitor)
    }

    override val batteryRepository: BatteryRepository by lazy {
        BatteryRepository(context, apiService, tokenStorage, database, networkMonitor)
    }
}
