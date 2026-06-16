package com.interraqt.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    // Using 'Rounded' for a softer, more modern edge
    object Home : Screen("Home", Icons.Rounded.Home, Icons.Outlined.Home)
    object Chat : Screen("Chat", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Explore : Screen("Explore", Icons.Rounded.Search, Icons.Outlined.Search)
    object Video : Screen("Video", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle)
    object Profile : Screen("Profile", Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle)
}
