package com.nivya.ui.role

/**
 * UI State for the Role Selection Screen.
 */
sealed interface RoleUiState {
    object Idle : RoleUiState
    data class Selected(val role: RoleType) : RoleUiState
    object Submitting : RoleUiState
    data class Success(val role: RoleType, val nextDestination: String) : RoleUiState
    data class Error(val message: String) : RoleUiState
}

enum class RoleType {
    PARENT,
    CHILD
}
