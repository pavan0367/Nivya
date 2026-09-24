package com.nivya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParentDashboardUiState(
    val hasChildDevice: Boolean = false,
    val childDeviceId: Long? = null,
    val childDeviceName: String = "No Device Paired",
    val isOnline: Boolean = false,
    val batteryPct: Int? = null,
    val networkType: String = "Unavailable",
    val isStale: Boolean = false,
    val lastSeen: String = "No data",
    val screenTimeFormatted: String = "--",
    val screenTimeUnit: String = "today",
    val deviceHealthFormatted: String = "--",
    val deviceHealthUnit: String = "health",
    val activeAlertsCount: String = "0",
    val activeAlertsUnit: String = "alerts",
    val convocationCount: String = "0",
    val convocationUnit: String = "unread",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ParentDashboardViewModel(
    private val pairingRepository: PairingRepository? = null,
    private val usageRepository: UsageRepository? = null,
    private val deviceHealthRepository: DeviceHealthRepository? = null,
    private val alertRepository: AlertRepository? = null,
    private val convocationRepository: ConvocationRepository? = null,
    private val tokenStorage: TokenStorage? = null,
    private val getCachedDevicesProvider: (suspend () -> List<DeviceStatusEntity>)? = null,
    private val getDailySummaryProvider: (suspend (deviceId: Long, today: String) -> NetworkResult<UsageSummaryResponseDto>)? = null,
    private val getDeviceHealthProvider: (suspend (deviceId: Long) -> NetworkResult<DeviceHealthResponseDto>)? = null,
    private val getUnreadAlertsCountProvider: (suspend () -> Result<Long>)? = null,
    private val getConvocationHistoryProvider: (suspend () -> Result<List<ParentConvocationMessageDto>>)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentDashboardUiState(isLoading = true))
    val uiState: StateFlow<ParentDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Resolve paired child devices
            val parentUuid = tokenStorage?.getDeviceUuid()
            val childDevices = if (getCachedDevicesProvider != null) {
                getCachedDevicesProvider.invoke().filter { it.deviceUuid != parentUuid }
            } else {
                pairingRepository?.getPairingStatus()
                val cached = pairingRepository?.getCachedDevices()?.firstOrNull() ?: emptyList()
                cached.filter { it.deviceUuid != parentUuid }
            }

            if (childDevices.isEmpty()) {
                _uiState.update {
                    it.copy(
                        hasChildDevice = false,
                        childDeviceId = null,
                        childDeviceName = "No Paired Device",
                        isOnline = false,
                        batteryPct = null,
                        networkType = "None",
                        isStale = false,
                        lastSeen = "Pair a child device",
                        screenTimeFormatted = "--",
                        deviceHealthFormatted = "--",
                        activeAlertsCount = "0",
                        convocationCount = "0",
                        isLoading = false
                    )
                }
                loadParentIndependentMetrics()
                return@launch
            }

            val child = childDevices.first()
            val childId = child.deviceId

            _uiState.update {
                it.copy(
                    hasChildDevice = true,
                    childDeviceId = childId,
                    childDeviceName = child.deviceName,
                    isOnline = child.isOnline,
                    batteryPct = child.batteryPct,
                    networkType = child.networkType ?: (if (child.isOnline) "Connected" else "Offline"),
                    isStale = child.isStale,
                    lastSeen = if (child.isOnline) "Online now" else if (!child.lastSeenAt.isNullOrBlank()) "Last seen ${child.lastSeenAt}" else "Offline",
                    isLoading = false
                )
            }

            // 2. Load Screen Time for child
            loadScreenTime(childId)

            // 3. Load Device Health for child
            loadDeviceHealth(childId, child.batteryPct)

            // 4. Load Alerts
            loadAlerts()

            // 5. Load Convocation messages
            loadConvocation()
        }
    }

    private fun loadScreenTime(childDeviceId: Long) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val result = if (getDailySummaryProvider != null) {
                    getDailySummaryProvider.invoke(childDeviceId, today)
                } else {
                    usageRepository?.getDailySummary(childDeviceId, today) ?: NetworkResult.Error(0, "Unavailable")
                }

                when (result) {
                    is NetworkResult.Success -> {
                        val totalSec = result.data.totalForegroundSeconds
                        val hours = totalSec / 3600
                        val mins = (totalSec % 3600) / 60
                        val formatted = when {
                            hours > 0 -> "${hours}h ${mins}m"
                            mins > 0 -> "${mins}m"
                            else -> "0m"
                        }
                        _uiState.update { it.copy(screenTimeFormatted = formatted, screenTimeUnit = "today") }
                    }
                    is NetworkResult.Error, is NetworkResult.Exception -> {
                        _uiState.update { it.copy(screenTimeFormatted = "0m", screenTimeUnit = "today") }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(screenTimeFormatted = "--", screenTimeUnit = "unavailable") }
            }
        }
    }

    private fun loadDeviceHealth(childDeviceId: Long, batteryPct: Int?) {
        viewModelScope.launch {
            try {
                val result = if (getDeviceHealthProvider != null) {
                    getDeviceHealthProvider.invoke(childDeviceId)
                } else {
                    deviceHealthRepository?.getDeviceHealth(childDeviceId) ?: NetworkResult.Error(0, "Unavailable")
                }

                when (result) {
                    is NetworkResult.Success -> {
                        val health = result.data
                        val isCharging = health.chargingState?.equals("CHARGING", ignoreCase = true) == true
                        val score = when {
                            health.healthScore > 0 -> "${health.healthScore}%"
                            batteryPct != null && batteryPct > 20 -> "${batteryPct}%"
                            isCharging -> "Charging"
                            else -> health.healthStatus.ifBlank { "Good" }
                        }
                        _uiState.update { it.copy(deviceHealthFormatted = score, deviceHealthUnit = "health") }
                    }
                    is NetworkResult.Error, is NetworkResult.Exception -> {
                        val fallback = if (batteryPct != null) "${batteryPct}%" else "--"
                        _uiState.update {
                            it.copy(
                                deviceHealthFormatted = fallback,
                                deviceHealthUnit = if (batteryPct != null) "battery" else "unavailable"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                val fallback = if (batteryPct != null) "${batteryPct}%" else "--"
                _uiState.update { it.copy(deviceHealthFormatted = fallback, deviceHealthUnit = "unavailable") }
            }
        }
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            try {
                val result = if (getUnreadAlertsCountProvider != null) {
                    getUnreadAlertsCountProvider.invoke()
                } else {
                    alertRepository?.getUnreadCount() ?: Result.success(0L)
                }

                result.onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            activeAlertsCount = count.toString(),
                            activeAlertsUnit = if (count == 1L) "alert" else "alerts"
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(activeAlertsCount = "0", activeAlertsUnit = "alerts") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(activeAlertsCount = "0", activeAlertsUnit = "alerts") }
            }
        }
    }

    private fun loadConvocation() {
        viewModelScope.launch {
            try {
                val result = if (getConvocationHistoryProvider != null) {
                    getConvocationHistoryProvider.invoke()
                } else {
                    convocationRepository?.parentGetHistory() ?: Result.success(emptyList())
                }

                result.onSuccess { list ->
                    val unread = list.count { !it.seen && it.childOriginated }
                    _uiState.update {
                        it.copy(
                            convocationCount = unread.toString(),
                            convocationUnit = "unread"
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(convocationCount = "0", convocationUnit = "unread") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(convocationCount = "0", convocationUnit = "unread") }
            }
        }
    }

    private fun loadParentIndependentMetrics() {
        loadAlerts()
        loadConvocation()
    }

    companion object {
        fun provideFactory(
            pairingRepository: PairingRepository,
            usageRepository: UsageRepository,
            deviceHealthRepository: DeviceHealthRepository,
            alertRepository: AlertRepository,
            convocationRepository: ConvocationRepository,
            tokenStorage: TokenStorage
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParentDashboardViewModel(
                    pairingRepository = pairingRepository,
                    usageRepository = usageRepository,
                    deviceHealthRepository = deviceHealthRepository,
                    alertRepository = alertRepository,
                    convocationRepository = convocationRepository,
                    tokenStorage = tokenStorage
                ) as T
            }
        }
    }
}
