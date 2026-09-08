package com.nivya.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.ui.role.RoleType
import com.nivya.ui.theme.*

/**
 * Top App Bar with navigation drawer toggle, live connectivity dot, role badge, and profile button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NivyaTopAppBar(
    title: String,
    role: RoleType,
    isOnline: Boolean,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Role Pill Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (role == RoleType.PARENT) BluePrimary.copy(alpha = 0.2f) else PurpleAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (role == RoleType.PARENT) "PARENT" else "CHILD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (role == RoleType.PARENT) BlueLight else PurpleLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Live status dot
                Surface(
                    shape = CircleShape,
                    color = if (isOnline) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile & Session",
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            navigationIconContentColor = TextPrimary,
            actionIconContentColor = TextSecondary
        )
    )
}
