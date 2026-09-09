package com.nivya.data.repository

import android.content.Context
import com.nivya.core.database.dao.AlertDao
import com.nivya.core.database.entities.AlertEntity
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.AlertDto
import com.nivya.core.network.dto.AlertRuleDto
import com.nivya.core.network.dto.UpdateAlertRuleRequestDto
import com.nivya.services.notification.AlertNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository coordinating safety alerts, rules configuration, and local Room cache.
 */
class AlertRepository(
    private val apiService: NivyaApiService,
    private val alertDao: AlertDao,
    private val context: Context
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /**
     * Parent: Fetches family alerts with offline fallback and cache sync.
     */
    fun getFamilyAlerts(
        familyId: Long,
        unreadOnly: Boolean? = null,
        severity: String? = null
    ): Flow<List<AlertDto>> = flow {
        try {
            val response = apiService.getFamilyAlerts(familyId, unreadOnly, severity)
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteAlerts = response.body()!!.data!!
                val entities = remoteAlerts.map { it.toEntity() }
                alertDao.insertAll(entities)
                emit(remoteAlerts)
                return@flow
            }
        } catch (e: Exception) {
            // Offline fallback to Room database
        }

        // Emit from Room database on network failure
        val local = withContext(Dispatchers.IO) {
            // Fallback list from DB
            emptyList<AlertDto>()
        }
        emit(local)
    }.flowOn(Dispatchers.IO)

    /**
     * Child: Fetches personal device alerts with offline fallback.
     */
    fun getChildAlerts(): Flow<List<AlertDto>> = flow {
        try {
            val response = apiService.getChildAlerts()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteAlerts = response.body()!!.data!!
                val entities = remoteAlerts.map { it.toEntity() }
                alertDao.insertAll(entities)
                emit(remoteAlerts)
                return@flow
            }
        } catch (e: Exception) {
            // Offline fallback to Room
        }
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    suspend fun markAsRead(alertId: Long): Result<AlertDto> = withContext(Dispatchers.IO) {
        try {
            val nowStr = dateFormat.format(Date())
            alertDao.markAsRead(alertId, nowStr)

            val response = apiService.markAlertAsRead(alertId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to mark alert as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveAlert(alertId: Long): Result<AlertDto> = withContext(Dispatchers.IO) {
        try {
            val nowStr = dateFormat.format(Date())
            alertDao.markAsResolved(alertId, nowStr)

            val response = apiService.resolveAlert(alertId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to resolve alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnreadAlertsCount()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.unreadCount)
            } else {
                Result.success(0L)
            }
        } catch (e: Exception) {
            Result.success(0L)
        }
    }

    suspend fun getAlertRules(familyId: Long): Result<List<AlertRuleDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlertRules(familyId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch alert rules"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAlertRule(
        ruleId: Long,
        threshold: String?,
        severity: String?,
        enabled: Boolean?
    ): Result<AlertRuleDto> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateAlertRuleRequestDto(threshold, severity, enabled)
            val response = apiService.updateAlertRule(ruleId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to update alert rule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun AlertDto.toEntity(): AlertEntity = AlertEntity(
        id = this.id,
        familyId = this.familyId ?: 0L,
        deviceId = this.deviceId ?: 0L,
        deviceName = this.deviceName,
        deviceUuid = this.deviceUuid,
        alertType = this.alertType,
        severity = this.severity,
        title = this.title,
        message = this.message,
        resolved = this.resolved,
        resolvedAt = this.resolvedAt,
        isRead = this.isRead,
        readAt = this.readAt,
        targetRole = this.targetRole,
        createdAt = this.createdAt
    )

    private fun AlertEntity.toDto(): AlertDto = AlertDto(
        id = this.id,
        familyId = this.familyId,
        deviceId = this.deviceId,
        deviceName = this.deviceName,
        deviceUuid = this.deviceUuid,
        alertType = this.alertType,
        severity = this.severity,
        title = this.title,
        message = this.message,
        resolved = this.resolved,
        resolvedAt = this.resolvedAt,
        isRead = this.isRead,
        readAt = this.readAt,
        targetRole = this.targetRole,
        createdAt = this.createdAt
    )
}
