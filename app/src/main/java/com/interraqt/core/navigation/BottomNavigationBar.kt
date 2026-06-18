package com.interraqt.core.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val unselectedColor = if (isDark) Color.Gray else Color.DarkGray
    
    // The High-Contrast Blue Theme
    val activeContentColor = if (isDark) Color(0xFF8AB4F8) else Color(0xFF0B57D0) 
    val indicatorColor = if (isDark) Color(0xFF004A77) else Color(0xFFD3E3FD) 

    Surface(
        color = barColor,
        shadowElevation = 16.dp,
        modifier = Modifier.height(66.dp) // 1. Restored safe height so the pill doesn't clip
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = activeContentColor,
            tonalElevation = 0.dp
            // 2. REMOVED the padding(top = 8.dp) that was slicing the pill flat!
        ) {
            
            Spacer(modifier = Modifier.width(10.dp))

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
                        selectedIconColor = activeContentColor,
                        selectedTextColor = activeContentColor,
                        indicatorColor = indicatorColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
        }
    }
}
