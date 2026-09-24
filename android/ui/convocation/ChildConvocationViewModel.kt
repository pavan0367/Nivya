package com.nivya.ui.convocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.ChildConvocationMessageDto
import com.nivya.core.network.dto.ConvocationActionEventDto
import com.nivya.core.network.dto.ParentConvocationMessageDto
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
 * Wires real-time WebSocket events: eligible Parent messages arrive in realtime only while viewing is active.
 * Seen status is strictly omitted from Child view.
 */
class ChildConvocationViewModel(
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildConvocationUiState())
    val uiState: StateFlow<ChildConvocationUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        convocationRepository.startRealtime()
        observeRealtimeEvents()
    }

    private fun observeRealtimeEvents() {
        viewModelScope.launch {
            convocationRepository.incomingMessages.collect { msg ->
                handleIncomingMessage(msg)
            }
        }

        viewModelScope.launch {
            convocationRepository.convocationActions.collect { action ->
                handleActionEvent(action)
            }
        }

        viewModelScope.launch {
            convocationRepository.reconnectEvents.collect {
                handleReconnect()
            }
        }
    }

    private fun handleIncomingMessage(msg: ParentConvocationMessageDto) {
        // Child visibility rules:
        // - Default OFF + EMPTY: strictly do not display Parent messages when viewing state is OFF
        // - Only eligible unread Parent messages shown (msg.childOriginated == false)
        // - Viewed old Parent messages must not reappear: new message becomes visible unread set
        if (!_uiState.value.isViewingActive || msg.childOriginated) return

        val childMsg = ChildConvocationMessageDto(
            id = msg.id,
            message = msg.message,
            createdAt = msg.createdAt ?: ""
        )

        _uiState.update { state ->
            val exists = state.activeMessages.any { it.id == childMsg.id }
            if (exists) {
                state
            } else {
                state.copy(activeMessages = listOf(childMsg))
            }
        }
    }

    private fun handleActionEvent(action: ConvocationActionEventDto) {
        if (action.action == "UNSEND") {
            _uiState.update { state ->
                state.copy(activeMessages = state.activeMessages.filter { it.id != action.messageId })
            }
        }
    }

    private fun handleReconnect() {
        if (_uiState.value.isConvocationEnabled) {
            viewModelScope.launch {
                convocationRepository.childGetVisibility()
                    .onSuccess { visibility ->
                        if (!visibility.viewingActive) {
                            closeViewingSession()
                        } else {
                            _uiState.update { it.copy(remainingSeconds = visibility.remainingSeconds) }
                        }
                    }
            }
        }
    }

    fun updateNoteText(text: String) {
        _uiState.update { it.copy(noteText = text, noteSentFeedback = false, errorMessage = null) }
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
        _uiState.update { it.copy(isLoading = true, isConvocationEnabled = true, errorMessage = null) }

        viewModelScope.launch {
            convocationRepository.childStartViewing()
                .onSuccess { session ->
                    val seconds = session.remainingSeconds
                    _uiState.update {
                        it.copy(
                            isViewingActive = seconds > 0 && session.messages.isNotEmpty(),
                            remainingSeconds = seconds,
                            activeMessages = session.messages,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    if (seconds > 0) {
                        startCountdownTimer(seconds)
                    } else {
                        closeViewingSession()
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isViewingActive = false,
                            isConvocationEnabled = false,
                            remainingSeconds = 0,
                            activeMessages = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to activate viewing session"
                        )
                    }
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
     * Never sets noteSentFeedback = true on failure.
     */
    fun sendChildNote() {
        val text = _uiState.value.noteText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingNote = true, errorMessage = null) }
            convocationRepository.childSendMessage(text)
                .onSuccess {
                    // Message disappears immediately from Child screen on authoritative ack
                    _uiState.update {
                        it.copy(
                            noteText = "",
                            isSendingNote = false,
                            noteSentFeedback = true,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    // Never fake success!
                    _uiState.update {
                        it.copy(
                            isSendingNote = false,
                            noteSentFeedback = false,
                            errorMessage = error.message ?: "Failed to send note"
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
