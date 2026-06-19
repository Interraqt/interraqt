package com.interraqt.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    val isDark = isSystemInDarkTheme()
    // 🚨 PREMIUM BACKGROUND & SLATE CARDS 
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF8F9FA)
    val textColor = if (isDark) Color.White else Color.Black
    val cardColor = if (isDark) Color(0xFF161C24) else Color.White

    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .graphicsLayer { alpha = 0.99f } 
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = statusBarHeightPx + 60f 
                    )
                    drawContent()
                    drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(top = statusBarHeightDp + 16.dp, bottom = 100.dp) 
        ) {
            item {
                Text(
                    text = "Home Feed", 
                    color = textColor, 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            items(15) { index ->
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("Post #${index + 1}", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
