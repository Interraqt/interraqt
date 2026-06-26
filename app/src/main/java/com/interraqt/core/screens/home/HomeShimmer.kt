package com.interraqt.core.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerFeedPostCard(bgColor: Color, glassColor: Color) {
    // 🚨 FIX: Dynamic theme-aware color for perfect contrast
    val isDark = isSystemInDarkTheme()
    val skeletonColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(skeletonColor))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(modifier = Modifier.height(14.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(10.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f/5f).background(skeletonColor))
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(skeletonColor))
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(skeletonColor))
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(skeletonColor))
        }
    }
}
