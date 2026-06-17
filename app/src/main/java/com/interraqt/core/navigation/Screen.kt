package com.interraqt.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    // Matched to your Flaticon references
    object Home : Screen("Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Chat : Screen("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Explore : Screen("Explore", Icons.Filled.Search, Icons.Outlined.Search)
    // Left Video untouched as you requested
    object Video : Screen("Video", Icons.Filled.PlayArrow, Icons.Outlined.PlayCircleOutline)
    object Profile : Screen("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}
