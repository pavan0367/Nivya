package com.nivya.core.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nivya.core.di.AppContainer
import com.nivya.ui.alerts.ChildAlertsScreen
import com.nivya.ui.alerts.ParentAlertsScreen
import com.nivya.ui.auth.LoginScreen
import com.nivya.ui.auth.LoginViewModel
import com.nivya.ui.battery.ChildBatteryScreen
import com.nivya.ui.cleanup.ChildCleanUpScreen
import com.nivya.ui.common.*
import com.nivya.ui.convocation.ChildConvocationScreen
import com.nivya.ui.convocation.ParentConvocationScreen
import com.nivya.ui.dashboard.*
import com.nivya.ui.device_health.ChildDeviceHealthScreen
import com.nivya.ui.device_health.ParentDeviceHealthScreen
import com.nivya.ui.location.ChildLocationScreen
import com.nivya.ui.location.ParentLocationScreen
import com.nivya.ui.network.ChildNetworkScreen
import com.nivya.ui.network.ParentNetworkScreen
import com.nivya.ui.pairing.PairingScreen
import com.nivya.ui.pairing.PairingViewModel
import com.nivya.ui.role.RoleSelectionScreen
import com.nivya.ui.role.RoleSelectionViewModel
import com.nivya.ui.role.RoleType
import com.nivya.ui.screen_time.ChildScreenTimeScreen
import com.nivya.ui.screen_time.ParentAppUsageScreen
import com.nivya.ui.settings.ChildSettingsScreen
import com.nivya.ui.settings.ParentSettingsScreen
import kotlinx.coroutines.launch

/**
 * Main Navigation Graph coordinating Login, Role Selection, Pairing,
 * and Role-Isolated Navigation Drawers and App Bars for Parent and Child.
 */
@Composable
fun AppNavGraph(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isOnline by appContainer.networkMonitor.isOnline.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val isLoggedIn = remember { appContainer.authRepository.isLoggedIn() }
    val savedRole = remember { appContainer.roleRepository.getSavedRole() }

    val startDestination = when {
        !isLoggedIn -> NavigationDestination.Login.route
        savedRole == null -> NavigationDestination.RoleSelection.route
        else -> "pairing/connection/${savedRole.name}"
    }

    val isAuthFlow = currentRoute.startsWith("auth/") || currentRoute.startsWith("pairing/")
    val activeRole = when {
        currentRoute.startsWith("parent/") -> RoleType.PARENT
        currentRoute.startsWith("child/") -> RoleType.CHILD
        else -> savedRole ?: RoleType.PARENT
    }

    val currentScreenTitle = when (currentRoute) {
        // Parent Routes
        NavigationDestination.ParentDashboard.route -> "Parent Dashboard"
        NavigationDestination.ParentLiveActivity.route -> "Live Activity"
        NavigationDestination.ParentHistory.route -> "Activity History"
        NavigationDestination.ParentAppUsage.route -> "App Usage"
        NavigationDestination.ParentCommunication.route -> "Communication"
        NavigationDestination.ParentLocation.route -> "Location & Safe Zones"
        NavigationDestination.ParentDeviceHealth.route -> "Device Health"
        NavigationDestination.ParentNetwork.route -> "Network Quality"
        NavigationDestination.ParentAlerts.route -> "Safety Alerts"
        NavigationDestination.ParentConvocation.route -> "Convocation Guidance"
        NavigationDestination.ParentFamilyDevices.route -> "Family & Devices"
        NavigationDestination.ParentSettings.route -> "Settings"

        // Child Routes
        NavigationDestination.ChildDashboard.route -> "Child Dashboard"
        NavigationDestination.ChildBattery.route -> "Battery Status"
        NavigationDestination.ChildScreenTime.route -> "Screen Time"
        NavigationDestination.ChildNetwork.route -> "Network Status"
        NavigationDestination.ChildLocation.route -> "Location Sharing"
        NavigationDestination.ChildDeviceHealth.route -> "Device Health"
        NavigationDestination.ChildCleanUp.route -> "Storage Clean Up"
        NavigationDestination.ChildAlerts.route -> "My Notifications"
        NavigationDestination.ChildConvocation.route -> "Convocation Messages"
        NavigationDestination.ChildSettings.route -> "Settings & Privacy"

        else -> "Nivya"
    }

    val onPerformLogout: () -> Unit = {
        coroutineScope.launch {
            drawerState.close()
            appContainer.authRepository.logout()
            navController.navigate(NavigationDestination.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (showProfileDialog) {
        ProfileSessionDialog(
            role = activeRole,
            userName = if (activeRole == RoleType.PARENT) "Parent Account" else "Child Companion",
            familyCode = "FAM-NIVYA-01",
            deviceUuid = appContainer.tokenStorage.getDeviceUuid(),
            onDismiss = { showProfileDialog = false },
            onLogout = onPerformLogout
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isAuthFlow,
        drawerContent = {
            if (!isAuthFlow) {
                NivyaDrawerContent(
                    currentRoute = currentRoute,
                    role = activeRole,
                    userName = if (activeRole == RoleType.PARENT) "Parent Account" else "Child Account",
                    familyCode = "FAM-NIVYA-01",
                    onNavigateTo = { dest ->
                        coroutineScope.launch {
                            drawerState.close()
                            navController.navigate(dest.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onLogoutClick = onPerformLogout
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column {
                    if (!isAuthFlow) {
                        NivyaTopAppBar(
                            title = currentScreenTitle,
                            role = activeRole,
                            isOnline = isOnline,
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onProfileClick = { showProfileDialog = true }
                        )
                    }
                    if (!isOnline) {
                        OfflineBanner(lastSyncTime = "3m ago")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                // 1. Auth & Setup
                composable(route = NavigationDestination.Login.route) {
                    val loginViewModel: LoginViewModel = viewModel(
                        factory = LoginViewModel.provideFactory(appContainer.authRepository)
                    )
                    LoginScreen(
                        viewModel = loginViewModel,
                        onAuthSuccess = { roleStr ->
                            if (roleStr.isNullOrBlank()) {
                                navController.navigate(NavigationDestination.RoleSelection.route) {
                                    popUpTo(NavigationDestination.Login.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate("pairing/connection/$roleStr") {
                                    popUpTo(NavigationDestination.Login.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(route = NavigationDestination.RoleSelection.route) {
                    val roleViewModel: RoleSelectionViewModel = viewModel(
                        factory = RoleSelectionViewModel.provideFactory(appContainer.roleRepository)
                    )
                    RoleSelectionScreen(
                        viewModel = roleViewModel,
                        onNavigateToConnection = { role ->
                            navController.navigate("pairing/connection/${role.name}")
                        }
                    )
                }

                composable(
                    route = NavigationDestination.Connection.route,
                    arguments = listOf(navArgument("role") { type = NavType.StringType })
                ) { backStackEntry ->
                    val roleName = backStackEntry.arguments?.getString("role") ?: "PARENT"
                    val roleType = try {
                        RoleType.valueOf(roleName.uppercase())
                    } catch (_: Exception) {
                        RoleType.PARENT
                    }

                    val pairingViewModel: PairingViewModel = viewModel(
                        factory = PairingViewModel.provideFactory(roleType, appContainer.pairingRepository)
                    )
                    PairingScreen(
                        viewModel = pairingViewModel,
                        userRole = roleType,
                        onPairingComplete = {
                            val destination = if (roleType == RoleType.PARENT) {
                                NavigationDestination.ParentDashboard.route
                            } else {
                                NavigationDestination.ChildDashboard.route
                            }
                            navController.navigate(destination) {
                                popUpTo(NavigationDestination.Connection.route) { inclusive = true }
                            }
                        }
                    )
                }

                // 2. Parent Destinations
                composable(route = NavigationDestination.ParentDashboard.route) {
                    ParentDashboardScreen(onNavigateTo = { dest -> navController.navigate(dest.route) })
                }
                composable(route = NavigationDestination.ParentLiveActivity.route) {
                    ParentLiveActivityScreen()
                }
                composable(route = NavigationDestination.ParentHistory.route) {
                    ParentHistoryScreen()
                }
                composable(route = NavigationDestination.ParentAppUsage.route) {
                    ParentAppUsageScreen()
                }
                composable(route = NavigationDestination.ParentCommunication.route) {
                    ParentCommunicationScreen()
                }
                composable(route = NavigationDestination.ParentLocation.route) {
                    ParentLocationScreen()
                }
                composable(route = NavigationDestination.ParentDeviceHealth.route) {
                    ParentDeviceHealthScreen()
                }
                composable(route = NavigationDestination.ParentNetwork.route) {
                    ParentNetworkScreen()
                }
                composable(route = NavigationDestination.ParentAlerts.route) {
                    ParentAlertsScreen()
                }
                composable(route = NavigationDestination.ParentConvocation.route) {
                    ParentConvocationScreen()
                }
                composable(route = NavigationDestination.ParentFamilyDevices.route) {
                    ParentFamilyDevicesScreen(
                        onPairNewDevice = {
                            navController.navigate("pairing/connection/PARENT")
                        }
                    )
                }
                composable(route = NavigationDestination.ParentSettings.route) {
                    ParentSettingsScreen()
                }

                // 3. Child Destinations
                composable(route = NavigationDestination.ChildDashboard.route) {
                    ChildDashboardScreen(onNavigateTo = { dest -> navController.navigate(dest.route) })
                }
                composable(route = NavigationDestination.ChildBattery.route) {
                    ChildBatteryScreen()
                }
                composable(route = NavigationDestination.ChildScreenTime.route) {
                    ChildScreenTimeScreen()
                }
                composable(route = NavigationDestination.ChildNetwork.route) {
                    ChildNetworkScreen()
                }
                composable(route = NavigationDestination.ChildLocation.route) {
                    ChildLocationScreen()
                }
                composable(route = NavigationDestination.ChildDeviceHealth.route) {
                    ChildDeviceHealthScreen()
                }
                composable(route = NavigationDestination.ChildCleanUp.route) {
                    ChildCleanUpScreen()
                }
                composable(route = NavigationDestination.ChildAlerts.route) {
                    ChildAlertsScreen()
                }
                composable(route = NavigationDestination.ChildConvocation.route) {
                    ChildConvocationScreen()
                }
                composable(route = NavigationDestination.ChildSettings.route) {
                    ChildSettingsScreen()
                }
            }
        }
    }
}
