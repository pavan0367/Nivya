package com.nivya.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.UsageEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.AppUsageItemDto
import com.nivya.core.network.dto.AppUsageResponseDto
import com.nivya.core.network.dto.UsageSummaryResponseDto
import com.nivya.core.network.dto.UsageTelemetryRequestDto
import com.nivya.core.network.dto.UsageTrendResponseDto
import com.nivya.core.security.SecureTokenStorage
import com.nivya.services.usage.AppUsageRecord
import com.nivya.services.usage.UsageCollectionResult
import com.nivya.services.usage.UsagePermissionHelper
import com.nivya.services.usage.UsageStatsCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository coordinating screen time telemetry, offline queueing via Room,
 * and backend synchronisation.
 */
class UsageRepository(
    private val context: Context,
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson = Gson()
) {

    private val usageDao = database.usageDao()

    fun getLatestUsageFlow(): Flow<UsageEntity?> = usageDao.getLatestUsageFlow()

    fun getUsageByDateFlow(date: String): Flow<UsageEntity?> = usageDao.getUsageByDateFlow(date)

    suspend fun collectAndRecordUsage(): UsageCollectionResult = withContext(Dispatchers.IO) {
        if (!UsagePermissionHelper.isUsagePermissionGranted(context)) {
            return@withContext UsageCollectionResult(0L, 0L, 0L, 0L, 0L, emptyList())
        }

        val result = UsageStatsCollector.collectDailyUsage(context)
        val deviceUuid = tokenStorage.getDeviceUuid()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val topAppsJson = gson.toJson(result.apps.take(20))

        val entity = UsageEntity(
            deviceUuid = deviceUuid,
            date = todayStr,
            totalForegroundSeconds = result.totalForegroundSeconds,
            educationalSeconds = result.educationalSeconds,
            recreationalSeconds = result.recreationalSeconds,
            socialSeconds = result.socialSeconds,
            productivitySeconds = result.productivitySeconds,
            topAppsJson = topAppsJson,
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        val insertedId = usageDao.insertUsageReading(entity)

        if (networkMonitor.isOnline.value && tokenStorage.hasAccessToken()) {
            try {
                val appDtos = result.apps.take(50).map { app ->
                    AppUsageItemDto(
                        packageName = app.packageName,
                        appName = app.appName,
                        category = app.category,
                        foregroundSeconds = app.foregroundSeconds,
                        lastTimeUsed = null
                    )
                }
                val request = UsageTelemetryRequestDto(
                    deviceUuid = deviceUuid,
                    date = todayStr,
                    totalForegroundSeconds = result.totalForegroundSeconds,
                    screenUnlocks = 0,
                    apps = appDtos
                )
                val response = apiService.sendUsageTelemetry(request)
                if (response.isSuccessful) {
                    usageDao.markAsSynced(listOf(insertedId))
                    flushOfflineQueue()
                }
            } catch (_: Exception) {
                // Keep isSynced = false for retry
            }
        }

        result
    }

    suspend fun flushOfflineQueue(): Int = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value || !tokenStorage.hasAccessToken()) {
            return@withContext 0
        }

        val unsynced = usageDao.getUnsyncedReadings()
        if (unsynced.isEmpty()) return@withContext 0

        val syncedIds = mutableListOf<Long>()
        val appListType = object : TypeToken<List<AppUsageRecord>>() {}.type

        for (item in unsynced) {
            try {
                val apps: List<AppUsageRecord> = try {
                    gson.fromJson(item.topAppsJson, appListType) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val appDtos = apps.map { app ->
                    AppUsageItemDto(
                        packageName = app.packageName,
                        appName = app.appName,
                        category = app.category,
                        foregroundSeconds = app.foregroundSeconds,
                        lastTimeUsed = null
                    )
                }

                val request = UsageTelemetryRequestDto(
                    deviceUuid = item.deviceUuid,
                    date = item.date,
                    totalForegroundSeconds = item.totalForegroundSeconds,
                    screenUnlocks = 0,
                    apps = appDtos
                )
                val response = apiService.sendUsageTelemetry(request)
                if (response.isSuccessful) {
                    syncedIds.add(item.id)
                }
            } catch (_: Exception) {
                break
            }
        }

        if (syncedIds.isNotEmpty()) {
            usageDao.markAsSynced(syncedIds)
        }

        syncedIds.size
    }

    suspend fun getDailySummary(deviceId: Long, date: String? = null): NetworkResult<UsageSummaryResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDailyUsageSummary(deviceId, date)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch usage summary")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getAppUsage(deviceId: Long, date: String? = null): NetworkResult<AppUsageResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAppUsage(deviceId, date)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch app usage")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getUsageTrends(deviceId: Long): NetworkResult<UsageTrendResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsageTrends(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch usage trends")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
