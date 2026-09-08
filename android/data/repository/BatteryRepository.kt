package com.nivya.data.repository

import android.content.Context
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.BatteryEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.SecureTokenStorage
import com.nivya.services.telemetry.BatterySnapshot
import com.nivya.services.telemetry.BatteryTelemetryCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating local battery monitoring, offline queuing via Room,
 * and background backend synchronization.
 */
class BatteryRepository(
    private val context: Context,
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor
) {

    private val batteryDao = database.batteryDao()

    fun getLatestBatteryFlow(): Flow<BatteryEntity?> = batteryDao.getLatestBatteryFlow()

    suspend fun collectAndRecordBattery(): BatterySnapshot = withContext(Dispatchers.IO) {
        val snapshot = BatteryTelemetryCollector.collect(context)
        val deviceUuid = tokenStorage.getDeviceUuid()

        // 1. Insert into local Room database
        val entity = BatteryEntity(
            deviceUuid = deviceUuid,
            batteryPct = snapshot.percentage,
            chargingState = snapshot.chargingState,
            batteryState = snapshot.batteryState,
            health = snapshot.health,
            temperatureCelsius = snapshot.temperatureCelsius,
            recordedAt = snapshot.timestamp,
            isSynced = false
        )
        val insertedId = batteryDao.insertBatteryReading(entity)

        // 2. If online and authenticated, push to backend & flush queue
        if (networkMonitor.isOnline.value && tokenStorage.hasAccessToken()) {
            try {
                val req = BatteryTelemetryRequestDto(
                    deviceUuid = deviceUuid,
                    batteryPct = snapshot.percentage,
                    chargingState = snapshot.chargingState,
                    batteryState = snapshot.batteryState,
                    health = snapshot.health,
                    temperatureCelsius = snapshot.temperatureCelsius
                )
                val response = apiService.sendBatteryTelemetry(req)
                if (response.isSuccessful) {
                    batteryDao.markAsSynced(listOf(insertedId))
                    // Flush any pending historical queue
                    flushOfflineQueue()
                }
            } catch (_: Exception) {
                // Keep isSynced = false for retry
            }
        }

        snapshot
    }

    suspend fun flushOfflineQueue(): Int = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value || !tokenStorage.hasAccessToken()) {
            return@withContext 0
        }

        val unsynced = batteryDao.getUnsyncedReadings()
        if (unsynced.isEmpty()) return@withContext 0

        val syncedIds = mutableListOf<Long>()
        for (item in unsynced) {
            try {
                val req = BatteryTelemetryRequestDto(
                    deviceUuid = item.deviceUuid,
                    batteryPct = item.batteryPct,
                    chargingState = item.chargingState,
                    batteryState = item.batteryState,
                    health = item.health,
                    temperatureCelsius = item.temperatureCelsius
                )
                val response = apiService.sendBatteryTelemetry(req)
                if (response.isSuccessful) {
                    syncedIds.add(item.id)
                }
            } catch (_: Exception) {
                break // Stop on connection failure
            }
        }

        if (syncedIds.isNotEmpty()) {
            batteryDao.markAsSynced(syncedIds)
        }

        syncedIds.size
    }

    suspend fun getCurrentBattery(deviceId: Long): NetworkResult<BatteryStatusResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentBattery(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch battery status")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getBatteryHistory(deviceId: Long): NetworkResult<BatteryHistoryResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBatteryHistory(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch battery history")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getBatteryTrends(deviceId: Long): NetworkResult<BatteryTrendResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBatteryTrends(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch battery trends")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
