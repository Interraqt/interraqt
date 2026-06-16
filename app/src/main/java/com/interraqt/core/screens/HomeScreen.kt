package com.interraqt.core.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
@Composable fun HomeScreen() { 
    val isDark = isSystemInDarkTheme()
    Box(modifier = Modifier.fillMaxSize().background(if(isDark) Color.Black else Color.White), contentAlignment = Alignment.Center) { 
        Text("Home Feed", color = if(isDark) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) 
    } 
}
