package com.interraqt.core.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(Screen.Home, Screen.Chat, Screen.Explore, Screen.Video, Screen.Profile)
    
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black,
        tonalElevation = 8.dp,
        modifier = Modifier.height(64.dp) // Much sleeker and shorter
    ) {
        items.forEachIndexed { index, screen ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon, 
                        contentDescription = screen.title,
                        modifier = Modifier.size(26.dp)
                    ) 
                },
                selected = isSelected,
                onClick = { onTabSelected(index) },
                alwaysShowLabel = false, // Hides the bulky text labels
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color(0xFFF0F0F0) // Subtle light gray highlight pill
                )
            )
        }
    }
}
