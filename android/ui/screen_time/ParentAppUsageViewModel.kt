package com.nivya.ui.screen_time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.AppUsageResponseDto
import com.nivya.core.network.dto.UsageSummaryResponseDto
import com.nivya.core.network.dto.UsageTrendResponseDto
import com.nivya.data.repository.UsageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentAppUsageUiState(
    val isLoading: Boolean = false,
    val deviceId: Long = 1L,
    val deviceName: String = "Alex's Galaxy A54",
    val summary: UsageSummaryResponseDto? = null,
    val appUsage: AppUsageResponseDto? = null,
    val trends: UsageTrendResponseDto? = null,
    val errorMessage: String? = null
)

class ParentAppUsageViewModel(
    private val usageRepository: UsageRepository,
    private val deviceId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentAppUsageUiState(
            isLoading = true,
            deviceId = deviceId,
            summary = UsageSummaryResponseDto(
                deviceId = deviceId,
                deviceUuid = "dev-galaxy-a54",
                deviceName = "Alex's Galaxy A54",
                date = "Today",
                totalForegroundSeconds = 9900L,
                formattedTotalTime = "2h 45m",
                educationalSeconds = 4200L,
                recreationalSeconds = 3600L,
                socialSeconds = 1200L,
                productivitySeconds = 900L,
                screenUnlocks = 14,
                categoryBreakdown = mapOf(
                    "EDUCATION" to 4200L,
                    "GAMES" to 2400L,
                    "ENTERTAINMENT" to 1200L,
                    "SOCIAL" to 1200L,
                    "PRODUCTIVITY" to 900L
                )
            )
        )
    )
    val uiState: StateFlow<ParentAppUsageUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val summaryDeferred = async { usageRepository.getDailySummary(deviceId) }
            val appsDeferred = async { usageRepository.getAppUsage(deviceId) }
            val trendsDeferred = async { usageRepository.getUsageTrends(deviceId) }

            val summaryRes = summaryDeferred.await()
            val appsRes = appsDeferred.await()
            val trendsRes = trendsDeferred.await()

            var newSummary = _uiState.value.summary
            var newApps = _uiState.value.appUsage
            var newTrends = _uiState.value.trends

            if (summaryRes is NetworkResult.Success) {
                newSummary = summaryRes.data
            }
            if (appsRes is NetworkResult.Success) {
                newApps = appsRes.data
            }
            if (trendsRes is NetworkResult.Success) {
                newTrends = trendsRes.data
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                summary = newSummary,
                appUsage = newApps,
                trends = newTrends
            )
        }
    }

    companion object {
        fun provideFactory(
            usageRepository: UsageRepository,
            deviceId: Long = 1L
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentAppUsageViewModel(usageRepository, deviceId) as T
                }
            }
    }
}
