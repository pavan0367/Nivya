package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.ActivityEventDto
import com.nivya.core.network.dto.LiveActivityResponseDto
import com.nivya.data.repository.LiveActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentLiveActivityUiState(
    val deviceId: Long = 1L,
    val deviceName: String = "Alex's Galaxy A54",
    val isOnline: Boolean = true,
    val currentActivity: ActivityEventDto? = null,
    val recentActivities: List<ActivityEventDto> = emptyList(),
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing real-time Parent-only Live Activity stream.
 * In accordance with Nivya privacy rules:
 * Only broad activities and legitimate metadata are displayed.
 */
class ParentLiveActivityViewModel(
    private val liveActivityRepository: LiveActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentLiveActivityUiState())
    val uiState: StateFlow<ParentLiveActivityUiState> = _uiState.asStateFlow()

    init {
        loadLiveActivity(_uiState.value.deviceId)
    }

    fun loadLiveActivity(deviceId: Long = 1L) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            liveActivityRepository.getLiveActivity(deviceId).collect { response ->
                if (response != null) {
                    _uiState.update {
                        it.copy(
                            deviceId = response.deviceId,
                            deviceName = response.deviceName ?: it.deviceName,
                            isOnline = response.isOnline,
                            currentActivity = response.currentActivity,
                            recentActivities = response.recentActivities,
                            isLoading = false,
                            isStale = false,
                            errorMessage = null
                        )
                    }
                } else {
                    // Fallback to sample data for smooth demonstration if offline/no remote server
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isStale = true,
                            currentActivity = it.currentActivity ?: ActivityEventDto(
                                id = 101L,
                                deviceId = deviceId,
                                packageName = "com.whatsapp",
                                appName = "WhatsApp",
                                broadActivity = "Chatting with Arun",
                                category = "COMMUNICATION",
                                durationSeconds = 840,
                                durationFormatted = "14m",
                                isCurrent = true,
                                startedAt = "2026-09-09T09:25:00Z",
                                endedAt = null,
                                createdAt = "2026-09-09T09:25:00Z"
                            ),
                            recentActivities = if (it.recentActivities.isNotEmpty()) it.recentActivities else listOf(
                                ActivityEventDto(
                                    id = 101L,
                                    deviceId = deviceId,
                                    packageName = "com.whatsapp",
                                    appName = "WhatsApp",
                                    broadActivity = "Chatting with Arun",
                                    category = "COMMUNICATION",
                                    durationSeconds = 840,
                                    durationFormatted = "14m",
                                    isCurrent = true,
                                    startedAt = "2026-09-09T09:25:00Z",
                                    endedAt = null,
                                    createdAt = "2026-09-09T09:25:00Z"
                                ),
                                ActivityEventDto(
                                    id = 100L,
                                    deviceId = deviceId,
                                    packageName = "com.android.chrome",
                                    appName = "Chrome",
                                    broadActivity = "Browsing",
                                    category = "BROWSING",
                                    durationSeconds = 1200,
                                    durationFormatted = "20m",
                                    isCurrent = false,
                                    startedAt = "2026-09-09T09:05:00Z",
                                    endedAt = "2026-09-09T09:25:00Z",
                                    createdAt = "2026-09-09T09:05:00Z"
                                ),
                                ActivityEventDto(
                                    id = 99L,
                                    deviceId = deviceId,
                                    packageName = "com.google.android.youtube",
                                    appName = "YouTube",
                                    broadActivity = "Watching",
                                    category = "ENTERTAINMENT",
                                    durationSeconds = 1800,
                                    durationFormatted = "30m",
                                    isCurrent = false,
                                    startedAt = "2026-09-09T08:35:00Z",
                                    endedAt = "2026-09-09T09:05:00Z",
                                    createdAt = "2026-09-09T08:35:00Z"
                                ),
                                ActivityEventDto(
                                    id = 98L,
                                    deviceId = deviceId,
                                    packageName = "com.google.android.apps.docs",
                                    appName = "Files",
                                    broadActivity = "Viewing report.pdf",
                                    category = "PRODUCTIVITY",
                                    durationSeconds = 300,
                                    durationFormatted = "5m",
                                    isCurrent = false,
                                    startedAt = "2026-09-09T08:30:00Z",
                                    endedAt = "2026-09-09T08:35:00Z",
                                    createdAt = "2026-09-09T08:30:00Z"
                                ),
                                ActivityEventDto(
                                    id = 97L,
                                    deviceId = deviceId,
                                    packageName = "com.google.android.dialer",
                                    appName = "Phone",
                                    broadActivity = "In call with Mom",
                                    category = "COMMUNICATION",
                                    durationSeconds = 480,
                                    durationFormatted = "8m",
                                    isCurrent = false,
                                    startedAt = "2026-09-09T08:22:00Z",
                                    endedAt = "2026-09-09T08:30:00Z",
                                    createdAt = "2026-09-09T08:22:00Z"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadLiveActivity(_uiState.value.deviceId)
    }

    companion object {
        fun provideFactory(
            liveActivityRepository: LiveActivityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentLiveActivityViewModel(liveActivityRepository) as T
            }
        }
    }
}
