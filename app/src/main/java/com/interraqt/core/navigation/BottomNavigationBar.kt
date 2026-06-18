package com.interraqt.core.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(Screen.Home, Screen.Chat, Screen.Explore, Screen.Video, Screen.Profile)
    
    val isDark = isSystemInDarkTheme()
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val unselectedColor = if (isDark) Color.Gray else Color.DarkGray
    
    val activeContentColor = if (isDark) Color(0xFF8AB4F8) else Color(0xFF0B57D0) 
    val indicatorColor = if (isDark) Color(0xFF004A77) else Color(0xFFD3E3FD) 

    // This state secretly tracks your finger during the swipe
    var currentDragIndex by remember { mutableStateOf<Int?>(null) }

    Surface(
        color = barColor,
        shadowElevation = 16.dp,
        // 1. Increased slightly from 68 to 70 to make room for the downward push
        modifier = Modifier.height(70.dp) 
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // 2. The tiny inward squeeze from the left and right edges
                .padding(horizontal = 7.dp)
                // The Custom Gesture Interceptor
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val tabWidthPx = size.width / 5f
                        
                        // Instantly calculate which tab you touched
                        var dragIndex = (down.position.x / tabWidthPx).toInt().coerceIn(0, 4)
                        currentDragIndex = dragIndex

                        var isTracking = true
                        while (isTracking) {
                            // Track your finger as it slides left or right
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position?.x ?: 0f
                            
                            dragIndex = (pos / tabWidthPx).toInt().coerceIn(0, 4)
                            currentDragIndex = dragIndex

                            // Detect when you lift your finger off the glass
                            if (event.changes.all { !it.pressed }) {
                                isTracking = false
                            }
                        }

                        // Fire the screen switch only when you let go
                        onTabSelected(dragIndex)
                        currentDragIndex = null
                    }
                }
        ) {
            val tabWidth = maxWidth / 5
            
            // If dragging, follow the finger. If not, park on the selected tab.
            val targetIndex = currentDragIndex ?: selectedIndex

            // The butter-smooth sliding math for the blue pill
            val pillXOffset by animateDpAsState(
                targetValue = (tabWidth * targetIndex) + (tabWidth / 2) - 32.dp,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "PillAnimation"
            )

            // 3. We manually draw the Sliding Blue Pill (Pushed down from 5.dp to 8.dp)
            Box(
                modifier = Modifier
                    .offset(x = pillXOffset, y = 10.dp) 
                    .size(width = 64.dp, height = 32.dp)
                    .background(indicatorColor, shape = CircleShape)
            )

            // 4. We draw the Icons and Text floating on top
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, screen ->
                    // Make the icon active instantly as the pill slides under it
                    val isHovered = targetIndex == index 
                    val contentColor = if (isHovered) activeContentColor else unselectedColor

                    Column(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            // Pushed downward slightly from 6.dp to 9.dp to match the pill
                            .padding(top = 11.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Crossfade(
                            targetState = if (isHovered) screen.selectedIcon else screen.unselectedIcon,
                            animationSpec = tween(durationMillis = 200),
                            label = "IconAnimation"
                        ) { activeIcon ->
                            Icon(
                                imageVector = activeIcon, 
                                contentDescription = screen.title,
                                modifier = Modifier.size(30.dp),
                                tint = contentColor
                            ) 
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = screen.title,
                            fontSize = 11.sp,
                            fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
