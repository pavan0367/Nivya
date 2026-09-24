package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.network.dto.HistoryEventDetailDto
import com.nivya.core.network.dto.HistoryEventDto
import com.nivya.core.network.dto.HistoryPageResponseDto
import com.nivya.core.security.TokenStorage
import com.nivya.data.repository.HistoryRepository
import com.nivya.data.repository.PairingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class DatePreset(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

data class ParentHistoryUiState(
    val deviceId: Long = 0L,
    val deviceName: String = "",
    val items: List<HistoryEventDto> = emptyList(),
    val availableApps: List<String> = emptyList(),
    val selectedApp: String? = null,
    val dateFilter: DatePreset = DatePreset.ALL_TIME,
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val totalElements: Long = 0,
    val pageSize: Int = 15,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val selectedEventDetail: HistoryEventDetailDto? = null,
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing Parent-only chronological activity history,
 * pagination controls, multi-dimensional filters, and event details.
 * Bound strictly to authoritative backend and Room cached data.
 * No hardcoded demo fallback data is ever used.
 */
class ParentHistoryViewModel(
    private val historyRepository: HistoryRepository? = null,
    private val pairingRepository: PairingRepository? = null,
    private val tokenStorage: TokenStorage? = null,
    private val getCachedDevicesProvider: (suspend () -> List<DeviceStatusEntity>)? = null,
    private val getHistoryProvider: ((deviceId: Long, page: Int, size: Int, startDate: String?, endDate: String?, application: String?) -> Flow<HistoryPageResponseDto?>)? = null,
    private val getDistinctApplicationsProvider: (suspend (deviceId: Long) -> Result<List<String>>)? = null,
    private val getEventDetailProvider: (suspend (deviceId: Long, eventId: Long) -> Result<HistoryEventDetailDto>)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentHistoryUiState())
    val uiState: StateFlow<ParentHistoryUiState> = _uiState.asStateFlow()

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
                        deviceName = child.deviceName
                    )
                }
                loadApplications()
                loadHistory(page = 0)
            } else if (_uiState.value.deviceId > 0L) {
                loadApplications()
                loadHistory(page = 0)
            } else {
                _uiState.update {
                    it.copy(
                        deviceId = 0L,
                        deviceName = "No Paired Child Device",
                        items = emptyList(),
                        availableApps = emptyList(),
                        totalElements = 0L,
                        isLoading = false,
                        isStale = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun loadApplications() {
        val targetDeviceId = _uiState.value.deviceId
        if (targetDeviceId <= 0L) return

        viewModelScope.launch {
            val result = if (getDistinctApplicationsProvider != null) {
                getDistinctApplicationsProvider.invoke(targetDeviceId)
            } else {
                historyRepository?.getDistinctApplications(targetDeviceId) ?: Result.success(emptyList())
            }

            result.onSuccess { apps ->
                _uiState.update { it.copy(availableApps = apps) }
            }
        }
    }

    fun loadHistory(page: Int = _uiState.value.currentPage) {
        val targetDeviceId = _uiState.value.deviceId
        if (targetDeviceId <= 0L) {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    totalElements = 0L,
                    isLoading = false,
                    isStale = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val (startDate, endDate) = computeDateBounds(_uiState.value.dateFilter)

            val historyFlow = if (getHistoryProvider != null) {
                getHistoryProvider.invoke(
                    targetDeviceId,
                    page,
                    _uiState.value.pageSize,
                    startDate,
                    endDate,
                    _uiState.value.selectedApp
                )
            } else {
                historyRepository?.getHistory(
                    deviceId = targetDeviceId,
                    page = page,
                    size = _uiState.value.pageSize,
                    startDate = startDate,
                    endDate = endDate,
                    application = _uiState.value.selectedApp
                ) ?: flowOf(null)
            }

            historyFlow.collect { response ->
                if (response != null && response.items.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            items = response.items,
                            currentPage = response.currentPage,
                            totalPages = response.totalPages,
                            totalElements = response.totalElements,
                            hasNext = response.hasNext,
                            hasPrevious = response.hasPrevious,
                            isLoading = false,
                            isStale = false,
                            errorMessage = null
                        )
                    }
                } else {
                    // Authoritative empty / offline state - NEVER demo/sample fallback
                    _uiState.update {
                        it.copy(
                            items = emptyList(),
                            currentPage = 0,
                            totalPages = 1,
                            totalElements = 0L,
                            hasNext = false,
                            hasPrevious = false,
                            isLoading = false,
                            isStale = response == null,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun filterByApplication(app: String?) {
        _uiState.update { it.copy(selectedApp = app, currentPage = 0) }
        loadHistory(page = 0)
    }

    fun filterByDate(preset: DatePreset) {
        _uiState.update { it.copy(dateFilter = preset, currentPage = 0) }
        loadHistory(page = 0)
    }

    fun nextPage() {
        if (_uiState.value.hasNext) {
            loadHistory(page = _uiState.value.currentPage + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.hasPrevious && _uiState.value.currentPage > 0) {
            loadHistory(page = _uiState.value.currentPage - 1)
        }
    }

    fun selectEvent(eventId: Long) {
        val targetDeviceId = _uiState.value.deviceId
        val resolvedName = _uiState.value.deviceName.ifBlank { "Child Device" }

        viewModelScope.launch {
            val localItem = _uiState.value.items.find { it.id == eventId }
            if (localItem != null) {
                _uiState.update {
                    it.copy(
                        selectedEventDetail = HistoryEventDetailDto(
                            id = localItem.id,
                            deviceId = localItem.deviceId,
                            deviceName = resolvedName,
                            packageName = localItem.packageName,
                            appName = localItem.appName,
                            broadActivity = localItem.broadActivity,
                            activityLabel = localItem.activityLabel,
                            category = localItem.category,
                            durationSeconds = localItem.durationSeconds,
                            durationFormatted = localItem.durationFormatted,
                            details = "Consented activity session within authorized scope.",
                            eventTimestamp = localItem.eventTimestamp,
                            recordedAt = localItem.eventTimestamp
                        )
                    )
                }
            }

            if (targetDeviceId > 0L) {
                val detailResult = if (getEventDetailProvider != null) {
                    getEventDetailProvider.invoke(targetDeviceId, eventId)
                } else {
                    historyRepository?.getEventDetail(targetDeviceId, eventId) ?: Result.failure(Exception("Unavailable"))
                }

                detailResult.onSuccess { detail ->
                    _uiState.update { it.copy(selectedEventDetail = detail) }
                }
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedEventDetail = null) }
    }

    fun refresh() {
        if (_uiState.value.deviceId > 0L) {
            loadApplications()
            loadHistory(_uiState.value.currentPage)
        } else {
            resolveAndLoad()
        }
    }

    private fun computeDateBounds(preset: DatePreset): Pair<String?, String?> {
        val now = Instant.now()
        return when (preset) {
            DatePreset.ALL_TIME -> Pair(null, null)
            DatePreset.TODAY -> Pair(now.truncatedTo(ChronoUnit.DAYS).toString(), now.toString())
            DatePreset.LAST_7_DAYS -> Pair(now.minus(7, ChronoUnit.DAYS).toString(), now.toString())
            DatePreset.LAST_30_DAYS -> Pair(now.minus(30, ChronoUnit.DAYS).toString(), now.toString())
        }
    }

    companion object {
        fun provideFactory(
            historyRepository: HistoryRepository,
            pairingRepository: PairingRepository? = null,
            tokenStorage: TokenStorage? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentHistoryViewModel(
                    historyRepository = historyRepository,
                    pairingRepository = pairingRepository,
                    tokenStorage = tokenStorage
                ) as T
            }
        }
    }
}
