package com.nivya.ui.pairing

import com.nivya.ui.role.RoleType

/**
 * UI State for the Device Pairing Screen.
 */
sealed interface PairingUiState {
    object Idle : PairingUiState
    object Loading : PairingUiState
    data class CodeReady(
        val myCode: String,
        val myRole: RoleType,
        val targetRole: RoleType,
        val expiresAt: String,
        val ttlSeconds: Long
    ) : PairingUiState
    object Connecting : PairingUiState
    data class Connected(
        val familyCode: String,
        val memberCount: Int
    ) : PairingUiState
    data class Error(val message: String) : PairingUiState
}
