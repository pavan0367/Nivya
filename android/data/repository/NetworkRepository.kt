package com.nivya.data.repository

import android.content.Context
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.NetworkEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.NetworkHistoryResponseDto
import com.nivya.core.network.dto.NetworkStatusResponseDto
import com.nivya.core.network.dto.NetworkTelemetryRequestDto
import com.nivya.core.security.SecureTokenStorage
import com.nivya.services.telemetry.NetworkSnapshot
import com.nivya.services.telemetry.NetworkTelemetryCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating network telemetry collection, offline queuing via Room,
 * and background backend synchronization.
 */
class NetworkRepository(
    private val context: Context,
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor
) {

    private val networkDao = database.networkDao()

    fun getLatestNetworkFlow(): Flow<NetworkEntity?> = networkDao.getLatestNetworkFlow()

    suspend fun collectAndRecordNetwork(): NetworkSnapshot = withContext(Dispatchers.IO) {
        val snapshot = NetworkTelemetryCollector.collect(context)
        val deviceUuid = tokenStorage.getDeviceUuid()

        // 1. Insert into local Room database
        val entity = NetworkEntity(
            deviceUuid = deviceUuid,
            networkType = snapshot.networkType,
            connectionType = snapshot.connectionType,
            isNetworkAvailable = snapshot.isNetworkAvailable,
            isInternetAvailable = snapshot.isInternetAvailable,
            signalLevel = snapshot.signalLevel,
            signalDbm = snapshot.signalDbm,
            quality = snapshot.quality,
            recordedAt = snapshot.timestamp,
            isSynced = false
        )
        val insertedId = networkDao.insertNetworkReading(entity)

        // 2. If online and authenticated, push to backend & flush queue
        if (networkMonitor.isOnline.value && tokenStorage.hasAccessToken()) {
            try {
                val req = NetworkTelemetryRequestDto(
                    deviceUuid = deviceUuid,
                    networkType = snapshot.networkType,
                    connectionType = snapshot.connectionType,
                    isNetworkAvailable = snapshot.isNetworkAvailable,
                    isInternetAvailable = snapshot.isInternetAvailable,
                    signalLevel = snapshot.signalLevel,
                    signalDbm = snapshot.signalDbm,
                    quality = snapshot.quality,
                    ssid = snapshot.ssid,
                    ipAddress = snapshot.ipAddress
                )
                val response = apiService.sendNetworkTelemetry(req)
                if (response.isSuccessful) {
                    networkDao.markAsSynced(listOf(insertedId))
                    flushOfflineQueue()
                }
            } catch (_: Exception) {
                // Kept isSynced = false for retry
            }
        }

        snapshot
    }

    suspend fun flushOfflineQueue(): Int = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value || !tokenStorage.hasAccessToken()) {
            return@withContext 0
        }

        val unsynced = networkDao.getUnsyncedReadings()
        if (unsynced.isEmpty()) return@withContext 0

        val syncedIds = mutableListOf<Long>()
        for (item in unsynced) {
            try {
                val req = NetworkTelemetryRequestDto(
                    deviceUuid = item.deviceUuid,
                    networkType = item.networkType,
                    connectionType = item.connectionType,
                    isNetworkAvailable = item.isNetworkAvailable,
                    isInternetAvailable = item.isInternetAvailable,
                    signalLevel = item.signalLevel,
                    signalDbm = item.signalDbm,
                    quality = item.quality
                )
                val response = apiService.sendNetworkTelemetry(req)
                if (response.isSuccessful) {
                    syncedIds.add(item.id)
                }
            } catch (_: Exception) {
                break
            }
        }

        if (syncedIds.isNotEmpty()) {
            networkDao.markAsSynced(syncedIds)
        }

        syncedIds.size
    }

    suspend fun getCurrentNetwork(deviceId: Long): NetworkResult<NetworkStatusResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentNetwork(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch network status")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getNetworkHistory(deviceId: Long): NetworkResult<NetworkHistoryResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNetworkHistory(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch network history")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
