package com.nivya.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.nivya.ui.role.RoleType

/**
 * Navigation destination contracts enforcing strict role isolation.
 */
sealed class NavigationDestination(val route: String, val title: String) {
    // Auth & Setup destinations
    object Splash : NavigationDestination("splash", "Splash")
    object Login : NavigationDestination("auth/login", "Login")
    object RoleSelection : NavigationDestination("auth/role_selection", "Role Selection")
    object Connection : NavigationDestination("pairing/connection/{role}", "Connection")

    // Parent Destinations (Strictly Parent-Only)
    object ParentDashboard : NavigationDestination("parent/dashboard", "Dashboard")
    object ParentLiveActivity : NavigationDestination("parent/live_activity", "Live Activity")
    object ParentHistory : NavigationDestination("parent/history", "History")
    object ParentAppUsage : NavigationDestination("parent/app_usage", "App Usage")
    object ParentCommunication : NavigationDestination("parent/communication", "Communication Status")
    object ParentLocation : NavigationDestination("parent/location", "Location")
    object ParentDeviceHealth : NavigationDestination("parent/device_health", "Device Health")
    object ParentNetwork : NavigationDestination("parent/network", "Network")
    object ParentAlerts : NavigationDestination("parent/alerts", "Alerts")
    object ParentConvocation : NavigationDestination("parent/convocation", "Convocation")
    object ParentFamilyDevices : NavigationDestination("parent/family_devices", "Family/Devices")
    object ParentSettings : NavigationDestination("parent/settings", "Settings")

    // Child Destinations (Strictly Child-Only, zero Parent controls)
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
 * Navigation item model for role-specific drawer rendering.
 */
data class NavigationDrawerItem(
    val destination: NavigationDestination,
    val icon: ImageVector,
    val badge: String? = null
)

/**
 * Authoritative navigation catalog enforcing role isolation.
 * Invariant: Child navigation strictly excludes Parent controls and common administrative leaks.
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
                NavigationDestination.ParentNetwork,
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

    fun getDrawerItems(role: RoleType): List<NavigationDrawerItem> {
        return when (role) {
            RoleType.PARENT -> listOf(
                NavigationDrawerItem(NavigationDestination.ParentDashboard, Icons.Default.Dashboard),
                NavigationDrawerItem(NavigationDestination.ParentLiveActivity, Icons.Default.Timeline),
                NavigationDrawerItem(NavigationDestination.ParentHistory, Icons.Default.History),
                NavigationDrawerItem(NavigationDestination.ParentAppUsage, Icons.Default.PieChart),
                NavigationDrawerItem(NavigationDestination.ParentCommunication, Icons.Default.Call),
                NavigationDrawerItem(NavigationDestination.ParentLocation, Icons.Default.Place),
                NavigationDrawerItem(NavigationDestination.ParentDeviceHealth, Icons.Default.Favorite),
                NavigationDrawerItem(NavigationDestination.ParentNetwork, Icons.Default.Wifi),
                NavigationDrawerItem(NavigationDestination.ParentAlerts, Icons.Default.Notifications, badge = "2"),
                NavigationDrawerItem(NavigationDestination.ParentConvocation, Icons.Default.Mail),
                NavigationDrawerItem(NavigationDestination.ParentFamilyDevices, Icons.Default.Devices),
                NavigationDrawerItem(NavigationDestination.ParentSettings, Icons.Default.Settings)
            )
            RoleType.CHILD -> listOf(
                NavigationDrawerItem(NavigationDestination.ChildDashboard, Icons.Default.Dashboard),
                NavigationDrawerItem(NavigationDestination.ChildBattery, Icons.Default.BatteryChargingFull),
                NavigationDrawerItem(NavigationDestination.ChildScreenTime, Icons.Default.Timer),
                NavigationDrawerItem(NavigationDestination.ChildNetwork, Icons.Default.Wifi),
                NavigationDrawerItem(NavigationDestination.ChildLocation, Icons.Default.Place),
                NavigationDrawerItem(NavigationDestination.ChildDeviceHealth, Icons.Default.Favorite),
                NavigationDrawerItem(NavigationDestination.ChildCleanUp, Icons.Default.Delete),
                NavigationDrawerItem(NavigationDestination.ChildAlerts, Icons.Default.Notifications),
                NavigationDrawerItem(NavigationDestination.ChildConvocation, Icons.Default.Mail),
                NavigationDrawerItem(NavigationDestination.ChildSettings, Icons.Default.Settings)
            )
        }
    }
}
