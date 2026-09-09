package com.nivya.data.repository

import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Parent and Child Convocation messaging.
 * Completely isolated from other telemetry and hardware repositories.
 */
class ConvocationRepository(
    private val apiService: NivyaApiService
) {

    // =========================================================================
    // PARENT OPERATIONS
    // =========================================================================

    suspend fun parentSendMessage(
        message: String,
        receiverUserId: Long? = null
    ): Result<ParentConvocationMessageDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.parentSendConvocationMessage(
                ParentSendMessageRequestDto(receiverUserId = receiverUserId, message = message)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to send convocation message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentGetHistory(): Result<List<ParentConvocationMessageDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.parentGetConvocationHistory()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch convocation history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentGetSeenState(): Result<Map<String, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.parentGetConvocationSeenState()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch seen state: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // CHILD OPERATIONS
    // =========================================================================

    suspend fun childGetUnread(): Result<List<ChildConvocationMessageDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.childGetConvocationUnread()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch unread messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childStartViewing(): Result<ChildViewingSessionResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.childStartConvocationViewing()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to activate viewing session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childSendMessage(message: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.childSendConvocationMessage(
                ChildSendMessageRequestDto(message = message)
            )
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send note: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childGetVisibility(): Result<ChildVisibilityStateResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.childGetConvocationVisibility()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch visibility state: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
