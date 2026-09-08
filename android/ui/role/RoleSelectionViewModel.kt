package com.nivya.ui.role

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Architecture ViewModel managing role selection state and authoritative backend submission.
 */
class RoleSelectionViewModel(
    private val roleRepository: RoleRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoleUiState>(RoleUiState.Idle)
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun selectRole(role: RoleType) {
        _uiState.value = RoleUiState.Selected(role)
    }

    fun confirmRole(role: RoleType) {
        _uiState.value = RoleUiState.Submitting
        if (roleRepository != null) {
            viewModelScope.launch {
                when (val result = roleRepository.selectRole(role)) {
                    is NetworkResult.Success -> {
                        _uiState.value = RoleUiState.Success(
                            role = role,
                            nextDestination = "pairing/connection/${role.name}"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = RoleUiState.Error(result.message)
                    }
                    is NetworkResult.Exception -> {
                        _uiState.value = RoleUiState.Error(
                            result.exception.localizedMessage ?: "Failed to set role on server"
                        )
                    }
                }
            }
        } else {
            _uiState.value = RoleUiState.Success(
                role = role,
                nextDestination = "pairing/connection/${role.name}"
            )
        }
    }

    fun resetState() {
        _uiState.value = RoleUiState.Idle
    }

    companion object {
        fun provideFactory(roleRepository: RoleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoleSelectionViewModel(roleRepository) as T
                }
            }
    }
}
