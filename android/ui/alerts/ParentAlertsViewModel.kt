package com.nivya.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.AlertDto
import com.nivya.core.network.dto.AlertRuleDto
import com.nivya.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlertFilter {
    ALL,
    UNREAD,
    CRITICAL,
    SECURITY
}

data class ParentAlertsUiState(
    val alerts: List<AlertDto> = emptyList(),
    val rules: List<AlertRuleDto> = emptyList(),
    val filter: AlertFilter = AlertFilter.ALL,
    val unreadCount: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ParentAlertsViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentAlertsUiState())
    val uiState: StateFlow<ParentAlertsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(familyId: Long = 1L) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Fetch unread count
            alertRepository.getUnreadCount().onSuccess { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }

            // 2. Fetch alert rules
            alertRepository.getAlertRules(familyId).onSuccess { rules ->
                _uiState.update { it.copy(rules = rules) }
            }

            // 3. Fetch alerts stream
            val unreadOnly = if (_uiState.value.filter == AlertFilter.UNREAD) true else null
            val severity = if (_uiState.value.filter == AlertFilter.CRITICAL) "CRITICAL" else null

            alertRepository.getFamilyAlerts(familyId, unreadOnly, severity).collect { alerts ->
                val filtered = when (_uiState.value.filter) {
                    AlertFilter.SECURITY -> alerts.filter { "SECURITY_ALERT".equals(it.alertType, ignoreCase = true) }
                    AlertFilter.UNREAD -> alerts.filter { !it.isRead }
                    AlertFilter.CRITICAL -> alerts.filter { "CRITICAL".equals(it.severity, ignoreCase = true) }
                    AlertFilter.ALL -> alerts
                }
                _uiState.update { it.copy(alerts = filtered, isLoading = false) }
            }
        }
    }

    fun setFilter(filter: AlertFilter, familyId: Long = 1L) {
        _uiState.update { it.copy(filter = filter) }
        loadData(familyId)
    }

    fun markAsRead(alertId: Long) {
        viewModelScope.launch {
            alertRepository.markAsRead(alertId).onSuccess { updated ->
                _uiState.update { state ->
                    val newAlerts = state.alerts.map { if (it.id == updated.id) updated else it }
                    state.copy(
                        alerts = newAlerts,
                        unreadCount = maxOf(0L, state.unreadCount - 1)
                    )
                }
            }
        }
    }

    fun resolveAlert(alertId: Long) {
        viewModelScope.launch {
            alertRepository.resolveAlert(alertId).onSuccess { updated ->
                _uiState.update { state ->
                    val newAlerts = state.alerts.map { if (it.id == updated.id) updated else it }
                    state.copy(alerts = newAlerts)
                }
            }
        }
    }

    fun toggleRule(rule: AlertRuleDto) {
        viewModelScope.launch {
            alertRepository.updateAlertRule(
                ruleId = rule.id,
                threshold = rule.thresholdValue,
                severity = rule.severity,
                enabled = !rule.enabled
            ).onSuccess { updated ->
                _uiState.update { state ->
                    val newRules = state.rules.map { if (it.id == updated.id) updated else it }
                    state.copy(rules = newRules)
                }
            }
        }
    }

    companion object {
        fun provideFactory(alertRepository: AlertRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ParentAlertsViewModel(alertRepository) as T
                }
            }
    }
}
