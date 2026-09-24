package com.nivya.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.LocationHistoryResponseDto
import com.nivya.core.network.dto.LocationPointDto
import com.nivya.core.network.dto.LocationStatusResponseDto
import com.nivya.data.repository.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentLocationUiState(
    val isLoading: Boolean = false,
    val deviceId: Long = 1L,
    val deviceName: String = "",
    val currentLocation: LocationStatusResponseDto? = null,
    val historyPoints: List<LocationPointDto> = emptyList(),
    val isConsentGranted: Boolean = true,
    val consentMessage: String? = null,
    val errorMessage: String? = null
)

class ParentLocationViewModel(
    private val locationRepository: LocationRepository,
    private val deviceId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentLocationUiState(
            isLoading = true,
            deviceId = deviceId,
            currentLocation = null
        )
    )
    val uiState: StateFlow<ParentLocationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentDeferred = async { locationRepository.getCurrentLocation(deviceId) }
            val historyDeferred = async { locationRepository.getLocationHistory(deviceId) }

            val currentRes = currentDeferred.await()
            val historyRes = historyDeferred.await()

            var newLocation = _uiState.value.currentLocation
            var newHistory = _uiState.value.historyPoints
            var consentGranted = _uiState.value.isConsentGranted
            var consentMsg = _uiState.value.consentMessage

            if (currentRes is NetworkResult.Success) {
                newLocation = currentRes.data
            }
            if (historyRes is NetworkResult.Success) {
                newHistory = historyRes.data.points
                consentGranted = historyRes.data.consentGranted
                consentMsg = historyRes.data.message
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentLocation = newLocation,
                historyPoints = newHistory,
                isConsentGranted = consentGranted,
                consentMessage = consentMsg
            )
        }
    }

    companion object {
        fun provideFactory(
            locationRepository: LocationRepository,
            deviceId: Long = 1L
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentLocationViewModel(locationRepository, deviceId) as T
                }
            }
    }
}
