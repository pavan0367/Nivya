package com.nivya.ui.location

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.data.repository.LocationRepository
import com.nivya.permissions.location.LocationPermissionHelper
import com.nivya.permissions.location.LocationPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChildLocationUiState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMetersPerSec: Float? = null,
    val provider: String = "gps",
    val isGpsAvailable: Boolean = true,
    val isNetworkAvailable: Boolean = true,
    val permissionState: LocationPermissionState = LocationPermissionState.DENIED,
    val isBackgroundConsented: Boolean = false,
    val isStale: Boolean = false,
    val recordedAt: Long = 0L,
    val lastUpdatedText: String = "Not updated yet",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChildLocationViewModel(
    private val appContext: Context,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildLocationUiState(isLoading = true))
    val uiState: StateFlow<ChildLocationUiState> = _uiState.asStateFlow()

    init {
        checkPermissionsAndProvider()

        viewModelScope.launch {
            locationRepository.getLatestLocationFlow().collectLatest { entity ->
                if (entity != null) {
                    val now = System.currentTimeMillis()
                    val minutesAgo = (now - entity.recordedAt) / (60 * 1000)
                    val isStale = minutesAgo > 15 || !entity.isGpsAvailable

                    _uiState.value = _uiState.value.copy(
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        accuracyMeters = entity.accuracyMeters,
                        altitudeMeters = entity.altitudeMeters,
                        speedMetersPerSec = entity.speedMetersPerSec,
                        provider = entity.provider,
                        isGpsAvailable = entity.isGpsAvailable,
                        isNetworkAvailable = entity.isNetworkAvailable,
                        isBackgroundConsented = entity.isBackgroundConsented,
                        isStale = isStale,
                        recordedAt = entity.recordedAt,
                        lastUpdatedText = formatTimeAgo(entity.recordedAt),
                        isLoading = false
                    )
                }
            }
        }

        refresh()
    }

    fun checkPermissionsAndProvider() {
        val permState = LocationPermissionHelper.getLocationPermissionState(appContext)
        val gpsAvail = LocationPermissionHelper.isGpsProviderEnabled(appContext)
        val netAvail = LocationPermissionHelper.isNetworkProviderEnabled(appContext)
        val bgConsented = LocationPermissionHelper.hasBackgroundLocationPermission(appContext)

        _uiState.value = _uiState.value.copy(
            permissionState = permState,
            isGpsAvailable = gpsAvail,
            isNetworkAvailable = netAvail,
            isBackgroundConsented = bgConsented
        )
    }

    fun refresh() {
        checkPermissionsAndProvider()
        if (!LocationPermissionHelper.hasForegroundLocationPermission(appContext)) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val snapshot = locationRepository.collectAndRecordLocation("FOREGROUND")
                _uiState.value = _uiState.value.copy(
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                    accuracyMeters = snapshot.accuracyMeters,
                    altitudeMeters = snapshot.altitudeMeters,
                    speedMetersPerSec = snapshot.speedMetersPerSec,
                    provider = snapshot.provider,
                    isGpsAvailable = snapshot.isGpsAvailable,
                    isNetworkAvailable = snapshot.isNetworkAvailable,
                    permissionState = snapshot.permissionState,
                    isBackgroundConsented = snapshot.isBackgroundConsented,
                    isStale = snapshot.isStale,
                    recordedAt = snapshot.timestamp,
                    lastUpdatedText = formatTimeAgo(snapshot.timestamp),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to update location"
                )
            }
        }
    }

    private fun formatTimeAgo(timeMs: Long): String {
        if (timeMs <= 0) return "Never"
        val seconds = (System.currentTimeMillis() - timeMs) / 1000
        if (seconds < 60) return "Just now"
        val minutes = seconds / 60
        if (minutes < 60) return "${minutes}m ago"
        val hours = minutes / 60
        if (hours < 24) return "${hours}h ago"
        return "${hours / 24}d ago"
    }

    companion object {
        fun provideFactory(
            context: Context,
            locationRepository: LocationRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChildLocationViewModel(context.applicationContext, locationRepository) as T
                }
            }
    }
}
