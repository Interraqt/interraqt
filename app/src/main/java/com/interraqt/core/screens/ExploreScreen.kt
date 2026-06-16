package com.interraqt.core.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
@Composable fun ExploreScreen() { Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) { Text("Explore Feed", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) } }
