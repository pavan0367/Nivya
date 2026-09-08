package com.nivya.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.PairingRepository
import com.nivya.ui.role.RoleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing device code generation, input validation, and connection requests.
 */
class PairingViewModel(
    private val myRole: RoleType = RoleType.PARENT,
    private val pairingRepository: PairingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    init {
        generatePairingCode()
    }

    fun generatePairingCode() {
        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading
            val targetRole = if (myRole == RoleType.PARENT) RoleType.CHILD else RoleType.PARENT

            if (pairingRepository != null) {
                when (val result = pairingRepository.generatePairingCode()) {
                    is NetworkResult.Success -> {
                        _uiState.value = PairingUiState.CodeReady(
                            myCode = result.data.code,
                            myRole = myRole,
                            targetRole = targetRole,
                            expiresAt = "${result.data.ttlSeconds / 60} minutes remaining",
                            ttlSeconds = result.data.ttlSeconds
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = PairingUiState.Error(result.message)
                    }
                    is NetworkResult.Exception -> {
                        _uiState.value = PairingUiState.Error(
                            result.exception.localizedMessage ?: "Failed to generate pairing code"
                        )
                    }
                }
            } else {
                // Fallback mock code for preview / isolated tests
                _uiState.value = PairingUiState.CodeReady(
                    myCode = "NV-9A2F-K4B7",
                    myRole = myRole,
                    targetRole = targetRole,
                    expiresAt = "10 minutes remaining",
                    ttlSeconds = 600
                )
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

            if (pairingRepository != null) {
                when (val result = pairingRepository.connect(cleanCode)) {
                    is NetworkResult.Success -> {
                        _uiState.value = PairingUiState.Connected(
                            familyCode = result.data.familyCode ?: "FAM-CONNECTED",
                            memberCount = result.data.members.size.coerceAtLeast(1)
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = PairingUiState.Error(result.message)
                    }
                    is NetworkResult.Exception -> {
                        _uiState.value = PairingUiState.Error(
                            result.exception.localizedMessage ?: "Failed to connect devices"
                        )
                    }
                }
            } else {
                _uiState.value = PairingUiState.Connected(
                    familyCode = "FAM-NIVYA-01",
                    memberCount = 2
                )
            }
        }
    }

    fun resetError() {
        if (_uiState.value is PairingUiState.Error) {
            generatePairingCode()
        }
    }

    companion object {
        fun provideFactory(
            myRole: RoleType,
            pairingRepository: PairingRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PairingViewModel(myRole, pairingRepository) as T
                }
            }
    }
}
