package com.nivya.ui.role

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Architecture ViewModel managing role selection state and submission.
 */
class RoleSelectionViewModel {

    private val _uiState = MutableStateFlow<RoleUiState>(RoleUiState.Idle)
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun selectRole(role: RoleType) {
        _uiState.value = RoleUiState.Selected(role)
    }

    fun confirmRole(role: RoleType) {
        _uiState.value = RoleUiState.Submitting
        // Emits Success leading to the Connection Screen
        _uiState.value = RoleUiState.Success(
            role = role,
            nextDestination = "connection_screen/${role.name}"
        )
    }

    fun resetState() {
        _uiState.value = RoleUiState.Idle
    }
}
