package com.interraqt.core.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(Screen.Home, Screen.Chat, Screen.Explore, Screen.Video, Screen.Profile)
    
    // 1. Detect Phone's Default Theme Instantly
    val isDark = isSystemInDarkTheme()
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val unselectedColor = if (isDark) Color.Gray else Color.DarkGray
    val indicatorColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)

    Surface(
        color = Color.Transparent, // Let the main app background show through the padding
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp) // Left/right breathing room + lowered lift
    ) {
        NavigationBar(
            containerColor = barColor,
            contentColor = contentColor,
            tonalElevation = 8.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp)) // This curves the outer edges to create a "Floating Dock" look
                .height(80.dp)
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
