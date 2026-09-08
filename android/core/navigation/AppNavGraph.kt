package com.nivya.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nivya.core.di.AppContainer
import com.nivya.ui.auth.LoginScreen
import com.nivya.ui.auth.LoginViewModel
import com.nivya.ui.pairing.PairingScreen
import com.nivya.ui.pairing.PairingViewModel
import com.nivya.ui.role.RoleSelectionScreen
import com.nivya.ui.role.RoleSelectionViewModel
import com.nivya.ui.role.RoleType
import kotlinx.coroutines.launch

/**
 * Main Navigation Graph coordinating Login, Role Selection, Pairing,
 * and Role-Isolated Foundation destinations.
 */
@Composable
fun AppNavGraph(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isOnline by appContainer.networkMonitor.isOnline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn = remember { appContainer.authRepository.isLoggedIn() }
    val savedRole = remember { appContainer.roleRepository.getSavedRole() }

    val startDestination = when {
        !isLoggedIn -> NavigationDestination.Login.route
        savedRole == null -> NavigationDestination.RoleSelection.route
        else -> "pairing/connection/${savedRole.name}"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Offline Mode - Operating on cached local data",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Login Screen
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

            // 2. Role Selection Screen
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

            // 3. Device Pairing Screen
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

            // 4. Parent Foundation Dashboard
            composable(route = NavigationDestination.ParentDashboard.route) {
                FoundationDashboardScreen(
                    role = RoleType.PARENT,
                    onLogout = {
                        coroutineScope.launch {
                            appContainer.authRepository.logout()
                            navController.navigate(NavigationDestination.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 5. Child Foundation Dashboard
            composable(route = NavigationDestination.ChildDashboard.route) {
                FoundationDashboardScreen(
                    role = RoleType.CHILD,
                    onLogout = {
                        coroutineScope.launch {
                            appContainer.authRepository.logout()
                            navController.navigate(NavigationDestination.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Foundation landing view confirming successful connection and session persistence.
 */
@Composable
fun FoundationDashboardScreen(
    role: RoleType,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Connected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${role.name} Connected",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (role == RoleType.PARENT)
                    "Parent device paired. Safe monitoring, battery status, and Convocation tools active."
                else
                    "Child device paired. Status sharing active. Parent-only administrative tools strictly excluded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Sign Out & Revoke Session")
            }
        }
    }
}
