package com.nivya.data.repository

import android.content.Context
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.DeviceHealthEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.DeviceHealthResponseDto
import com.nivya.core.network.dto.DeviceHealthTelemetryRequestDto
import com.nivya.core.security.SecureTokenStorage
import com.nivya.services.health.DeviceHealthCollector
import com.nivya.services.health.DeviceHealthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository orchestrating legitimate Android device health & permission diagnostics collection,
 * offline Room caching, and remote backend synchronization.
 */
class DeviceHealthRepository(
    private val context: Context,
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor
) {

    private val deviceHealthDao = database.deviceHealthDao()

    fun observeLatestHealth(): Flow<DeviceHealthEntity?> = deviceHealthDao.observeLatest()

    suspend fun getLatestCached(): DeviceHealthEntity? = withContext(Dispatchers.IO) {
        deviceHealthDao.getLatest()
    }

    suspend fun collectAndRecordHealth(): DeviceHealthSnapshot = withContext(Dispatchers.IO) {
        val snapshot = DeviceHealthCollector.collect(context)
        val deviceUuid = tokenStorage.getDeviceUuid()

        val entity = DeviceHealthEntity(
            deviceUuid = deviceUuid,
            deviceModel = snapshot.deviceModel,
            deviceManufacturer = snapshot.deviceManufacturer,
            osVersion = snapshot.osVersion,
            sdkVersion = snapshot.sdkVersion,
            storageTotalBytes = snapshot.storageTotalBytes,
            storageUsedBytes = snapshot.storageUsedBytes,
            storageFreeBytes = snapshot.storageFreeBytes,
            ramTotalBytes = snapshot.ramTotalBytes,
            ramUsedBytes = snapshot.ramUsedBytes,
            ramFreeBytes = snapshot.ramFreeBytes,
            isLowRam = snapshot.isLowRam,
            batteryPct = snapshot.batteryPct,
            chargingState = snapshot.chargingState,
            batteryHealth = snapshot.batteryHealth,
            batteryTempCelsius = snapshot.batteryTempCelsius,
            networkType = snapshot.networkType,
            isOnline = snapshot.isOnline,
            locationPermission = snapshot.locationPermission,
            usagePermission = snapshot.usagePermission,
            notificationPermission = snapshot.notificationPermission,
            batteryOptimization = snapshot.batteryOptimization,
            allPermissionsHealthy = snapshot.allPermissionsHealthy,
            recordedAt = snapshot.recordedAt,
            isSynced = false
        )

        val insertedId = deviceHealthDao.insert(entity)

        if (networkMonitor.isOnline.value) {
            try {
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(snapshot.recordedAt))

                val req = DeviceHealthTelemetryRequestDto(
                    deviceUuid = deviceUuid,
                    deviceModel = snapshot.deviceModel,
                    deviceManufacturer = snapshot.deviceManufacturer,
                    osVersion = snapshot.osVersion,
                    sdkVersion = snapshot.sdkVersion,
                    batteryPct = snapshot.batteryPct,
                    chargingState = snapshot.chargingState,
                    batteryHealth = snapshot.batteryHealth,
                    batteryTempCelsius = snapshot.batteryTempCelsius,
                    storageTotalBytes = snapshot.storageTotalBytes,
                    storageUsedBytes = snapshot.storageUsedBytes,
                    storageFreeBytes = snapshot.storageFreeBytes,
                    ramTotalBytes = snapshot.ramTotalBytes,
                    ramUsedBytes = snapshot.ramUsedBytes,
                    ramFreeBytes = snapshot.ramFreeBytes,
                    isLowRam = snapshot.isLowRam,
                    networkType = snapshot.networkType,
                    isOnline = snapshot.isOnline,
                    locationPermission = snapshot.locationPermission,
                    usagePermission = snapshot.usagePermission,
                    notificationPermission = snapshot.notificationPermission,
                    batteryOptimization = snapshot.batteryOptimization,
                    syncState = "SYNCED",
                    recordedAt = isoDate
                )

                val res = apiService.sendDeviceHealthTelemetry(req)
                if (res.isSuccessful && res.body()?.success == true) {
                    deviceHealthDao.markSynced(insertedId)
                }
            } catch (_: Exception) {
                // Keep marked as isSynced=false for background worker retry
            }
        }

        snapshot
    }

    suspend fun syncPendingHealth(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) {
            return@withContext NetworkResult.Error(0, "Device is offline. Queued health telemetry retained.")
        }

        val unsynced = deviceHealthDao.getUnsynced()
        if (unsynced.isEmpty()) {
            return@withContext NetworkResult.Success(Unit)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var syncedCount = 0
        for (item in unsynced) {
            try {
                val req = DeviceHealthTelemetryRequestDto(
                    deviceUuid = item.deviceUuid,
                    deviceModel = item.deviceModel,
                    deviceManufacturer = item.deviceManufacturer,
                    osVersion = item.osVersion,
                    sdkVersion = item.sdkVersion,
                    batteryPct = item.batteryPct,
                    chargingState = item.chargingState,
                    batteryHealth = item.batteryHealth,
                    batteryTempCelsius = item.batteryTempCelsius,
                    storageTotalBytes = item.storageTotalBytes,
                    storageUsedBytes = item.storageUsedBytes,
                    storageFreeBytes = item.storageFreeBytes,
                    ramTotalBytes = item.ramTotalBytes,
                    ramUsedBytes = item.ramUsedBytes,
                    ramFreeBytes = item.ramFreeBytes,
                    isLowRam = item.isLowRam,
                    networkType = item.networkType,
                    isOnline = item.isOnline,
                    locationPermission = item.locationPermission,
                    usagePermission = item.usagePermission,
                    notificationPermission = item.notificationPermission,
                    batteryOptimization = item.batteryOptimization,
                    syncState = "SYNCED",
                    recordedAt = dateFormat.format(Date(item.recordedAt))
                )

                val response = apiService.sendDeviceHealthTelemetry(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    deviceHealthDao.markSynced(item.id)
                    syncedCount++
                }
            } catch (e: Exception) {
                return@withContext NetworkResult.Exception(e)
            }
        }

        NetworkResult.Success(Unit)
    }

    suspend fun getDeviceHealth(deviceId: Long): NetworkResult<DeviceHealthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDeviceHealth(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch device health: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getMyDeviceHealth(): NetworkResult<DeviceHealthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyDeviceHealth()
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch own device health: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
