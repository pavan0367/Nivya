package com.nivya.data.repository

import android.content.Context
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.LocationEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.LocationHistoryResponseDto
import com.nivya.core.network.dto.LocationStatusResponseDto
import com.nivya.core.network.dto.LocationTelemetryRequestDto
import com.nivya.core.security.SecureTokenStorage
import com.nivya.services.location.LocationSnapshot
import com.nivya.services.location.LocationTelemetryCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository coordinating location telemetry collection via official LocationManager APIs,
 * offline queueing via Room, and remote backend synchronization.
 */
class LocationRepository(
    private val context: Context,
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor
) {

    private val locationDao = database.locationDao()

    fun getLatestLocationFlow(): Flow<LocationEntity?> = locationDao.getLatestLocationFlow()

    suspend fun collectAndRecordLocation(sourceMode: String = "FOREGROUND"): LocationSnapshot = withContext(Dispatchers.IO) {
        val snapshot = LocationTelemetryCollector.collect(context, sourceMode)
        val deviceUuid = tokenStorage.getDeviceUuid()

        val entity = LocationEntity(
            deviceUuid = deviceUuid,
            latitude = snapshot.latitude,
            longitude = snapshot.longitude,
            accuracyMeters = snapshot.accuracyMeters,
            altitudeMeters = snapshot.altitudeMeters,
            speedMetersPerSec = snapshot.speedMetersPerSec,
            bearingDegrees = snapshot.bearingDegrees,
            provider = snapshot.provider,
            isGpsAvailable = snapshot.isGpsAvailable,
            isNetworkAvailable = snapshot.isNetworkAvailable,
            permissionState = snapshot.permissionState.name,
            isBackgroundConsented = snapshot.isBackgroundConsented,
            isStale = snapshot.isStale,
            sourceMode = snapshot.sourceMode,
            recordedAt = snapshot.timestamp,
            isSynced = false
        )

        val insertedId = locationDao.insertLocationReading(entity)

        // If online and authenticated, push to backend & flush queue
        if (networkMonitor.isOnline.value && tokenStorage.hasAccessToken()) {
            try {
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(snapshot.timestamp))

                val request = LocationTelemetryRequestDto(
                    deviceUuid = deviceUuid,
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                    accuracyMeters = snapshot.accuracyMeters,
                    altitudeMeters = snapshot.altitudeMeters,
                    speedMetersPerSec = snapshot.speedMetersPerSec,
                    bearingDegrees = snapshot.bearingDegrees,
                    provider = snapshot.provider,
                    isGpsAvailable = snapshot.isGpsAvailable,
                    isNetworkAvailable = snapshot.isNetworkAvailable,
                    permissionState = snapshot.permissionState.name,
                    isBackgroundConsented = snapshot.isBackgroundConsented,
                    sourceMode = snapshot.sourceMode,
                    recordedAt = isoDate
                )
                val response = apiService.sendLocationTelemetry(request)
                if (response.isSuccessful) {
                    locationDao.markAsSynced(listOf(insertedId))
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

        val unsynced = locationDao.getUnsyncedLocations()
        if (unsynced.isEmpty()) return@withContext 0

        val syncedIds = mutableListOf<Long>()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (item in unsynced) {
            try {
                val request = LocationTelemetryRequestDto(
                    deviceUuid = item.deviceUuid,
                    latitude = item.latitude,
                    longitude = item.longitude,
                    accuracyMeters = item.accuracyMeters,
                    altitudeMeters = item.altitudeMeters,
                    speedMetersPerSec = item.speedMetersPerSec,
                    bearingDegrees = item.bearingDegrees,
                    provider = item.provider,
                    isGpsAvailable = item.isGpsAvailable,
                    isNetworkAvailable = item.isNetworkAvailable,
                    permissionState = item.permissionState,
                    isBackgroundConsented = item.isBackgroundConsented,
                    sourceMode = item.sourceMode,
                    recordedAt = sdf.format(Date(item.recordedAt))
                )
                val response = apiService.sendLocationTelemetry(request)
                if (response.isSuccessful) {
                    syncedIds.add(item.id)
                }
            } catch (_: Exception) {
                break
            }
        }

        if (syncedIds.isNotEmpty()) {
            locationDao.markAsSynced(syncedIds)
        }

        syncedIds.size
    }

    suspend fun getCurrentLocation(deviceId: Long): NetworkResult<LocationStatusResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentLocation(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch device location")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getLocationHistory(
        deviceId: Long,
        startTime: String? = null,
        endTime: String? = null
    ): NetworkResult<LocationHistoryResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLocationHistory(deviceId, startTime, endTime)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!)
            } else {
                NetworkResult.Error(response.code(), response.body()?.message ?: "Failed to fetch location history")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
