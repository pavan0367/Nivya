package com.nivya.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivya.ui.role.RoleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing device code generation, input validation, and connection requests.
 */
class PairingViewModel(
    private val myRole: RoleType = RoleType.PARENT
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    init {
        generatePairingCode()
    }

    fun generatePairingCode() {
        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading
            try {
                // In production this delegates to PairingRepository.generateCode()
                val targetRole = if (myRole == RoleType.PARENT) RoleType.CHILD else RoleType.PARENT
                // Mock ready state with sample code format until network repository injects
                _uiState.value = PairingUiState.CodeReady(
                    myCode = "NV-9A2F-K4B7",
                    myRole = myRole,
                    targetRole = targetRole,
                    expiresAt = "10 minutes remaining",
                    ttlSeconds = 600
                )
            } catch (e: Exception) {
                _uiState.value = PairingUiState.Error(e.message ?: "Failed to generate pairing code")
            }
        }
    }

    fun connectWithCode(oppositeCode: String) {
        val cleanCode = oppositeCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            _uiState.value = PairingUiState.Error("Please enter the opposite device's pairing code")
            return
        }

        viewModelScope.launch {
            _uiState.value = PairingUiState.Connecting
            try {
                // In production this delegates to PairingRepository.connect(cleanCode)
                _uiState.value = PairingUiState.Connected(
                    familyCode = "FAM-NIVYA-01",
                    memberCount = 2
                )
            } catch (e: Exception) {
                _uiState.value = PairingUiState.Error(e.message ?: "Failed to connect devices")
            }
        }
    }

    fun resetError() {
        if (_uiState.value is PairingUiState.Error) {
            generatePairingCode()
        }
    }
}
