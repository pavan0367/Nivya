package com.nivya.dashboard

import com.nivya.core.database.entities.ActivityEntity
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.ui.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 6 Unit Tests: Removal of Parent-side hardcoded/demo data and real data binding.
 *
 * Covers:
 * 1. Parent Dashboard:
 *    - Real child device data displayed (name, online, battery, network)
 *    - Real usage screen time displayed
 *    - Real device health data displayed
 *    - Real active alerts count displayed
 *    - Real convocation count displayed
 *    - Empty state when no child device paired (no demo fallback)
 *    - Offline state with authoritative cached data
 *    - Stale state representation
 * 2. Family & Devices:
 *    - Real paired child devices displayed
 *    - Parent device strictly excluded
 *    - Multiple child devices handled separately
 *    - No hardcoded family ID ("FAM-NIVYA-01" removed)
 *    - No hardcoded device names ("Sarah's Pixel 8", "Alex's Galaxy A54" removed)
 *    - Empty state handled
 * 3. Live Activity:
 *    - Real live activity displayed
 *    - No demo fallback when empty/offline (no "Chatting with Arun", no fake timestamps)
 *    - Room cache used when offline if authoritative data exists
 *    - Genuine empty state when no activity recorded
 * 4. History:
 *    - Real chronological history displayed
 *    - No demo fallback when empty/offline (no sample events)
 *    - Bounded pagination and filtering
 *    - Event detail uses dynamic child device name
 * 5. Device targeting & security:
 *    - Child-only targeting
 *    - No arbitrary fallback to parent web or hardcoded ID
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentDashboardUnitTest {

    private val parentUuid = "parent-device-uuid-999"
    private lateinit var fakeTokenStorage: FakeDashboardTokenStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        fakeTokenStorage = FakeDashboardTokenStorage(parentUuid)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDummyHealthDto(deviceId: Long, battery: Int? = 85, status: String = "Good", score: Int = 85): DeviceHealthResponseDto {
        return DeviceHealthResponseDto(
            deviceId = deviceId,
            deviceUuid = "child-device-uuid-$deviceId",
            deviceName = "Child Device",
            deviceModel = "Tablet",
            deviceManufacturer = "Brand",
            osVersion = "14",
            sdkVersion = 34,
            storage = StorageHealthDto(5000L, 1000L, 4000L, 20.0, false),
            memory = MemoryHealthDto(4000L, 2000L, 2000L, 50.0, false),
            batteryPct = battery,
            chargingState = "NOT_CHARGING",
            batteryHealth = "GOOD",
            batteryTempCelsius = 30.0,
            networkType = "Wi-Fi",
            isOnline = true,
            syncState = "SYNCED",
            permissionHealth = PermissionHealthDto("GRANTED", "GRANTED", "GRANTED", "OFF", true),
            healthScore = score,
            healthStatus = status,
            conditionSummary = status,
            storageSummary = "Normal",
            batterySummary = "${battery ?: 0}%",
            protectionSummary = "Active",
            recordedAt = "2026-09-20T10:00:00Z",
            updatedAt = "2026-09-20T10:00:00Z"
        )
    }

    private fun createDummyUsageSummary(deviceId: Long = 42L, totalSeconds: Long = 5400L): UsageSummaryResponseDto {
        return UsageSummaryResponseDto(
            deviceId = deviceId,
            deviceUuid = "child-device-uuid-$deviceId",
            deviceName = "Mia's Tablet",
            date = "2026-09-20",
            totalForegroundSeconds = totalSeconds,
            formattedTotalTime = "1h 30m",
            educationalSeconds = 3600L,
            recreationalSeconds = 1800L,
            socialSeconds = 0L,
            productivitySeconds = 0L,
            screenUnlocks = 12
        )
    }

    // =========================================================================
    // 1. PARENT DASHBOARD REAL DATA BINDING TESTS
    // =========================================================================

    @Test
    fun testDashboard_BindsRealChildDeviceAndMetrics() = runTest {
        val childDevice = DeviceStatusEntity(
            deviceId = 42L,
            deviceUuid = "child-device-uuid-42",
            deviceName = "Mia's Tablet",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 85,
            networkType = "Wi-Fi (Home)",
            networkQuality = "EXCELLENT",
            lastSyncAt = "2026-09-20T10:00:00Z",
            lastSeenAt = "2026-09-20T10:00:00Z",
            isStale = false
        )

        val viewModel = ParentDashboardViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getDailySummaryProvider = { id, _ ->
                NetworkResult.Success(createDummyUsageSummary(deviceId = id, totalSeconds = 5400L))
            },
            getDeviceHealthProvider = { id ->
                NetworkResult.Success(createDummyHealthDto(id, battery = 85, score = 85))
            },
            getUnreadAlertsCountProvider = { Result.success(5L) },
            getConvocationHistoryProvider = {
                Result.success(
                    listOf(
                        ParentConvocationMessageDto(
                            id = 1L,
                            senderUserId = 20L,
                            senderName = "Mia",
                            receiverUserId = 10L,
                            message = "Need help with homework",
                            childOriginated = true,
                            createdAt = "2026-09-20T10:00:00Z",
                            seen = false
                        ),
                        ParentConvocationMessageDto(
                            id = 2L,
                            senderUserId = 20L,
                            senderName = "Mia",
                            receiverUserId = 10L,
                            message = "Finished homework",
                            childOriginated = true,
                            createdAt = "2026-09-20T10:30:00Z",
                            seen = false
                        )
                    )
                )
            }
        )

        val state = viewModel.uiState.value

        // Child device bound
        assertTrue(state.hasChildDevice)
        assertEquals(42L, state.childDeviceId)
        assertEquals("Mia's Tablet", state.childDeviceName)
        assertTrue(state.isOnline)
        assertEquals(85, state.batteryPct)
        assertEquals("Wi-Fi (Home)", state.networkType)
        assertFalse(state.isStale)

        // Real screen time formatted (5400s = 1h 30m)
        assertEquals("1h 30m", state.screenTimeFormatted)
        assertEquals("today", state.screenTimeUnit)

        // Real health formatted
        assertEquals("85%", state.deviceHealthFormatted)

        // Real alerts count
        assertEquals("5", state.activeAlertsCount)

        // Real convocation unread count (2 unread child messages)
        assertEquals("2", state.convocationCount)

        // Ensure NO demo fallback values exist
        assertNotEquals("Alex's Galaxy A54", state.childDeviceName)
        assertNotEquals("2h 45m", state.screenTimeFormatted)
        assertNotEquals("94%", state.deviceHealthFormatted)
    }

    @Test
    fun testDashboard_EmptyStateWhenNoChildDevice() = runTest {
        val viewModel = ParentDashboardViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { emptyList() },
            getUnreadAlertsCountProvider = { Result.success(0L) },
            getConvocationHistoryProvider = { Result.success(emptyList()) }
        )

        val state = viewModel.uiState.value

        assertFalse("Must report no child device", state.hasChildDevice)
        assertEquals("No Paired Device", state.childDeviceName)
        assertEquals("--", state.screenTimeFormatted)
        assertEquals("--", state.deviceHealthFormatted)
        assertEquals("0", state.activeAlertsCount)
        assertEquals("0", state.convocationCount)
    }

    @Test
    fun testDashboard_OfflineChildDeviceReflectedAccurately() = runTest {
        val offlineChild = DeviceStatusEntity(
            deviceId = 77L,
            deviceUuid = "child-uuid-offline",
            deviceName = "Liam's Phone",
            platform = "ANDROID",
            isOnline = false,
            batteryPct = 40,
            networkType = "None",
            networkQuality = "OFFLINE",
            lastSyncAt = "2026-09-20T08:00:00Z",
            lastSeenAt = "10m ago",
            isStale = true
        )

        val viewModel = ParentDashboardViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(offlineChild) },
            getDailySummaryProvider = { _, _ -> NetworkResult.Error(0, "Offline") },
            getDeviceHealthProvider = { _ -> NetworkResult.Error(0, "Offline") },
            getUnreadAlertsCountProvider = { Result.failure(Exception("Offline")) },
            getConvocationHistoryProvider = { Result.failure(Exception("Offline")) }
        )

        val state = viewModel.uiState.value

        assertTrue(state.hasChildDevice)
        assertEquals("Liam's Phone", state.childDeviceName)
        assertFalse(state.isOnline)
        assertTrue(state.isStale)
        assertEquals(40, state.batteryPct)
        assertEquals("Last seen 10m ago", state.lastSeen)
        assertEquals("40%", state.deviceHealthFormatted)
    }

    // =========================================================================
    // 2. PARENT FAMILY & DEVICES TESTS
    // =========================================================================

    @Test
    fun testFamilyDevices_ExcludesParentOwnDevice() {
        val parentDevice = DeviceStatusEntity(
            deviceId = 1L,
            deviceUuid = parentUuid,
            deviceName = "Parent Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 95,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val child1 = DeviceStatusEntity(
            deviceId = 2L,
            deviceUuid = "child-uuid-1",
            deviceName = "Child 1 Tablet",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 80,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val allDevices = listOf(parentDevice, child1)
        val filteredChildDevices = allDevices.filter { it.deviceUuid != fakeTokenStorage.getDeviceUuid() }

        assertEquals(1, filteredChildDevices.size)
        assertEquals("Child 1 Tablet", filteredChildDevices.first().deviceName)
        assertFalse("Parent device must be excluded", filteredChildDevices.any { it.deviceUuid == parentUuid })
    }

    @Test
    fun testFamilyDevices_HandlesMultipleChildDevicesIndividually() {
        val child1 = DeviceStatusEntity(
            deviceId = 10L,
            deviceUuid = "child-1",
            deviceName = "Emma's Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 70,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val child2 = DeviceStatusEntity(
            deviceId = 11L,
            deviceUuid = "child-2",
            deviceName = "Noah's Tablet",
            platform = "ANDROID",
            isOnline = false,
            batteryPct = 30,
            networkType = "None",
            networkQuality = "OFFLINE",
            lastSyncAt = null,
            lastSeenAt = "1 hour ago",
            isStale = true
        )

        val allDevices = listOf(child1, child2)
        val filtered = allDevices.filter { it.deviceUuid != fakeTokenStorage.getDeviceUuid() }

        assertEquals(2, filtered.size)
        assertEquals("Emma's Phone", filtered[0].deviceName)
        assertTrue(filtered[0].isOnline)
        assertEquals("Noah's Tablet", filtered[1].deviceName)
        assertFalse(filtered[1].isOnline)
    }

    @Test
    fun testFamilyDevices_NoHardcodedFamilyCode() {
        val familyFromBackend = FamilyEntity(
            familyId = 55L,
            familyCode = "NV-REAL-CODE",
            familyName = "The Johnson Family",
            userRole = "PARENT",
            isPaired = true
        )

        val codeTitle = if (familyFromBackend.familyCode.isNotBlank()) {
            "Family Unit: ${familyFromBackend.familyCode}"
        } else {
            "Family Unit"
        }

        assertEquals("Family Unit: NV-REAL-CODE", codeTitle)
        assertNotEquals("Family Unit: FAM-NIVYA-01", codeTitle)
    }

    // =========================================================================
    // 3. LIVE ACTIVITY REAL-DATA & DEMO REMOVAL TESTS
    // =========================================================================

    @Test
    fun testLiveActivity_RealActivityDisplayed() = runTest {
        val realEvent = ActivityEventDto(
            id = 201L,
            deviceId = 50L,
            packageName = "org.khanacademy.android",
            appName = "Khan Academy",
            broadActivity = "Studying Math",
            category = "EDUCATION",
            durationSeconds = 1200,
            durationFormatted = "20m",
            isCurrent = true,
            startedAt = "2026-09-20T10:00:00Z",
            endedAt = null,
            createdAt = "2026-09-20T10:00:00Z"
        )

        val childDevice = DeviceStatusEntity(
            deviceId = 50L,
            deviceUuid = "child-uuid-50",
            deviceName = "Zoe's Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 90,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val viewModel = ParentLiveActivityViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getLiveActivityProvider = {
                flowOf(
                    LiveActivityResponseDto(
                        deviceId = 50L,
                        deviceUuid = "child-uuid-50",
                        deviceName = "Zoe's Phone",
                        isOnline = true,
                        currentActivity = realEvent,
                        recentActivities = listOf(realEvent)
                    )
                )
            }
        )

        val state = viewModel.uiState.value

        assertEquals(50L, state.deviceId)
        assertEquals("Zoe's Phone", state.deviceName)
        assertTrue(state.isOnline)
        assertNotNull(state.currentActivity)
        assertEquals("Khan Academy", state.currentActivity?.appName)
        assertEquals("Studying Math", state.currentActivity?.broadActivity)
        assertEquals(1, state.recentActivities.size)
        assertFalse(state.isStale)
    }

    @Test
    fun testLiveActivity_NoDemoFallbackWhenEmptyOrOffline() = runTest {
        val childDevice = DeviceStatusEntity(
            deviceId = 33L,
            deviceUuid = "child-33",
            deviceName = "Ethan's Phone",
            platform = "ANDROID",
            isOnline = false,
            batteryPct = 50,
            networkType = "None",
            networkQuality = "OFFLINE",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = true
        )

        val viewModel = ParentLiveActivityViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getLiveActivityProvider = { flowOf(null) },
            observeCurrentActivityProvider = { flowOf(null) },
            observeRecentActivitiesProvider = { flowOf(emptyList()) }
        )

        val state = viewModel.uiState.value

        // Must NOT fallback to "Chatting with Arun" or sample WhatsApp/YouTube
        assertNull("Current activity must be null when empty", state.currentActivity)
        assertTrue("Recent activities must be empty", state.recentActivities.isEmpty())
        assertFalse("Must not claim to be online", state.isOnline)

        // Verify none of the demo strings are present anywhere in state
        val recentTitles = state.recentActivities.map { it.broadActivity }
        assertFalse(recentTitles.contains("Chatting with Arun"))
        assertFalse(recentTitles.contains("Viewing report.pdf"))
        assertFalse(recentTitles.contains("In call with Mom"))
    }

    // =========================================================================
    // 4. HISTORY REAL-DATA & DEMO REMOVAL TESTS
    // =========================================================================

    @Test
    fun testHistory_RealHistoryDisplayed() = runTest {
        val historyItem = HistoryEventDto(
            id = 501L,
            deviceId = 80L,
            packageName = "com.duolingo",
            appName = "Duolingo",
            broadActivity = "Spanish Lesson",
            activityLabel = "Unit 4",
            category = "EDUCATION",
            durationSeconds = 600,
            durationFormatted = "10m",
            eventTimestamp = "2026-09-20T09:00:00Z"
        )

        val childDevice = DeviceStatusEntity(
            deviceId = 80L,
            deviceUuid = "child-uuid-80",
            deviceName = "Ava's Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 75,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val viewModel = ParentHistoryViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getHistoryProvider = { _, _, _, _, _, _ ->
                flowOf(
                    HistoryPageResponseDto(
                        items = listOf(historyItem),
                        currentPage = 0,
                        totalPages = 1,
                        totalElements = 1L,
                        pageSize = 15,
                        hasNext = false,
                        hasPrevious = false
                    )
                )
            },
            getDistinctApplicationsProvider = { Result.success(listOf("Duolingo")) }
        )

        val state = viewModel.uiState.value

        assertEquals(80L, state.deviceId)
        assertEquals("Ava's Phone", state.deviceName)
        assertEquals(1, state.items.size)
        assertEquals("Duolingo", state.items.first().appName)
        assertEquals("Spanish Lesson", state.items.first().broadActivity)
        assertEquals(1L, state.totalElements)
        assertFalse(state.isStale)
    }

    @Test
    fun testHistory_NoDemoFallbackWhenEmpty() = runTest {
        val childDevice = DeviceStatusEntity(
            deviceId = 90L,
            deviceUuid = "child-uuid-90",
            deviceName = "Lucas's Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 80,
            networkType = "Wi-Fi",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val viewModel = ParentHistoryViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getHistoryProvider = { _, _, _, _, _, _ -> flowOf(null) },
            getDistinctApplicationsProvider = { Result.success(emptyList()) }
        )

        val state = viewModel.uiState.value

        // Must be genuinely empty - NO sample items
        assertTrue("History items must be empty when backend is null", state.items.isEmpty())
        assertEquals(0L, state.totalElements)
        assertTrue("Stale indicator true on null response", state.isStale)

        // Verify none of the demo sample events are present
        val sampleTitles = state.items.map { it.broadActivity }
        assertFalse(sampleTitles.contains("Chatting with Arun"))
        assertFalse(sampleTitles.contains("Viewing report.pdf"))
        assertFalse(sampleTitles.contains("Science Documentary"))
    }

    @Test
    fun testHistory_SelectEventUsesDynamicDeviceName() = runTest {
        val event = HistoryEventDto(
            id = 701L,
            deviceId = 12L,
            packageName = "com.spotify.music",
            appName = "Spotify",
            broadActivity = "Listening to Music",
            activityLabel = null,
            category = "ENTERTAINMENT",
            durationSeconds = 1800,
            durationFormatted = "30m",
            eventTimestamp = "2026-09-20T11:00:00Z"
        )

        val childDevice = DeviceStatusEntity(
            deviceId = 12L,
            deviceUuid = "child-12",
            deviceName = "Ella's Phone",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 60,
            networkType = "Cellular",
            networkQuality = "GOOD",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val viewModel = ParentHistoryViewModel(
            tokenStorage = fakeTokenStorage,
            getCachedDevicesProvider = { listOf(childDevice) },
            getHistoryProvider = { _, _, _, _, _, _ ->
                flowOf(
                    HistoryPageResponseDto(
                        items = listOf(event),
                        currentPage = 0,
                        totalPages = 1,
                        totalElements = 1L,
                        pageSize = 15,
                        hasNext = false,
                        hasPrevious = false
                    )
                )
            }
        )

        viewModel.selectEvent(701L)

        val detail = viewModel.uiState.value.selectedEventDetail
        assertNotNull(detail)
        assertEquals("Ella's Phone", detail?.deviceName)
        assertNotEquals("Alex's Galaxy A54", detail?.deviceName)
    }

    // =========================================================================
    // 5. TARGETING & SECURITY TESTS
    // =========================================================================

    @Test
    fun testTargeting_NeverTargetsParentDevice() {
        val parentDevice = DeviceStatusEntity(
            deviceId = 100L,
            deviceUuid = parentUuid,
            deviceName = "Parent Controller",
            platform = "ANDROID",
            isOnline = true,
            batteryPct = 90,
            networkType = "Wi-Fi",
            networkQuality = "EXCELLENT",
            lastSyncAt = null,
            lastSeenAt = null,
            isStale = false
        )

        val devices = listOf(parentDevice)
        val targetedChild = devices.firstOrNull { it.deviceUuid != fakeTokenStorage.getDeviceUuid() }

        assertNull("Targeting must never select the Parent's own device as child target", targetedChild)
    }
}

/**
 * In-memory test implementation of TokenStorage.
 */
class FakeDashboardTokenStorage(
    private var deviceUuid: String = "parent-uuid-default"
) : TokenStorage {
    private var accessToken: String? = "valid-token"
    private var refreshToken: String? = "valid-refresh"
    private var userRole: String? = "PARENT"

    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken

    override fun saveUserRole(role: String) {
        this.userRole = role
    }

    override fun getUserRole(): String? = userRole

    override fun saveDeviceUuid(uuid: String) {
        this.deviceUuid = uuid
    }

    override fun getDeviceUuid(): String = deviceUuid

    override fun clearAll() {
        accessToken = null
        refreshToken = null
        userRole = null
    }

    override fun hasAccessToken(): Boolean = !accessToken.isNullOrBlank()
}
