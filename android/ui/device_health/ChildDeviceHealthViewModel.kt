package com.nivya.ui.device_health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.DeviceHealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChildDeviceHealthUiState(
    val isLoading: Boolean = false,
    val conditionSummary: String = "Your phone is running great! 🎉",
    val healthScore: Int = 96,
    val freeStorageGb: Double = 79.8,
    val storageUsedPct: Double = 37.6,
    val batteryPct: Int = 82,
    val isCharging: Boolean = false,
    val allPermissionsHealthy: Boolean = true,
    val errorMessage: String? = null
)

class ChildDeviceHealthViewModel(
    private val deviceHealthRepository: DeviceHealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildDeviceHealthUiState(isLoading = true))
    val uiState: StateFlow<ChildDeviceHealthUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // First check local cached entity from Room
            val cached = deviceHealthRepository.getLatestCached()
            if (cached != null) {
                val freeGb = cached.storageFreeBytes / (1024.0 * 1024.0 * 1024.0)
                val usedPct = if (cached.storageTotalBytes > 0) {
                    (cached.storageUsedBytes.toDouble() / cached.storageTotalBytes.toDouble()) * 100.0
                } else 35.0

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    freeStorageGb = freeGb,
                    storageUsedPct = usedPct,
                    batteryPct = cached.batteryPct,
                    isCharging = cached.chargingState.contains("CHARGING", ignoreCase = true),
                    allPermissionsHealthy = cached.allPermissionsHealthy
                )
            }

            // Next attempt fresh backend fetch for child view
            when (val res = deviceHealthRepository.getMyDeviceHealth()) {
                is NetworkResult.Success -> {
                    val data = res.data
                    val freeGb = (data.storage.freeBytes) / (1024.0 * 1024.0 * 1024.0)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conditionSummary = data.conditionSummary ?: "Your phone is running great! 🎉",
                        healthScore = data.healthScore,
                        freeStorageGb = freeGb,
                        storageUsedPct = data.storage.usedPct,
                        batteryPct = data.batteryPct ?: 80,
                        isCharging = data.chargingState?.contains("CHARGING", ignoreCase = true) == true,
                        allPermissionsHealthy = data.permissionHealth.allHealthy,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    // Fallback to local snapshot, don't show intrusive error if cached data exists
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (cached == null) res.message else null
                    )
                }
                is NetworkResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (cached == null) (res.throwable.localizedMessage ?: "Failed to connect to server") else null
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            deviceHealthRepository: DeviceHealthRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildDeviceHealthViewModel(deviceHealthRepository) as T
                }
            }
    }
}
