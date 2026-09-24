package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.data.repository.ConvocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChildDashboardUiState(
    val isCrackInFlight: Boolean = false,
    val isFreakInFlight: Boolean = false,
    val showCrackSending: Boolean = false,
    val showFreakSending: Boolean = false,
    val showDoneToast: Boolean = false,
    val errorMessage: String? = null
)

class ChildDashboardViewModel(
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildDashboardUiState())
    val uiState: StateFlow<ChildDashboardUiState> = _uiState.asStateFlow()

    private var crackGraceJob: Job? = null
    private var freakGraceJob: Job? = null
    private var doneToastJob: Job? = null

    fun triggerCrack() {
        // Duplicate tap protection while CRACK request is in flight
        if (_uiState.value.isCrackInFlight) return

        _uiState.update {
            it.copy(
                isCrackInFlight = true,
                showCrackSending = false,
                errorMessage = null
            )
        }

        // 400ms grace period: only show "Sending..." if still pending after 400ms
        crackGraceJob = viewModelScope.launch {
            delay(400)
            _uiState.update {
                if (it.isCrackInFlight) it.copy(showCrackSending = true) else it
            }
        }

        // Immediate backend dispatch of exact internal message "Mom,here"
        viewModelScope.launch {
            try {
                val result = convocationRepository.childSendMessage("Mom,here")
                crackGraceJob?.cancel()
                _uiState.update {
                    it.copy(
                        isCrackInFlight = false,
                        showCrackSending = false
                    )
                }

                result.onSuccess {
                    showDoneFeedback()
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            errorMessage = err.message ?: "Failed to send message. Please check connection."
                        )
                    }
                }
            } catch (e: Exception) {
                crackGraceJob?.cancel()
                _uiState.update {
                    it.copy(
                        isCrackInFlight = false,
                        showCrackSending = false,
                        errorMessage = e.message ?: "Failed to send message. Please check connection."
                    )
                }
            }
        }
    }

    fun triggerFreak() {
        // Duplicate tap protection while FREAK request is in flight
        if (_uiState.value.isFreakInFlight) return

        _uiState.update {
            it.copy(
                isFreakInFlight = true,
                showFreakSending = false,
                errorMessage = null
            )
        }

        // 400ms grace period: only show "Sending..." if still pending after 400ms
        freakGraceJob = viewModelScope.launch {
            delay(400)
            _uiState.update {
                if (it.isFreakInFlight) it.copy(showFreakSending = true) else it
            }
        }

        // Immediate backend dispatch of exact internal message "Someone's,here"
        viewModelScope.launch {
            try {
                val result = convocationRepository.childSendMessage("Someone's,here")
                freakGraceJob?.cancel()
                _uiState.update {
                    it.copy(
                        isFreakInFlight = false,
                        showFreakSending = false
                    )
                }

                result.onSuccess {
                    showDoneFeedback()
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            errorMessage = err.message ?: "Failed to send message. Please check connection."
                        )
                    }
                }
            } catch (e: Exception) {
                freakGraceJob?.cancel()
                _uiState.update {
                    it.copy(
                        isFreakInFlight = false,
                        showFreakSending = false,
                        errorMessage = e.message ?: "Failed to send message. Please check connection."
                    )
                }
            }
        }
    }

    private fun showDoneFeedback() {
        doneToastJob?.cancel()
        doneToastJob = viewModelScope.launch {
            _uiState.update { it.copy(showDoneToast = true) }
            delay(2000)
            _uiState.update { it.copy(showDoneToast = false) }
        }
    }

    fun dismissDoneToast() {
        doneToastJob?.cancel()
        _uiState.update { it.copy(showDoneToast = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        crackGraceJob?.cancel()
        freakGraceJob?.cancel()
        doneToastJob?.cancel()
    }

    companion object {
        fun provideFactory(
            convocationRepository: ConvocationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChildDashboardViewModel(convocationRepository) as T
            }
        }
    }
}
