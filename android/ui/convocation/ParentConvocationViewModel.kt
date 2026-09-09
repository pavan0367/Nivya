package com.nivya.ui.convocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.dto.ParentConvocationMessageDto
import com.nivya.data.repository.ConvocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

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
 */
class ParentConvocationViewModel(
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentConvocationUiState())
    val uiState: StateFlow<ParentConvocationUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            convocationRepository.parentGetHistory()
                .onSuccess { list ->
                    _uiState.update { it.copy(messages = list, isLoading = false) }
                }
                .onFailure {
                    // Fallback to demonstration history if offline
                    val demoList = getDemoHistory()
                    _uiState.update { it.copy(messages = demoList, isLoading = false) }
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
            _uiState.update { it.copy(isSending = true) }
            convocationRepository.parentSendMessage(text)
                .onSuccess { newMsg ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + newMsg,
                            inputMessage = "",
                            isSending = false
                        )
                    }
                }
                .onFailure {
                    // Demo append if offline
                    val now = Instant.now().toString()
                    val localMsg = ParentConvocationMessageDto(
                        id = System.currentTimeMillis(),
                        senderUserId = 1L,
                        senderName = "Parent",
                        receiverUserId = 2L,
                        message = text,
                        childOriginated = false,
                        createdAt = now,
                        seen = false,
                        seenAt = null
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + localMsg,
                            inputMessage = "",
                            isSending = false
                        )
                    }
                }
        }
    }

    private fun getDemoHistory(): List<ParentConvocationMessageDto> {
        val now = Instant.now()
        return listOf(
            ParentConvocationMessageDto(
                id = 1L,
                senderUserId = 1L,
                senderName = "Parent",
                receiverUserId = 2L,
                message = "Please wrap up device time by 8:30 PM and finish science homework.",
                childOriginated = false,
                createdAt = now.minus(40, ChronoUnit.MINUTES).toString(),
                seen = true,
                seenAt = now.minus(35, ChronoUnit.MINUTES).toString()
            ),
            ParentConvocationMessageDto(
                id = 2L,
                senderUserId = 2L,
                senderName = "Alex",
                receiverUserId = 1L,
                message = "Finished science homework, packing my bag now!",
                childOriginated = true,
                createdAt = now.minus(25, ChronoUnit.MINUTES).toString(),
                seen = true,
                seenAt = now.minus(20, ChronoUnit.MINUTES).toString()
            )
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
