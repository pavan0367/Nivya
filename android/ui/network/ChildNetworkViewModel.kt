package com.nivya.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.data.repository.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChildNetworkUiState(
    val networkType: String = "WIFI",
    val connectionType: String = "Wi-Fi",
    val isNetworkAvailable: Boolean = true,
    val isInternetAvailable: Boolean = true,
    val signalLevel: Int? = 4,
    val signalDbm: Int? = -58,
    val quality: String = "EXCELLENT", // EXCELLENT, GOOD, WEAK, UNAVAILABLE
    val isSynced: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChildNetworkViewModel(
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildNetworkUiState(isLoading = true))
    val uiState: StateFlow<ChildNetworkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkRepository.getLatestNetworkFlow().collectLatest { entity ->
                if (entity != null) {
                    _uiState.value = _uiState.value.copy(
                        networkType = entity.networkType,
                        connectionType = entity.connectionType ?: entity.networkType,
                        isNetworkAvailable = entity.isNetworkAvailable,
                        isInternetAvailable = entity.isInternetAvailable,
                        signalLevel = entity.signalLevel,
                        signalDbm = entity.signalDbm,
                        quality = entity.quality,
                        isSynced = entity.isSynced,
                        isLoading = false
                    )
                }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val snapshot = networkRepository.collectAndRecordNetwork()
                _uiState.value = _uiState.value.copy(
                    networkType = snapshot.networkType,
                    connectionType = snapshot.connectionType,
                    isNetworkAvailable = snapshot.isNetworkAvailable,
                    isInternetAvailable = snapshot.isInternetAvailable,
                    signalLevel = snapshot.signalLevel,
                    signalDbm = snapshot.signalDbm,
                    quality = snapshot.quality,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to refresh network state"
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            networkRepository: NetworkRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildNetworkViewModel(networkRepository) as T
                }
            }
    }
}
