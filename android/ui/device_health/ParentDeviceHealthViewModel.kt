package com.nivya.ui.device_health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.DeviceHealthResponseDto
import com.nivya.core.network.dto.MemoryHealthDto
import com.nivya.core.network.dto.PermissionHealthDto
import com.nivya.core.network.dto.StorageHealthDto
import com.nivya.data.repository.DeviceHealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentDeviceHealthUiState(
    val isLoading: Boolean = false,
    val deviceId: Long = 1L,
    val healthData: DeviceHealthResponseDto? = null,
    val errorMessage: String? = null
)

class ParentDeviceHealthViewModel(
    private val deviceHealthRepository: DeviceHealthRepository,
    private val deviceId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentDeviceHealthUiState(
            isLoading = true,
            deviceId = deviceId,
            healthData = createInitialHealthSnapshot(deviceId)
        )
    )
    val uiState: StateFlow<ParentDeviceHealthUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = deviceHealthRepository.getDeviceHealth(deviceId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        healthData = result.data,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.throwable.localizedMessage ?: "Failed to connect to server"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            deviceHealthRepository: DeviceHealthRepository,
            deviceId: Long = 1L
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentDeviceHealthViewModel(deviceHealthRepository, deviceId) as T
                }
            }

        private fun createInitialHealthSnapshot(deviceId: Long): DeviceHealthResponseDto {
            return DeviceHealthResponseDto(
                deviceId = deviceId,
                deviceUuid = "dev-galaxy-a54",
                deviceName = "Alex's Galaxy A54",
                deviceModel = "Samsung Galaxy A54 5G",
                deviceManufacturer = "Samsung",
                osVersion = "Android 14 (One UI 6)",
                sdkVersion = 34,
                storage = StorageHealthDto(
                    totalBytes = 128_000_000_000L,
                    usedBytes = 48_200_000_000L,
                    freeBytes = 79_800_000_000L,
                    usedPct = 37.6,
                    isLowStorage = false
                ),
                memory = MemoryHealthDto(
                    totalBytes = 8_000_000_000L,
                    usedBytes = 4_100_000_000L,
                    freeBytes = 3_900_000_000L,
                    usedPct = 51.2,
                    isLowRam = false
                ),
                batteryPct = 82,
                chargingState = "NOT_CHARGING",
                batteryHealth = "GOOD",
                batteryTempCelsius = 31.5,
                networkType = "WIFI",
                isOnline = true,
                syncState = "SYNCED",
                permissionHealth = PermissionHealthDto(
                    locationPermission = "GRANTED",
                    usagePermission = "GRANTED",
                    notificationPermission = "GRANTED",
                    batteryOptimization = "OPTIMIZED",
                    allHealthy = true
                ),
                healthScore = 96,
                healthStatus = "EXCELLENT",
                conditionSummary = "Your child's device is running in optimal condition.",
                storageSummary = "79.8 GB available storage (37.6% used).",
                batterySummary = "Battery is healthy at 82%, normal temperature.",
                protectionSummary = "All essential Nivya permissions are active and healthy.",
                recordedAt = "Just now",
                updatedAt = "Just now"
            )
        }
    }
}
