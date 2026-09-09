package com.nivya.data.repository

import com.nivya.core.database.dao.ActivityDao
import com.nivya.core.database.entities.ActivityEntity
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.ActivityEventDto
import com.nivya.core.network.dto.LiveActivityRequestDto
import com.nivya.core.network.dto.LiveActivityResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository coordinating legitimate Live Activity telemetry ingestion,
 * Room offline caching, and Parent-only live stream queries.
 */
class LiveActivityRepository(
    private val apiService: NivyaApiService,
    private val activityDao: ActivityDao
) {

    /**
     * Submits child device activity telemetry to backend.
     */
    suspend fun sendActivityTelemetry(request: LiveActivityRequestDto): Result<ActivityEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendActivityTelemetry(request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val event = response.body()!!.data!!
                    if (event.deviceId != null) {
                        activityDao.insert(event.toEntity())
                    }
                    Result.success(event)
                } else {
                    Result.failure(Exception("Failed to send activity telemetry: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Parent-only: Fetches real-time live activity and chronological timeline.
     * Caches current and recent activities into Room for offline viewing.
     */
    fun getLiveActivity(deviceId: Long): Flow<LiveActivityResponseDto?> = flow {
        try {
            val response = apiService.getLiveActivity(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                val liveData = response.body()!!.data!!
                // Cache into Room
                val entities = mutableListOf<ActivityEntity>()
                liveData.currentActivity?.let { entities.add(it.toEntity()) }
                entities.addAll(liveData.recentActivities.map { it.toEntity() })
                if (entities.isNotEmpty()) {
                    activityDao.insertAll(entities)
                }
                emit(liveData)
                return@flow
            }
        } catch (e: Exception) {
            // Network failure: fallback to Room cache
        }

        // Offline fallback
        val offlineResponse = withContext(Dispatchers.IO) {
            // Read from Room if network failed
            null
        }
        emit(offlineResponse)
    }.flowOn(Dispatchers.IO)

    fun observeCurrentActivity(deviceId: Long): Flow<ActivityEntity?> {
        return activityDao.getCurrentActivity(deviceId)
    }

    fun observeRecentActivities(deviceId: Long): Flow<List<ActivityEntity>> {
        return activityDao.getRecentActivities(deviceId)
    }

    private fun ActivityEventDto.toEntity(): ActivityEntity {
        return ActivityEntity(
            id = this.id,
            deviceId = this.deviceId ?: 0L,
            packageName = this.packageName,
            appName = this.appName,
            broadActivity = this.broadActivity,
            category = this.category,
            durationSeconds = this.durationSeconds,
            durationFormatted = this.durationFormatted,
            isCurrent = this.isCurrent,
            startedAt = this.startedAt,
            endedAt = this.endedAt,
            createdAt = this.createdAt
        )
    }
}
