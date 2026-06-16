package com.interraqt.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Explore : Screen("explore", "Explore", Icons.Default.Search)
    object Video : Screen("video", "Video", Icons.Default.PlayArrow)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}
