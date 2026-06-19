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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White

    // The Feather Fade Gradient: Transparent at the very top, solid Black right below it
    val topFadingEdge = Brush.verticalGradient(
        0f to Color.Transparent,
        0.15f to Color.Black, 
        1f to Color.Black
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                // 🚨 The Graphics Layer that powers the feather effect
                .graphicsLayer { alpha = 0.99f } 
                .drawWithContent {
                    drawContent()
                    drawRect(brush = topFadingEdge, blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp)
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
            // Dummy posts so you can see the scrolling feather effect!
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
