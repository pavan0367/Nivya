package com.nivya.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.navigation.NavigationDestination
import com.nivya.core.navigation.RoleNavigationCatalog
import com.nivya.ui.role.RoleType
import com.nivya.ui.theme.*

/**
 * Role-Specific Navigation Drawer enforcing strict compile-time and runtime isolation.
 * Child drawer never renders Parent-only administrative controls.
 */
@Composable
fun NivyaDrawerContent(
    currentRoute: String,
    role: RoleType,
    userName: String,
    familyCode: String?,
    onNavigateTo: (NavigationDestination) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerItems = RoleNavigationCatalog.getDrawerItems(role)
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerContainerColor = SurfaceDark,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Header Profile Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (role == RoleType.PARENT) BluePrimary else PurpleAccent,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "N",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (userName.isNotBlank()) userName else "Nivya User",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (role == RoleType.PARENT) BluePrimary.copy(alpha = 0.25f) else PurpleAccent.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = role.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (role == RoleType.PARENT) BlueLight else PurpleLight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (!familyCode.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = familyCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = OutlineDark)
                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items List
                drawerItems.forEach { item ->
                    val selected = currentRoute == item.destination.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.destination.title,
                                tint = if (selected) BluePrimary else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = item.destination.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        },
                        badge = if (item.badge != null) {
                            {
                                Badge(
                                    containerColor = ErrorRed,
                                    contentColor = Color.White
                                ) {
                                    Text(item.badge)
                                }
                            }
                        } else null,
                        selected = selected,
                        onClick = { onNavigateTo(item.destination) },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Footer Logout Action
            Column {
                HorizontalDivider(color = OutlineDark)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onLogoutClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
