package com.nivya.data.repository

import android.os.Build
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.network.NetworkMonitor
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Repository managing pairing code generation, mutual pairing handshake,
 * and offline-resilient local caching via Room database.
 */
class PairingRepository(
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val database: NivyaDatabase,
    private val networkMonitor: NetworkMonitor
) {

    private val familyDao = database.familyDao()
    private val deviceStatusDao = database.deviceStatusDao()

    fun getCachedFamily(): Flow<FamilyEntity?> = familyDao.getFamilyFlow()
    fun getCachedDevices(): Flow<List<DeviceStatusEntity>> = deviceStatusDao.getDevicesFlow()

    private fun buildDeviceInfo(): DeviceInfoDto {
        return DeviceInfoDto(
            deviceUuid = tokenStorage.getDeviceUuid(),
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            platform = "ANDROID",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "1.0.0"
        )
    }

    suspend fun generatePairingCode(): NetworkResult<PairingCodeResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.generatePairingCode(
                    GenerateCodeRequestDto(deviceInfo = buildDeviceInfo())
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!)
                } else {
                    val msg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Failed to generate pairing code (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = msg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun connect(oppositeCode: String): NetworkResult<PairingStatusResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.connectDevices(
                    ConnectPairingRequestDto(
                        code = oppositeCode.trim().uppercase(),
                        deviceInfo = buildDeviceInfo()
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val status = response.body()!!.data!!
                    cachePairingStatus(status)
                    NetworkResult.Success(status)
                } else {
                    val msg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Pairing failed (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = msg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun getPairingStatus(): NetworkResult<PairingStatusResponseDto> {
        return withContext(Dispatchers.IO) {
            val isOnline = networkMonitor.isOnline.value
            if (isOnline) {
                try {
                    val response = apiService.getPairingStatus()
                    if (response.isSuccessful && response.body()?.data != null) {
                        val status = response.body()!!.data!!
                        cachePairingStatus(status)
                        return@withContext NetworkResult.Success(status)
                    }
                } catch (_: Exception) {
                    // Fall through to offline cache
                }
            }

            // Offline fallback: load from Room database
            val cachedFamily = familyDao.getFamily()
            if (cachedFamily != null) {
                val cachedDevices = deviceStatusDao.getDevicesFlow().firstOrNull() ?: emptyList()
                val offlineStatus = PairingStatusResponseDto(
                    paired = cachedFamily.isPaired,
                    familyId = cachedFamily.familyId,
                    familyCode = cachedFamily.familyCode,
                    familyName = cachedFamily.familyName,
                    userRole = cachedFamily.userRole,
                    members = emptyList(),
                    devices = cachedDevices.map { entity ->
                        DeviceStatusDto(
                            deviceId = entity.deviceId,
                            deviceUuid = entity.deviceUuid,
                            deviceName = entity.deviceName,
                            platform = entity.platform,
                            isOnline = entity.isOnline,
                            batteryPct = entity.batteryPct,
                            networkType = entity.networkType,
                            networkQuality = entity.networkQuality,
                            lastSyncAt = entity.lastSyncAt,
                            lastSeenAt = entity.lastSeenAt,
                            isStale = entity.isStale
                        )
                    }
                )
                NetworkResult.Success(offlineStatus)
            } else {
                NetworkResult.Error(code = 404, message = "No local pairing found and device is offline")
            }
        }
    }

    private suspend fun cachePairingStatus(status: PairingStatusResponseDto) {
        if (status.paired && status.familyId != null) {
            familyDao.insertFamily(
                FamilyEntity(
                    familyId = status.familyId,
                    familyCode = status.familyCode ?: "",
                    familyName = status.familyName ?: "Family",
                    userRole = status.userRole ?: tokenStorage.getUserRole() ?: "PARENT",
                    isPaired = true
                )
            )

            val deviceEntities = status.devices.map { d ->
                DeviceStatusEntity(
                    deviceId = d.deviceId,
                    deviceUuid = d.deviceUuid,
                    deviceName = d.deviceName,
                    platform = d.platform,
                    isOnline = d.isOnline,
                    batteryPct = d.batteryPct,
                    networkType = d.networkType,
                    networkQuality = d.networkQuality,
                    lastSyncAt = d.lastSyncAt,
                    lastSeenAt = d.lastSeenAt,
                    isStale = d.isStale
                )
            }
            deviceStatusDao.insertDevices(deviceEntities)
        }
    }

    suspend fun revokePairing(targetDeviceId: Long): NetworkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.revokePairing(RevokePairingRequestDto(targetDeviceId))
                if (response.isSuccessful) {
                    getPairingStatus() // Refresh cache
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(code = response.code(), message = "Failed to revoke device link")
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }
}
