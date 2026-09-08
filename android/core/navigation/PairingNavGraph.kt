package com.nivya.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nivya.ui.pairing.PairingScreen
import com.nivya.ui.pairing.PairingViewModel
import com.nivya.ui.role.RoleType

/**
 * NavGraphBuilder extension for registering the Pairing and Connection screen.
 */
fun NavGraphBuilder.pairingNavigation(
    navController: NavController,
    userRole: RoleType,
    onPairingComplete: () -> Unit
) {
    composable(
        route = NavigationDestination.Connection.route
    ) {
        val viewModel = PairingViewModel(userRole)
        PairingScreen(
            viewModel = viewModel,
            userRole = userRole,
            onPairingComplete = onPairingComplete
        )
    }
}
