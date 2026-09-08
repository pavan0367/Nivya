package com.nivya.ui.device_health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.BatteryDataPointDto
import com.nivya.core.network.dto.BatteryStatusResponseDto
import com.nivya.core.network.dto.BatteryTrendResponseDto
import com.nivya.data.repository.BatteryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentDeviceHealthUiState(
    val isLoading: Boolean = false,
    val deviceId: Long = 1L,
    val deviceName: String = "Alex's Galaxy A54",
    val batteryStatus: BatteryStatusResponseDto? = null,
    val trends: BatteryTrendResponseDto? = null,
    val historyPoints: List<BatteryDataPointDto> = emptyList(),
    val errorMessage: String? = null
)

class ParentDeviceHealthViewModel(
    private val batteryRepository: BatteryRepository,
    private val deviceId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentDeviceHealthUiState(
            isLoading = true,
            deviceId = deviceId,
            batteryStatus = BatteryStatusResponseDto(
                deviceId = deviceId,
                deviceUuid = "dev-galaxy-a54",
                deviceName = "Alex's Galaxy A54",
                batteryPct = 78,
                chargingState = "NOT_CHARGING",
                batteryState = "UNPLUGGED",
                health = "GOOD",
                temperatureCelsius = 31.0,
                isLowBattery = false,
                updatedAt = "2026-09-08T11:00:00Z"
            ),
            trends = BatteryTrendResponseDto(
                deviceId = deviceId,
                currentBatteryPct = 78,
                chargingState = "NOT_CHARGING",
                drainRatePctPerHour = 5.2,
                estimatedHoursRemaining = 15.0,
                averageTemperatureCelsius = 30.5
            )
        )
    )
    val uiState: StateFlow<ParentDeviceHealthUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentDeferred = async { batteryRepository.getCurrentBattery(deviceId) }
            val trendsDeferred = async { batteryRepository.getBatteryTrends(deviceId) }
            val historyDeferred = async { batteryRepository.getBatteryHistory(deviceId) }

            val currentResult = currentDeferred.await()
            val trendsResult = trendsDeferred.await()
            val historyResult = historyDeferred.await()

            var newStatus = _uiState.value.batteryStatus
            var newTrends = _uiState.value.trends
            var newHistory = _uiState.value.historyPoints

            if (currentResult is NetworkResult.Success) {
                newStatus = currentResult.data
            }
            if (trendsResult is NetworkResult.Success) {
                newTrends = trendsResult.data
            }
            if (historyResult is NetworkResult.Success) {
                newHistory = historyResult.data.points
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                batteryStatus = newStatus,
                trends = newTrends,
                historyPoints = newHistory
            )
        }
    }

    companion object {
        fun provideFactory(
            batteryRepository: BatteryRepository,
            deviceId: Long = 1L
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentDeviceHealthViewModel(batteryRepository, deviceId) as T
                }
            }
    }
}
