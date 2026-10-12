package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.BOTTOM_NAV_ITEMS
import com.example.ui.navigation.Screen
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FintechTopBar(
    currentScreen: Screen,
    hideAmounts: Boolean,
    unresolvedFraudCount: Int,
    onToggleHideAmounts: () -> Unit,
    onNavigateToSandbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("top_app_bar"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "AutoPay Alert Logo",
                    tint = ElectricCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = currentScreen.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        actions = {
            // Privacy Amount Hide/Show Toggle
            IconButton(
                onClick = onToggleHideAmounts,
                modifier = Modifier.testTag("toggle_hide_amounts_button")
            ) {
                Icon(
                    imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (hideAmounts) "Show Balances" else "Hide Balances",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // SMS Sandbox / Live Tester Shortcut
            IconButton(
                onClick = onNavigateToSandbox,
                modifier = Modifier.testTag("sms_sandbox_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Test SMS Parser",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Settings Shortcut
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun FintechBottomBar(
    currentScreen: Screen,
    unresolvedFraudCount: Int,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        BOTTOM_NAV_ITEMS.forEach { screen ->
            val isSelected = currentScreen.route == screen.route
            val icon = when (screen) {
                Screen.Dashboard -> Icons.Default.AccountBalanceWallet
                Screen.Passbook -> Icons.Default.AccountBalanceWallet
                Screen.Insights -> Icons.Default.Equalizer
                Screen.Payments -> Icons.Default.Autorenew
                Screen.FraudShield -> Icons.Default.Security
                else -> Icons.Default.AccountBalanceWallet
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    if (screen == Screen.FraudShield && unresolvedFraudCount > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = CoralRed) {
                                Text(unresolvedFraudCount.toString())
                            }
                        }) {
                            Icon(imageVector = icon, contentDescription = screen.title)
                        }
                    } else {
                        Icon(imageVector = icon, contentDescription = screen.title)
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricCyan,
                    selectedTextColor = ElectricCyan,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${screen.route}")
            )
        }
    }
}
