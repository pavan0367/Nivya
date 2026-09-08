package com.nivya.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.NetworkPointDto
import com.nivya.core.network.dto.NetworkStatusResponseDto
import com.nivya.data.repository.NetworkRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentNetworkUiState(
    val isLoading: Boolean = false,
    val deviceId: Long = 1L,
    val deviceName: String = "Alex's Galaxy A54",
    val networkStatus: NetworkStatusResponseDto? = null,
    val historyPoints: List<NetworkPointDto> = emptyList(),
    val errorMessage: String? = null
)

class ParentNetworkViewModel(
    private val networkRepository: NetworkRepository,
    private val deviceId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentNetworkUiState(
            isLoading = true,
            deviceId = deviceId,
            networkStatus = NetworkStatusResponseDto(
                deviceId = deviceId,
                deviceUuid = "dev-galaxy-a54",
                deviceName = "Alex's Galaxy A54",
                networkType = "WIFI",
                connectionType = "Wi-Fi (5GHz)",
                isNetworkAvailable = true,
                isInternetAvailable = true,
                signalLevel = 4,
                signalDbm = -54,
                quality = "EXCELLENT",
                ssid = "Home-Wi-Fi-5G",
                ipAddress = "192.168.1.105",
                lastSyncAt = "Just now"
            )
        )
    )
    val uiState: StateFlow<ParentNetworkUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentDeferred = async { networkRepository.getCurrentNetwork(deviceId) }
            val historyDeferred = async { networkRepository.getNetworkHistory(deviceId) }

            val currentResult = currentDeferred.await()
            val historyResult = historyDeferred.await()

            var newStatus = _uiState.value.networkStatus
            var newHistory = _uiState.value.historyPoints

            if (currentResult is NetworkResult.Success) {
                newStatus = currentResult.data
            }
            if (historyResult is NetworkResult.Success) {
                newHistory = historyResult.data.points
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                networkStatus = newStatus,
                historyPoints = newHistory
            )
        }
    }

    companion object {
        fun provideFactory(
            networkRepository: NetworkRepository,
            deviceId: Long = 1L
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentNetworkViewModel(networkRepository, deviceId) as T
                }
            }
    }
}
