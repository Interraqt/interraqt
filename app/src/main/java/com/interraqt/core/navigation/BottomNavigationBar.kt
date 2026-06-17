package com.interraqt.core.navigation

import androidx.compose.foundation.isSystemInDarkTheme
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
    
    // Theme Support kept intact
    val isDark = isSystemInDarkTheme()
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val unselectedColor = if (isDark) Color.Gray else Color.DarkGray
    val indicatorColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)

    Surface(
        color = barColor,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = barColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            modifier = Modifier
                .padding(bottom = 12.dp) // Keeps it safe from bottom curved edges
                .height(84.dp) // Restored to the proper size
        ) {
            items.forEachIndexed { index, screen ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon, 
                            contentDescription = screen.title,
                            modifier = Modifier.size(28.dp) 
                        ) 
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = contentColor,
                        unselectedIconColor = unselectedColor,
                        selectedTextColor = contentColor,
                        unselectedTextColor = unselectedColor,
                        indicatorColor = indicatorColor
                    )
                )
            }
        }
    }
}
