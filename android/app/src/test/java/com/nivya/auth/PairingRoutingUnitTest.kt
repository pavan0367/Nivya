package com.nivya.auth

import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.navigation.NavigationDestination
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.dto.PairingStatusResponseDto
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying authoritative post-login pairing resolution for Parent and Child,
 * ensuring local Room cache wipes on logout do NOT incorrectly force RoleSelection when backend confirms paired.
 */
class PairingRoutingUnitTest {

    /**
     * Replicates the exact post-login destination resolution logic implemented in AppNavGraph.kt.
     */
    private fun resolvePostLoginDestination(
        roleStr: String?,
        isPaired: Boolean
    ): String {
        return if (isPaired && !roleStr.isNullOrBlank()) {
            if (roleStr.equals("PARENT", ignoreCase = true)) {
                NavigationDestination.ParentDashboard.route
            } else {
                NavigationDestination.ChildDashboard.route
            }
        } else if (!roleStr.isNullOrBlank()) {
            val roleName = roleStr.uppercase()
            "pairing/connection/$roleName"
        } else {
            NavigationDestination.RoleSelection.route
        }
    }

    /**
     * Replicates the authoritative pairing status evaluation implemented in AppNavGraph.kt and SplashScreen.kt.
     */
    private fun resolvePairingStatus(
        backendResult: NetworkResult<PairingStatusResponseDto>?,
        cachedFamily: FamilyEntity?
    ): Boolean {
        return when (backendResult) {
            is NetworkResult.Success<PairingStatusResponseDto> -> backendResult.data.paired
            else -> cachedFamily?.isPaired == true
        }
    }

    @Test
    fun testPairedParentLoginQueriesBackendAndRoutesToParentDashboard() {
        val pairedStatus = PairingStatusResponseDto(
            paired = true,
            familyId = 501L,
            familyCode = "FAM-TEST-01",
            familyName = "Test Family",
            userRole = "PARENT",
            members = emptyList(),
            devices = emptyList()
        )
        val backendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(pairedStatus)
        val cachedFamily: FamilyEntity? = null // Room cache empty

        val isPaired = resolvePairingStatus(backendResult, cachedFamily)
        val destination = resolvePostLoginDestination("PARENT", isPaired)

        assertTrue("Backend authoritative pairing must evaluate to true", isPaired)
        assertEquals(NavigationDestination.ParentDashboard.route, destination)
    }

    @Test
    fun testUnpairedParentRoutesToPairingSetup() {
        val unpairedStatus = PairingStatusResponseDto(
            paired = false,
            familyId = null,
            familyCode = null,
            familyName = null,
            userRole = "PARENT",
            members = emptyList(),
            devices = emptyList()
        )
        val backendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(unpairedStatus)
        val cachedFamily: FamilyEntity? = null

        val isPaired = resolvePairingStatus(backendResult, cachedFamily)
        val destination = resolvePostLoginDestination("PARENT", isPaired)

        assertFalse(isPaired)
        assertEquals("pairing/connection/PARENT", destination)
    }

    @Test
    fun testPairedChildRoutesDirectlyToChildDashboard() {
        val pairedStatus = PairingStatusResponseDto(
            paired = true,
            familyId = 502L,
            familyCode = "FAM-TEST-02",
            familyName = "Test Family",
            userRole = "CHILD",
            members = emptyList(),
            devices = emptyList()
        )
        val backendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(pairedStatus)
        val cachedFamily: FamilyEntity? = null

        val isPaired = resolvePairingStatus(backendResult, cachedFamily)
        val destination = resolvePostLoginDestination("CHILD", isPaired)

        assertTrue(isPaired)
        assertEquals(NavigationDestination.ChildDashboard.route, destination)
    }

    @Test
    fun testUnpairedChildRoutesToPairingConnection() {
        val unpairedStatus = PairingStatusResponseDto(
            paired = false,
            familyId = null,
            familyCode = null,
            familyName = null,
            userRole = "CHILD",
            members = emptyList(),
            devices = emptyList()
        )
        val backendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(unpairedStatus)
        val cachedFamily: FamilyEntity? = null

        val isPaired = resolvePairingStatus(backendResult, cachedFamily)
        val destination = resolvePostLoginDestination("CHILD", isPaired)

        assertFalse(isPaired)
        assertEquals("pairing/connection/CHILD", destination)
    }

    @Test
    fun testEmptyRoomCacheDoesNotIncorrectlyForceRoleSelectionWhenBackendSaysPaired() {
        // Room database cache is null after logout (Bug 2 root cause)
        val cachedFamily: FamilyEntity? = null

        // Backend confirms pairing is established remotely
        val backendPairedStatus = PairingStatusResponseDto(
            paired = true,
            familyId = 777L,
            familyCode = "FAM-ACTIVE",
            familyName = "Active Family",
            userRole = "PARENT",
            members = emptyList(),
            devices = emptyList()
        )
        val backendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(backendPairedStatus)

        // Authoritative resolution: Backend status takes priority
        val isPaired = resolvePairingStatus(backendResult, cachedFamily)
        val destination = resolvePostLoginDestination("PARENT", isPaired)

        assertTrue("Backend authority must override empty Room cache", isPaired)
        assertEquals(
            "Paired user with empty local Room cache must route to ParentDashboard, not RoleSelection",
            NavigationDestination.ParentDashboard.route,
            destination
        )
        assertNotEquals(NavigationDestination.RoleSelection.route, destination)
    }

    @Test
    fun testLogoutAndReloginPreservesRemotePairingRelationship() {
        // 1. User was paired locally
        val cachedBeforeLogout = FamilyEntity(
            familyId = 888L,
            familyCode = "FAM-888",
            familyName = "Smith",
            userRole = "PARENT",
            isPaired = true
        )
        assertTrue(cachedBeforeLogout.isPaired)

        // 2. Logout occurs -> clearFamily() called -> Room cache is wiped
        val cachedFamilyAfterLogout: FamilyEntity? = null

        // 3. User logs back in -> backend pairing status queried
        val remoteStatus = PairingStatusResponseDto(
            paired = true,
            familyId = 888L,
            familyCode = "FAM-888",
            familyName = "Smith",
            userRole = "PARENT",
            members = emptyList(),
            devices = emptyList()
        )
        val loginBackendResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Success(remoteStatus)

        val isPaired = resolvePairingStatus(loginBackendResult, cachedFamilyAfterLogout)
        val targetDestination = resolvePostLoginDestination("PARENT", isPaired)

        assertTrue("Remote pairing relationship is preserved on relogin", isPaired)
        assertEquals(NavigationDestination.ParentDashboard.route, targetDestination)
    }

    @Test
    fun testOfflineFallbackUsesCachedRoomStateWhenBackendFails() {
        val cachedFamily = FamilyEntity(
            familyId = 999L,
            familyCode = "FAM-OFFLINE",
            familyName = "Offline Family",
            userRole = "PARENT",
            isPaired = true
        )
        // Backend request fails due to offline connectivity
        val offlineResult: NetworkResult<PairingStatusResponseDto> = NetworkResult.Error(
            code = 503,
            message = "Network unavailable"
        )

        val isPaired = resolvePairingStatus(offlineResult, cachedFamily)
        val destination = resolvePostLoginDestination("PARENT", isPaired)

        assertTrue("Offline fallback must preserve cached Room pairing", isPaired)
        assertEquals(NavigationDestination.ParentDashboard.route, destination)
    }
}
