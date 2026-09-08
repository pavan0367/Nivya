package com.nivya.ui.screen_time

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nivya.data.repository.UsageRepository
import com.nivya.services.usage.AppUsageRecord
import com.nivya.services.usage.UsagePermissionHelper
import com.nivya.services.usage.UsagePermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChildScreenTimeUiState(
    val permissionState: UsagePermissionState = UsagePermissionState.REQUIRED,
    val totalForegroundSeconds: Long = 0L,
    val formattedTotalTime: String = "0m",
    val educationalSeconds: Long = 0L,
    val formattedEducationalTime: String = "0m",
    val recreationalSeconds: Long = 0L,
    val formattedRecreationalTime: String = "0m",
    val socialSeconds: Long = 0L,
    val productivitySeconds: Long = 0L,
    val topApps: List<AppUsageRecord> = emptyList(),
    val isSynced: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChildScreenTimeViewModel(
    private val appContext: Context,
    private val usageRepository: UsageRepository,
    private val gson: Gson = Gson()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildScreenTimeUiState(isLoading = true))
    val uiState: StateFlow<ChildScreenTimeUiState> = _uiState.asStateFlow()

    private var hasRequestedPermission = false

    init {
        checkPermission(isInitialCheck = true)

        viewModelScope.launch {
            usageRepository.getLatestUsageFlow().collectLatest { entity ->
                if (entity != null) {
                    val appListType = object : TypeToken<List<AppUsageRecord>>() {}.type
                    val apps: List<AppUsageRecord> = try {
                        gson.fromJson(entity.topAppsJson, appListType) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    _uiState.value = _uiState.value.copy(
                        totalForegroundSeconds = entity.totalForegroundSeconds,
                        formattedTotalTime = formatDuration(entity.totalForegroundSeconds),
                        educationalSeconds = entity.educationalSeconds,
                        formattedEducationalTime = formatDuration(entity.educationalSeconds),
                        recreationalSeconds = entity.recreationalSeconds,
                        formattedRecreationalTime = formatDuration(entity.recreationalSeconds),
                        socialSeconds = entity.socialSeconds,
                        productivitySeconds = entity.productivitySeconds,
                        topApps = apps,
                        isSynced = entity.isSynced,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun markPermissionRequested() {
        hasRequestedPermission = true
    }

    fun checkPermission(isInitialCheck: Boolean = false) {
        val granted = UsagePermissionHelper.isUsagePermissionGranted(appContext)
        val newState = when {
            granted -> UsagePermissionState.GRANTED
            !isInitialCheck && hasRequestedPermission -> UsagePermissionState.TRY_AGAIN
            else -> UsagePermissionState.REQUIRED
        }

        _uiState.value = _uiState.value.copy(permissionState = newState)

        if (granted) {
            refresh()
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = usageRepository.collectAndRecordUsage()
                _uiState.value = _uiState.value.copy(
                    totalForegroundSeconds = result.totalForegroundSeconds,
                    formattedTotalTime = formatDuration(result.totalForegroundSeconds),
                    educationalSeconds = result.educationalSeconds,
                    formattedEducationalTime = formatDuration(result.educationalSeconds),
                    recreationalSeconds = result.recreationalSeconds,
                    formattedRecreationalTime = formatDuration(result.recreationalSeconds),
                    socialSeconds = result.socialSeconds,
                    productivitySeconds = result.productivitySeconds,
                    topApps = result.apps.take(15),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to refresh usage stats"
                )
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        val minutes = seconds / 60
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return if (hours > 0) "${hours}h ${remMinutes}m" else "${remMinutes}m"
    }

    companion object {
        fun provideFactory(
            context: Context,
            usageRepository: UsageRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildScreenTimeViewModel(context.applicationContext, usageRepository) as T
                }
            }
    }
}
