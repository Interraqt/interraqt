package com.interraqt.core.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(Screen.Home, Screen.Chat, Screen.Explore, Screen.Video, Screen.Profile)
    
    // We wrap the NavigationBar in a Surface to handle the bottom lift smoothly
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.White,
            contentColor = Color.Black,
            tonalElevation = 0.dp, // Removed so Surface handles the shadow
            modifier = Modifier
                .padding(bottom = 12.dp) // Lifts the bar up from curved screen edges
                .height(84.dp) // Taller to comfortably fit icons and text
        ) {
            items.forEachIndexed { index, screen ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon, 
                            contentDescription = screen.title,
                            modifier = Modifier.size(28.dp) // Larger icons to fill space
                        ) 
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontSize = 11.sp, // Small, clean, modern text size
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    alwaysShowLabel = true, // Text is now visible
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color.Black,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFFE0E0E0) // Slightly darker, visible highlight pill
                    )
                )
            }
        }
    }
}
