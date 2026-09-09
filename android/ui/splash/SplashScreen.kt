package com.nivya.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nivya.R
import com.nivya.core.di.AppContainer
import com.nivya.core.navigation.NavigationDestination
import com.nivya.ui.role.RoleType
import kotlinx.coroutines.delay

/**
 * Approved Nivya Splash Screen displaying the authoritative Mother-and-Child sunset visual.
 * Implements startup state routing:
 * - If already paired: directly routes to Parent or Child Dashboard without showing Role Selection or Pairing.
 * - If offline: preserves previously established pairing from local cache and opens correct Dashboard.
 * - If unpaired: routes to Login or Role Selection.
 */
@Composable
fun SplashScreen(
    appContainer: AppContainer,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        // Brief pause to display the approved cinematic artwork
        delay(1200)

        val isLoggedIn = appContainer.authRepository.isLoggedIn()
        val savedRole = appContainer.roleRepository.getSavedRole()

        if (!isLoggedIn) {
            onNavigateTo(NavigationDestination.Login.route)
            return@LaunchedEffect
        }

        // Check local cached pairing in Room
        val cachedFamily = try {
            appContainer.database.familyDao().getFamily()
        } catch (_: Exception) {
            null
        }

        val isPaired = cachedFamily?.isPaired == true

        if (isPaired && savedRole != null) {
            // Already-paired flow: directly open correct dashboard
            if (savedRole == RoleType.PARENT) {
                onNavigateTo(NavigationDestination.ParentDashboard.route)
            } else {
                onNavigateTo(NavigationDestination.ChildDashboard.route)
            }
        } else if (savedRole == null) {
            onNavigateTo(NavigationDestination.RoleSelection.route)
        } else {
            onNavigateTo("pairing/connection/${savedRole.name}")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        // Authoritative mother-and-child sunset visual from master asset
        Image(
            painter = painterResource(id = R.drawable.nivya_splash),
            contentDescription = "Nivya - Together for a Safer Tomorrow",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value)
        )
    }
}
