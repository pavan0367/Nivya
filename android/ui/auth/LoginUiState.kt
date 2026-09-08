package com.nivya.ui.auth

/**
 * UI State for the Login / Authentication screen.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isRegisterMode: Boolean = false,
    val selectedRegisterRole: String = "PARENT",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userRole: String? = null
)
