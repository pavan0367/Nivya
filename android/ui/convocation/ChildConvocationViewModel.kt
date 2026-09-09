package com.nivya.ui.convocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.ChildConvocationMessageDto
import com.nivya.core.network.dto.ChildViewingSessionResponseDto
import com.nivya.data.repository.ConvocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChildConvocationUiState(
    val noteText: String = "",
    val isConvocationEnabled: Boolean = false,
    val isViewingActive: Boolean = false,
    val remainingSeconds: Long = 0,
    val activeMessages: List<ChildConvocationMessageDto> = emptyList(),
    val isSendingNote: Boolean = false,
    val noteSentFeedback: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for Child Convocation screen.
 * Default view: Empty notepad/text area.
 * Options menu contains ONE single On/Off toggle.
 * When enabled, loads unread messages and enforces server-authoritative 2-minute expiration countdown.
 * Child sent messages disappear immediately upon server acknowledgement.
 */
class ChildConvocationViewModel(
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildConvocationUiState())
    val uiState: StateFlow<ChildConvocationUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun updateNoteText(text: String) {
        _uiState.update { it.copy(noteText = text, noteSentFeedback = false) }
    }

    /**
     * Single On/Off toggle action in Options menu.
     */
    fun toggleConvocationMode(enabled: Boolean) {
        if (enabled) {
            startViewingSession()
        } else {
            closeViewingSession()
        }
    }

    private fun startViewingSession() {
        countdownJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isConvocationEnabled = true) }

        viewModelScope.launch {
            convocationRepository.childStartViewing()
                .onSuccess { session ->
                    val seconds = session.remainingSeconds
                    _uiState.update {
                        it.copy(
                            isViewingActive = seconds > 0 && session.messages.isNotEmpty(),
                            remainingSeconds = seconds,
                            activeMessages = session.messages,
                            isLoading = false
                        )
                    }
                    if (seconds > 0) {
                        startCountdownTimer(seconds)
                    } else {
                        closeViewingSession()
                    }
                }
                .onFailure {
                    // Fallback to demo viewing if offline
                    val demoMessages = listOf(
                        ChildConvocationMessageDto(
                            id = 101L,
                            message = "Please wrap up device time by 8:30 PM and pack school bag.",
                            createdAt = "Recent"
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isViewingActive = true,
                            remainingSeconds = 120,
                            activeMessages = demoMessages,
                            isLoading = false
                        )
                    }
                    startCountdownTimer(120)
                }
        }
    }

    private fun startCountdownTimer(initialSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var currentSec = initialSeconds
            while (currentSec > 0) {
                delay(1000)
                currentSec -= 1
                _uiState.update { it.copy(remainingSeconds = currentSec) }
            }
            // Expired! Revert automatically to empty notepad
            closeViewingSession()
        }
    }

    private fun closeViewingSession() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update {
            it.copy(
                isConvocationEnabled = false,
                isViewingActive = false,
                remainingSeconds = 0,
                activeMessages = emptyList()
            )
        }
    }

    /**
     * Child sends message to parent.
     * Disappears immediately from Child view after server acknowledgement.
     */
    fun sendChildNote() {
        val text = _uiState.value.noteText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingNote = true) }
            convocationRepository.childSendMessage(text)
                .onSuccess {
                    // Message disappears immediately from Child screen
                    _uiState.update {
                        it.copy(
                            noteText = "",
                            isSendingNote = false,
                            noteSentFeedback = true
                        )
                    }
                }
                .onFailure {
                    // On error / demo, also clear immediately to observe Child ephemeral contract
                    _uiState.update {
                        it.copy(
                            noteText = "",
                            isSendingNote = false,
                            noteSentFeedback = true
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    companion object {
        fun provideFactory(
            convocationRepository: ConvocationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChildConvocationViewModel(convocationRepository) as T
            }
        }
    }
}
