package com.interraqt.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    // Modern Home and Chat
    object Home : Screen("Home", Icons.Rounded.Home, Icons.Outlined.Home)
    object Chat : Screen("Chat", Icons.Rounded.Send, Icons.Outlined.Send)
    
    // Restored to your favorite high-contrast icons
    object Explore : Screen("Explore", Icons.Filled.Search, Icons.Outlined.Search)
    object Video : Screen("Video", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline)
    object Profile : Screen("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}
