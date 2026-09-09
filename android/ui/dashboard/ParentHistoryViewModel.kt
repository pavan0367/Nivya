package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.HistoryEventDetailDto
import com.nivya.core.network.dto.HistoryEventDto
import com.nivya.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val deviceId: Long = 1L,
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
 */
class ParentHistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentHistoryUiState())
    val uiState: StateFlow<ParentHistoryUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
        loadHistory(page = 0)
    }

    fun loadApplications() {
        viewModelScope.launch {
            historyRepository.getDistinctApplications(_uiState.value.deviceId).onSuccess { apps ->
                _uiState.update { it.copy(availableApps = apps) }
            }
        }
    }

    fun loadHistory(page: Int = _uiState.value.currentPage) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val (startDate, endDate) = computeDateBounds(_uiState.value.dateFilter)

            historyRepository.getHistory(
                deviceId = _uiState.value.deviceId,
                page = page,
                size = _uiState.value.pageSize,
                startDate = startDate,
                endDate = endDate,
                application = _uiState.value.selectedApp
            ).collect { response ->
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
                    // Fallback to sample chronological demonstration data if offline/empty
                    val sampleEvents = getSampleHistory()
                    val filtered = filterSampleEvents(sampleEvents, _uiState.value.selectedApp, _uiState.value.dateFilter)

                    _uiState.update {
                        it.copy(
                            items = filtered,
                            availableApps = if (it.availableApps.isNotEmpty()) it.availableApps else listOf("WhatsApp", "Chrome", "YouTube", "Files", "Phone"),
                            currentPage = 0,
                            totalPages = 1,
                            totalElements = filtered.size.toLong(),
                            hasNext = false,
                            hasPrevious = false,
                            isLoading = false,
                            isStale = true,
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
        viewModelScope.launch {
            val localItem = _uiState.value.items.find { it.id == eventId }
            if (localItem != null) {
                _uiState.update {
                    it.copy(
                        selectedEventDetail = HistoryEventDetailDto(
                            id = localItem.id,
                            deviceId = localItem.deviceId,
                            deviceName = "Alex's Galaxy A54",
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

            historyRepository.getEventDetail(_uiState.value.deviceId, eventId).onSuccess { detail ->
                _uiState.update { it.copy(selectedEventDetail = detail) }
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedEventDetail = null) }
    }

    fun refresh() {
        loadApplications()
        loadHistory(_uiState.value.currentPage)
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

    private fun filterSampleEvents(
        events: List<HistoryEventDto>,
        selectedApp: String?,
        _datePreset: DatePreset
    ): List<HistoryEventDto> {
        return events.filter { event ->
            (selectedApp == null || event.appName.equals(selectedApp, ignoreCase = true))
        }
    }

    private fun getSampleHistory(): List<HistoryEventDto> {
        val now = Instant.now()
        return listOf(
            HistoryEventDto(
                id = 1L,
                deviceId = 1L,
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                broadActivity = "Chatting with Arun",
                activityLabel = "Arun",
                category = "COMMUNICATION",
                durationSeconds = 840,
                durationFormatted = "14m",
                eventTimestamp = now.minus(20, ChronoUnit.MINUTES).toString()
            ),
            HistoryEventDto(
                id = 2L,
                deviceId = 1L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                broadActivity = "Browsing",
                activityLabel = null,
                category = "BROWSING",
                durationSeconds = 1200,
                durationFormatted = "20m",
                eventTimestamp = now.minus(50, ChronoUnit.MINUTES).toString()
            ),
            HistoryEventDto(
                id = 3L,
                deviceId = 1L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                broadActivity = "Watching",
                activityLabel = "Science Documentary",
                category = "ENTERTAINMENT",
                durationSeconds = 1800,
                durationFormatted = "30m",
                eventTimestamp = now.minus(2, ChronoUnit.HOURS).toString()
            ),
            HistoryEventDto(
                id = 4L,
                deviceId = 1L,
                packageName = "com.google.android.apps.docs",
                appName = "Files",
                broadActivity = "Viewing report.pdf",
                activityLabel = "report.pdf",
                category = "PRODUCTIVITY",
                durationSeconds = 300,
                durationFormatted = "5m",
                eventTimestamp = now.minus(3, ChronoUnit.HOURS).toString()
            ),
            HistoryEventDto(
                id = 5L,
                deviceId = 1L,
                packageName = "com.google.android.dialer",
                appName = "Phone",
                broadActivity = "In call with Mom",
                activityLabel = "Mom",
                category = "COMMUNICATION",
                durationSeconds = 480,
                durationFormatted = "8m",
                eventTimestamp = now.minus(4, ChronoUnit.HOURS).toString()
            )
        )
    }

    companion object {
        fun provideFactory(
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentHistoryViewModel(historyRepository) as T
            }
        }
    }
}
