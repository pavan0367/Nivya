package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.database.entities.ActivityEntity
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.network.dto.ActivityEventDto
import com.nivya.core.network.dto.LiveActivityResponseDto
import com.nivya.core.security.TokenStorage
import com.nivya.data.repository.LiveActivityRepository
import com.nivya.data.repository.PairingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentLiveActivityUiState(
    val deviceId: Long = 0L,
    val deviceName: String = "",
    val isOnline: Boolean = false,
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
 * No hardcoded demo fallback data is ever shown.
 */
class ParentLiveActivityViewModel(
    private val liveActivityRepository: LiveActivityRepository? = null,
    private val pairingRepository: PairingRepository? = null,
    private val tokenStorage: TokenStorage? = null,
    private val getCachedDevicesProvider: (suspend () -> List<DeviceStatusEntity>)? = null,
    private val getLiveActivityProvider: ((deviceId: Long) -> Flow<LiveActivityResponseDto?>)? = null,
    private val observeRecentActivitiesProvider: ((deviceId: Long) -> Flow<List<ActivityEntity>>)? = null,
    private val observeCurrentActivityProvider: ((deviceId: Long) -> Flow<ActivityEntity?>)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentLiveActivityUiState())
    val uiState: StateFlow<ParentLiveActivityUiState> = _uiState.asStateFlow()

    init {
        resolveAndLoad()
    }

    fun resolveAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val parentUuid = tokenStorage?.getDeviceUuid()
            val cachedDevices = if (getCachedDevicesProvider != null) {
                getCachedDevicesProvider.invoke()
            } else {
                pairingRepository?.getCachedDevices()?.firstOrNull() ?: emptyList()
            }
            val child = cachedDevices.firstOrNull { it.deviceUuid != parentUuid }

            if (child != null) {
                _uiState.update {
                    it.copy(
                        deviceId = child.deviceId,
                        deviceName = child.deviceName,
                        isOnline = child.isOnline
                    )
                }
                loadLiveActivity(child.deviceId)
            } else if (_uiState.value.deviceId > 0L) {
                loadLiveActivity(_uiState.value.deviceId)
            } else {
                _uiState.update {
                    it.copy(
                        deviceId = 0L,
                        deviceName = "No Paired Child Device",
                        isOnline = false,
                        currentActivity = null,
                        recentActivities = emptyList(),
                        isLoading = false,
                        isStale = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun loadLiveActivity(deviceId: Long = _uiState.value.deviceId) {
        if (deviceId <= 0L) {
            _uiState.update {
                it.copy(
                    deviceId = 0L,
                    deviceName = if (it.deviceName.isNotBlank()) it.deviceName else "No Paired Child Device",
                    isOnline = false,
                    currentActivity = null,
                    recentActivities = emptyList(),
                    isLoading = false,
                    isStale = false,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val liveFlow = if (getLiveActivityProvider != null) {
                getLiveActivityProvider.invoke(deviceId)
            } else {
                liveActivityRepository?.getLiveActivity(deviceId) ?: flowOf(null)
            }

            liveFlow.collect { response ->
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
                    // Fallback to local Room cache only - NEVER demo/sample fallback
                    val cachedRecent = if (observeRecentActivitiesProvider != null) {
                        observeRecentActivitiesProvider.invoke(deviceId).firstOrNull() ?: emptyList()
                    } else {
                        liveActivityRepository?.observeRecentActivities(deviceId)?.firstOrNull() ?: emptyList()
                    }

                    val cachedCurrent = if (observeCurrentActivityProvider != null) {
                        observeCurrentActivityProvider.invoke(deviceId).firstOrNull()
                    } else {
                        liveActivityRepository?.observeCurrentActivity(deviceId)?.firstOrNull()
                    }

                    val recentDtos = cachedRecent.map { it.toDto() }
                    val currentDto = cachedCurrent?.toDto()

                    _uiState.update {
                        it.copy(
                            deviceId = deviceId,
                            isOnline = false,
                            currentActivity = currentDto,
                            recentActivities = recentDtos,
                            isLoading = false,
                            isStale = recentDtos.isNotEmpty() || currentDto != null,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.deviceId > 0L) {
            loadLiveActivity(_uiState.value.deviceId)
        } else {
            resolveAndLoad()
        }
    }

    companion object {
        fun provideFactory(
            liveActivityRepository: LiveActivityRepository,
            pairingRepository: PairingRepository? = null,
            tokenStorage: TokenStorage? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentLiveActivityViewModel(
                    liveActivityRepository = liveActivityRepository,
                    pairingRepository = pairingRepository,
                    tokenStorage = tokenStorage
                ) as T
            }
        }
    }
}

private fun ActivityEntity.toDto() = ActivityEventDto(
    id = this.id,
    deviceId = this.deviceId,
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
