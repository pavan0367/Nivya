package com.nivya.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmailVerificationUiState(
    val email: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val resendCooldown: Int = 0
) {
    val canSubmit: Boolean get() = otpCode.length == 6 && !isLoading
    val canResend: Boolean get() = resendCooldown <= 0 && !isResending
}

/**
 * ViewModel managing email verification lifecycle and authoritative backend confirmation.
 */
class EmailVerificationViewModel(
    initialEmail: String,
    private val authRepository: AuthRepository,
    initialCooldownSeconds: Int = 60
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EmailVerificationUiState(
            email = initialEmail,
            resendCooldown = initialCooldownSeconds
        )
    )
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        if (initialCooldownSeconds > 0) {
            startCooldown(initialCooldownSeconds)
        }
    }

    public override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }

    fun onOtpChanged(code: String) {
        // Enforce 6-digit numeric input only
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = filtered, errorMessage = null) }
        if (filtered.length == 6) {
            verifyCode()
        }
    }

    fun verifyCode() {
        val state = _uiState.value
        if (state.otpCode.length != 6 || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.confirmVerificationCode(
                email = state.email,
                code = state.otpCode
            )

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isVerified = true,
                            infoMessage = "Email verified successfully! You can now sign in."
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exception.localizedMessage ?: "Verification failed. Check network connection."
                        )
                    }
                }
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (!state.canResend) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.sendVerificationCode(state.email)

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            infoMessage = result.data
                        )
                    }
                    startCooldown(60)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            errorMessage = result.exception.localizedMessage ?: "Failed to resend code"
                        )
                    }
                }
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(resendCooldown = seconds) }
        if (seconds <= 0) return
        cooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update { it.copy(resendCooldown = remaining) }
                delay(1000L)
            }
            _uiState.update { it.copy(resendCooldown = 0) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            email: String,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EmailVerificationViewModel(email, authRepository) as T
            }
        }
    }
}
