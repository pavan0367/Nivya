package com.nivya.ui.role

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Jetpack Compose Role Selection Screen.
 * Presents Parent and Child roles as distinct interactive cards.
 */
@Composable
fun RoleSelectionScreen(
    viewModel: RoleSelectionViewModel,
    onNavigateToConnection: (RoleType) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRole by remember { mutableStateOf<RoleType?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is RoleUiState.Success) {
            val success = uiState as RoleUiState.Success
            onNavigateToConnection(success.role)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nivya",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select Your Role",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your selected role determines your device interface and available tools.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Role Selection Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoleCard(
                    title = "Parent",
                    subtitle = "Manage family safety & device health",
                    description = "Monitor battery, network status, screen time, and send family guidance through Convocation.",
                    isSelected = selectedRole == RoleType.PARENT,
                    onClick = { selectedRole = RoleType.PARENT }
                )

                RoleCard(
                    title = "Child",
                    subtitle = "Personal device care & sharing",
                    description = "Share device status with parents, track your screen time, and clean up temporary storage.",
                    isSelected = selectedRole == RoleType.CHILD,
                    onClick = { selectedRole = RoleType.CHILD }
                )
            }

            // Action Button
            Button(
                onClick = {
                    selectedRole?.let { viewModel.confirmRole(it) }
                },
                enabled = selectedRole != null && uiState !is RoleUiState.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState is RoleUiState.Submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue to Connection",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "RoleCardBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
