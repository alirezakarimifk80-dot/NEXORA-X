package com.nexora.x.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The five main navigation destinations required for NEXORA X:
 * 1. HOME
 * 2. SERVERS
 * 3. SUBSCRIPTIONS
 * 4. TOOLS
 * 5. SETTINGS
 */
enum class NexoraDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        label = "Home",
        icon = Icons.Outlined.Home
    ),
    SERVERS(
        route = "servers",
        label = "Servers",
        icon = Icons.Outlined.Dns
    ),
    SUBSCRIPTIONS(
        route = "subscriptions",
        label = "Subscriptions",
        icon = Icons.Outlined.RssFeed
    ),
    TOOLS(
        route = "tools",
        label = "Tools",
        icon = Icons.Outlined.Build
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings
    )
}
