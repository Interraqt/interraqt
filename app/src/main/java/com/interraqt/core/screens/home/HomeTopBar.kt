package com.interraqt.core.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeTopBar(
    topBarOffsetProvider: () -> Dp, // 🚨 FIX: Lambda prevents recomposition lag
    topBarAlphaProvider: () -> Float, // 🚨 FIX: Lambda prevents recomposition lag
    statusBarHeightDp: Dp,
    bgColor: Color,
    glassColor: Color,
    textColor: Color,
    onNavigateToCreatePost: () -> Unit
) {
    Box(
        modifier = Modifier
            // 🚨 FIX: GraphicsLayer renders smoothly on the GPU without freezing the scroll!
            .offset { IntOffset(0, topBarOffsetProvider().roundToPx()) }
            .graphicsLayer { alpha = topBarAlphaProvider() }
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.95f)) 
            .padding(top = statusBarHeightDp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToCreatePost() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, contentDescription = "Create", tint = textColor, modifier = Modifier.size(24.dp)) }

            Text(
                text = "Interraqt", 
                fontSize = 22.sp, 
                fontWeight = FontWeight.Normal, 
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { },
                contentAlignment = Alignment.Center
            ) { Icon(HomeScreenIcons.Like, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(24.dp)) }
        }
    }
}
