package com.nivya.ui.convocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.ConvocationActionEventDto
import com.nivya.core.network.dto.ConvocationSeenEventDto
import com.nivya.core.network.dto.ParentConvocationMessageDto
import com.nivya.data.repository.ConvocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentConvocationUiState(
    val messages: List<ParentConvocationMessageDto> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for Parent Convocation screen.
 * Retains complete conversation history, sent messages, Child-originated messages,
 * and "Seen" delivery status.
 * Coordinates real-time event subscriptions, server-authoritative message ID deduplication,
 * and reconnect backfill reconciliation.
 */
class ParentConvocationViewModel(
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentConvocationUiState())
    val uiState: StateFlow<ParentConvocationUiState> = _uiState.asStateFlow()

    init {
        convocationRepository.startRealtime()
        loadHistory()
        observeRealtimeEvents()
    }

    private fun observeRealtimeEvents() {
        viewModelScope.launch {
            convocationRepository.incomingMessages.collect { msg ->
                _uiState.update { state ->
                    state.copy(messages = mergeAndDeduplicate(state.messages, listOf(msg)))
                }
            }
        }

        viewModelScope.launch {
            convocationRepository.seenEvents.collect { seen ->
                handleSeenEvent(seen)
            }
        }

        viewModelScope.launch {
            convocationRepository.convocationActions.collect { action ->
                handleActionEvent(action)
            }
        }

        viewModelScope.launch {
            convocationRepository.reconnectEvents.collect {
                backfillHistory()
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.messages.isEmpty(), errorMessage = null) }
            convocationRepository.parentGetHistory()
                .onSuccess { list ->
                    _uiState.update { state ->
                        state.copy(
                            messages = mergeAndDeduplicate(state.messages, list),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load convocation history"
                        )
                    }
                }
        }
    }

    fun backfillHistory() {
        viewModelScope.launch {
            convocationRepository.parentGetHistory()
                .onSuccess { list ->
                    _uiState.update { state ->
                        state.copy(messages = mergeAndDeduplicate(state.messages, list))
                    }
                }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputMessage = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputMessage.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            convocationRepository.parentSendMessage(text)
                .onSuccess { newMsg ->
                    _uiState.update { state ->
                        state.copy(
                            messages = mergeAndDeduplicate(state.messages, listOf(newMsg)),
                            inputMessage = "",
                            isSending = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSending = false,
                            errorMessage = error.message ?: "Failed to send message"
                        )
                    }
                }
        }
    }

    fun unsendMessage(messageId: Long) {
        viewModelScope.launch {
            convocationRepository.parentUnsendMessage(messageId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != messageId })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to unsend message") }
                }
        }
    }

    fun togglePinMessage(messageId: Long) {
        viewModelScope.launch {
            convocationRepository.parentTogglePinMessage(messageId)
                .onSuccess { updatedMsg ->
                    _uiState.update { state ->
                        state.copy(messages = mergeAndDeduplicate(state.messages, listOf(updatedMsg)))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to pin message") }
                }
        }
    }

    private fun handleSeenEvent(event: ConvocationSeenEventDto) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == event.messageId) {
                    msg.copy(seen = true, seenAt = event.seenAt ?: msg.seenAt)
                } else {
                    msg
                }
            }
            state.copy(messages = updated)
        }
    }

    private fun handleActionEvent(action: ConvocationActionEventDto) {
        _uiState.update { state ->
            val updated = when (action.action) {
                "UNSEND" -> state.messages.filter { it.id != action.messageId }
                "PIN_TOGGLE" -> state.messages.map { msg ->
                    if (msg.id == action.messageId) msg.copy(pinned = action.isPinned ?: !msg.pinned) else msg
                }
                "REACTION" -> state.messages.map { msg ->
                    if (msg.id == action.messageId) msg.copy(reaction = action.reaction) else msg
                }
                else -> state.messages
            }
            state.copy(messages = updated)
        }
    }

    /**
     * Authoritative message ID deduplication and chronological ordering.
     */
    private fun mergeAndDeduplicate(
        current: List<ParentConvocationMessageDto>,
        incoming: List<ParentConvocationMessageDto>
    ): List<ParentConvocationMessageDto> {
        val map = LinkedHashMap<Long, ParentConvocationMessageDto>()
        for (msg in current) {
            map[msg.id] = msg
        }
        for (msg in incoming) {
            val existing = map[msg.id]
            if (existing != null) {
                map[msg.id] = existing.copy(
                    message = if (msg.message.isNotBlank()) msg.message else existing.message,
                    senderName = msg.senderName ?: existing.senderName ?: "",
                    seen = msg.seen || existing.seen,
                    seenAt = msg.seenAt ?: existing.seenAt,
                    pinned = msg.pinned || existing.pinned,
                    reaction = msg.reaction ?: existing.reaction,
                    status = msg.status ?: existing.status
                )
            } else {
                map[msg.id] = msg
            }
        }
        return map.values.sortedWith(
            compareBy<ParentConvocationMessageDto> { it.createdAt ?: "" }
                .thenBy { it.id }
        )
    }

    companion object {
        fun provideFactory(
            convocationRepository: ConvocationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentConvocationViewModel(convocationRepository) as T
            }
        }
    }
}
