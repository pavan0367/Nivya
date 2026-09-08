package com.nivya.core.navigation

import com.nivya.ui.role.RoleType

/**
 * Navigation destination contracts enforcing strict role isolation.
 */
sealed class NavigationDestination(val route: String, val title: String) {
    // Auth & Setup destinations
    object Login : NavigationDestination("auth/login", "Login")
    object RoleSelection : NavigationDestination("auth/role_selection", "Role Selection")
    object Connection : NavigationDestination("pairing/connection/{role}", "Connection")

    // Parent Destinations
    object ParentDashboard : NavigationDestination("parent/dashboard", "Dashboard")
    object ParentLiveActivity : NavigationDestination("parent/live_activity", "Live Activity")
    object ParentHistory : NavigationDestination("parent/history", "History")
    object ParentAppUsage : NavigationDestination("parent/app_usage", "App Usage")
    object ParentCommunication : NavigationDestination("parent/communication", "Communication")
    object ParentLocation : NavigationDestination("parent/location", "Location")
    object ParentDeviceHealth : NavigationDestination("parent/device_health", "Device Health")
    object ParentAlerts : NavigationDestination("parent/alerts", "Alerts")
    object ParentConvocation : NavigationDestination("parent/convocation", "Convocation")
    object ParentFamilyDevices : NavigationDestination("parent/family_devices", "Family & Devices")
    object ParentSettings : NavigationDestination("parent/settings", "Settings")

    // Child Destinations
    object ChildDashboard : NavigationDestination("child/dashboard", "Dashboard")
    object ChildBattery : NavigationDestination("child/battery", "Battery")
    object ChildScreenTime : NavigationDestination("child/screen_time", "Screen Time")
    object ChildNetwork : NavigationDestination("child/network", "Network")
    object ChildLocation : NavigationDestination("child/location", "Location")
    object ChildDeviceHealth : NavigationDestination("child/device_health", "Device Health")
    object ChildCleanUp : NavigationDestination("child/cleanup", "Clean Up")
    object ChildAlerts : NavigationDestination("child/alerts", "Alerts")
    object ChildConvocation : NavigationDestination("child/convocation", "Convocation")
    object ChildSettings : NavigationDestination("child/settings", "Settings")
}

/**
 * Returns permitted destinations for the active role.
 * Invariant: Child navigation strictly excludes Parent controls.
 */
object RoleNavigationCatalog {

    fun getPermittedDestinations(role: RoleType): List<NavigationDestination> {
        return when (role) {
            RoleType.PARENT -> listOf(
                NavigationDestination.ParentDashboard,
                NavigationDestination.ParentLiveActivity,
                NavigationDestination.ParentHistory,
                NavigationDestination.ParentAppUsage,
                NavigationDestination.ParentCommunication,
                NavigationDestination.ParentLocation,
                NavigationDestination.ParentDeviceHealth,
                NavigationDestination.ParentAlerts,
                NavigationDestination.ParentConvocation,
                NavigationDestination.ParentFamilyDevices,
                NavigationDestination.ParentSettings
            )
            RoleType.CHILD -> listOf(
                NavigationDestination.ChildDashboard,
                NavigationDestination.ChildBattery,
                NavigationDestination.ChildScreenTime,
                NavigationDestination.ChildNetwork,
                NavigationDestination.ChildLocation,
                NavigationDestination.ChildDeviceHealth,
                NavigationDestination.ChildCleanUp,
                NavigationDestination.ChildAlerts,
                NavigationDestination.ChildConvocation,
                NavigationDestination.ChildSettings
            )
        }
    }
}
