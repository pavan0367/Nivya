package com.nivya.ui.cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.data.repository.CleanUpRepository
import com.nivya.services.cleanup.CleanUpCategory
import com.nivya.services.cleanup.CleanUpResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChildCleanUpUiState(
    val isScanning: Boolean = false,
    val isCleaning: Boolean = false,
    val progress: Float = 0f,
    val showConfirmDialog: Boolean = false,
    val categories: List<CleanUpCategory> = emptyList(),
    val cleanUpResult: CleanUpResult? = null,
    val errorMessage: String? = null
) {
    val selectedEstimatedBytes: Long
        get() = categories.filter { it.isSupported && it.isSelected }.sumOf { it.estimatedBytes }

    val hasSelectedSupportedCategories: Boolean
        get() = categories.any { it.isSupported && it.isSelected }
}

class ChildCleanUpViewModel(
    private val cleanUpRepository: CleanUpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildCleanUpUiState(isScanning = true))
    val uiState: StateFlow<ChildCleanUpUiState> = _uiState.asStateFlow()

    init {
        scanStorage()
    }

    fun scanStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                val scanned = cleanUpRepository.scanRemovableData()
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    categories = scanned
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Unable to scan temporary storage: ${e.localizedMessage}"
                )
            }
        }
    }

    fun toggleCategory(categoryId: String) {
        val updated = _uiState.value.categories.map { cat ->
            if (cat.id == categoryId && cat.isSupported) {
                cat.copy(isSelected = !cat.isSelected)
            } else {
                cat
            }
        }
        _uiState.value = _uiState.value.copy(categories = updated)
    }

    fun requestConfirmation() {
        if (_uiState.value.hasSelectedSupportedCategories) {
            _uiState.value = _uiState.value.copy(showConfirmDialog = true)
        }
    }

    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun executeCleanUp() {
        val selectedIds = _uiState.value.categories
            .filter { it.isSupported && it.isSelected }
            .map { it.id }
            .toSet()

        if (selectedIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false,
            isCleaning = true,
            progress = 0f,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Progressive feedback during cleanup
                val result = cleanUpRepository.executeCleanUp(selectedIds) { fraction ->
                    _uiState.value = _uiState.value.copy(progress = fraction)
                }

                // Brief pause so the child user sees completion
                delay(300)

                // Refresh scan to reflect newly freed space
                val refreshed = cleanUpRepository.scanRemovableData()

                _uiState.value = _uiState.value.copy(
                    isCleaning = false,
                    progress = 1f,
                    cleanUpResult = result,
                    categories = refreshed
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCleaning = false,
                    errorMessage = "Clean-up encountered an issue: ${e.localizedMessage}"
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(cleanUpResult = null)
    }

    fun openSystemStorageSettings() {
        try {
            cleanUpRepository.openSystemStorageSettings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not open system settings: ${e.localizedMessage}"
            )
        }
    }

    companion object {
        fun provideFactory(cleanUpRepository: CleanUpRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildCleanUpViewModel(cleanUpRepository) as T
                }
            }
    }
}
