package com.interraqt.core.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
    
    // Theme Support
    val isDark = isSystemInDarkTheme()
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val unselectedColor = if (isDark) Color.Gray else Color.DarkGray
    val indicatorColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)

    // 1. Full-width Surface so the background touches the absolute edges
    Surface(
        color = barColor,
        shadowElevation = 16.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent, // Let Surface color show through
            contentColor = contentColor,
            tonalElevation = 0.dp,
            modifier = Modifier
                // 2. Keeps the background full-width, but pushes the icons safely inward!
                .padding(horizontal = 16.dp) 
                .height(84.dp)
        ) {
            items.forEachIndexed { index, screen ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    icon = { 
                        // 3. Premium Crossfade Animation for incredibly smooth icon changes
                        Crossfade(
                            targetState = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            animationSpec = tween(durationMillis = 300),
                            label = "IconAnimation"
                        ) { activeIcon ->
                            Icon(
                                imageVector = activeIcon, 
                                contentDescription = screen.title,
                                modifier = Modifier.size(28.dp) 
                            ) 
                        }
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
                        indicatorColor = indicatorColor // Keeps the highlight pill
                    )
                )
            }
        }
    }
}
