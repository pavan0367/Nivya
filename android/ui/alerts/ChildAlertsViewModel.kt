package com.nivya.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.AlertDto
import com.nivya.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChildAlertsUiState(
    val alerts: List<AlertDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Child-specific alerts ViewModel:
 * Restricted exclusively to alerts intended for the child device.
 * Exposes NO parent-only controls or sensitive resolution buttons.
 */
class ChildAlertsViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildAlertsUiState())
    val uiState: StateFlow<ChildAlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            alertRepository.getChildAlerts().collect { alerts ->
                _uiState.update { it.copy(alerts = alerts, isLoading = false) }
            }
        }
    }

    fun markAsRead(alertId: Long) {
        viewModelScope.launch {
            alertRepository.markAsRead(alertId).onSuccess { updated ->
                _uiState.update { state ->
                    val newAlerts = state.alerts.map { if (it.id == updated.id) updated else it }
                    state.copy(alerts = newAlerts)
                }
            }
        }
    }

    companion object {
        fun provideFactory(alertRepository: AlertRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildAlertsViewModel(alertRepository) as T
                }
            }
    }
}
