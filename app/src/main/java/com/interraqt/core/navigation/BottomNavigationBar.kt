package com.interraqt.core.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
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
    
    val isDark = isSystemInDarkTheme()
    
    // Background Colors
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    
    // Custom High-Contrast Blue Theme for the Active Tab
    val indicatorColor = if (isDark) Color(0xFF004A77) else Color(0xFFD3E3FD) // The Light/Deep Blue Pill
    val selectedContentColor = if (isDark) Color(0xFFC2E7FF) else Color(0xFF0B57D0) // The Colored Filled Icon & Text
    
    // Unselected Tab Colors (Clean Gray)
    val unselectedContentColor = if (isDark) Color.Gray else Color.DarkGray

    Surface(
        color = barColor,
        shadowElevation = 16.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = unselectedContentColor,
            tonalElevation = 0.dp,
            // Height dropped to 58.dp to remove dead space and sit lower
            modifier = Modifier.height(58.dp) 
        ) {
            items.forEachIndexed { index, screen ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    icon = { 
                        Crossfade(
                            targetState = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            animationSpec = tween(durationMillis = 300),
                            label = "IconAnimation"
                        ) { activeIcon ->
                            Icon(
                                imageVector = activeIcon, 
                                contentDescription = screen.title,
                                modifier = Modifier.size(26.dp) 
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
                        selectedIconColor = selectedContentColor, // Applies deep blue to active icon
                        unselectedIconColor = unselectedContentColor,
                        selectedTextColor = selectedContentColor, // Applies deep blue to active text
                        unselectedTextColor = unselectedContentColor,
                        indicatorColor = indicatorColor // Applies light blue to the pill
                    )
                )
            }
        }
    }
}
