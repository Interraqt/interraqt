package com.interraqt.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    // Smoothest Home variant
    object Home : Screen("Home", Icons.Rounded.Home, Icons.Outlined.Home)
    // Upgraded from outdated ChatBubble to modern Paper Plane (Send)
    object Chat : Screen("Chat", Icons.Rounded.Send, Icons.Outlined.Send)
    object Explore : Screen("Explore", Icons.Rounded.Search, Icons.Outlined.Search)
    object Video : Screen("Video", Icons.Rounded.PlayArrow, Icons.Outlined.PlayCircleOutline)
    object Profile : Screen("Profile", Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle)
}
