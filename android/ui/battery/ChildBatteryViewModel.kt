package com.nivya.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.data.repository.BatteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChildBatteryUiState(
    val percentage: Int = 100,
    val chargingState: String = "NOT_CHARGING",
    val batteryState: String = "OK",
    val health: String = "GOOD",
    val temperatureCelsius: Double? = 28.0,
    val recordedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChildBatteryViewModel(
    private val batteryRepository: BatteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildBatteryUiState(isLoading = true))
    val uiState: StateFlow<ChildBatteryUiState> = _uiState.asStateFlow()

    init {
        // 1. Observe Room database flow for immediate updates
        viewModelScope.launch {
            batteryRepository.getLatestBatteryFlow().collectLatest { entity ->
                if (entity != null) {
                    _uiState.value = _uiState.value.copy(
                        percentage = entity.batteryPct,
                        chargingState = entity.chargingState,
                        batteryState = entity.batteryState,
                        health = entity.health,
                        temperatureCelsius = entity.temperatureCelsius,
                        recordedAt = entity.recordedAt,
                        isSynced = entity.isSynced,
                        isLoading = false
                    )
                }
            }
        }

        // 2. Perform immediate snapshot collection & backend push
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val snapshot = batteryRepository.collectAndRecordBattery()
                _uiState.value = _uiState.value.copy(
                    percentage = snapshot.percentage,
                    chargingState = snapshot.chargingState,
                    batteryState = snapshot.batteryState,
                    health = snapshot.health,
                    temperatureCelsius = snapshot.temperatureCelsius,
                    recordedAt = snapshot.timestamp,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to refresh battery info"
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            batteryRepository: BatteryRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildBatteryViewModel(batteryRepository) as T
                }
            }
    }
}
