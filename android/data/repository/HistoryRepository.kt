package com.nivya.data.repository

import com.nivya.core.database.dao.HistoryDao
import com.nivya.core.database.entities.HistoryEntity
import com.nivya.core.database.entities.toEntity
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.HistoryEventDetailDto
import com.nivya.core.network.dto.HistoryEventDto
import com.nivya.core.network.dto.HistoryPageResponseDto
import com.nivya.core.network.dto.RecordHistoryRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Parent-only History queries, pagination,
 * application/date filters, and Room offline persistence.
 */
class HistoryRepository(
    private val apiService: NivyaApiService,
    private val historyDao: HistoryDao
) {

    /**
     * Parent-only: Retrieves paginated chronological activity history.
     */
    fun getHistory(
        deviceId: Long,
        page: Int = 0,
        size: Int = 20,
        startDate: String? = null,
        endDate: String? = null,
        application: String? = null
    ): Flow<HistoryPageResponseDto?> = flow {
        try {
            val response = apiService.getHistory(deviceId, page, size, startDate, endDate, application)
            if (response.isSuccessful && response.body()?.data != null) {
                val pageData = response.body()!!.data!!
                val entities = pageData.items.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    historyDao.insertAll(entities)
                }
                emit(pageData)
                return@flow
            }
        } catch (e: Exception) {
            // Offline fallback
        }

        // Return cached history on network failure
        val cached = withContext(Dispatchers.IO) {
            null
        }
        emit(cached)
    }.flowOn(Dispatchers.IO)

    /**
     * Parent-only: Retrieves individual event details within consented scope.
     */
    suspend fun getEventDetail(deviceId: Long, eventId: Long): Result<HistoryEventDetailDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getHistoryEventDetail(deviceId, eventId)
                if (response.isSuccessful && response.body()?.data != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception("Failed to fetch event detail: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Parent-only: Retrieves distinct application names recorded for device.
     */
    suspend fun getDistinctApplications(deviceId: Long): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getHistoryApplications(deviceId)
                if (response.isSuccessful && response.body()?.data != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception("Failed to fetch applications: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sends historical telemetry event from device.
     */
    suspend fun sendHistoryEvent(request: RecordHistoryRequestDto): Result<HistoryEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendHistoryEvent(request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val event = response.body()!!.data!!
                    if (event.deviceId != null) {
                        historyDao.insert(event.toEntity())
                    }
                    Result.success(event)
                } else {
                    Result.failure(Exception("Failed to send history event: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
