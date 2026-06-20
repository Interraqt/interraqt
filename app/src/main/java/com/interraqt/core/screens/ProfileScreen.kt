package com.interraqt.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(username: String, onNavigateToSettings: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    // 🚨 HYBRID THEME COLORS
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5) 
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328) 
    val primaryBlue = Color(0xFF0B57D0) 
    val glassColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    val isOwnProfile = true 
    var showUploadSheet by remember { mutableStateOf(false) }

    // 🚨 FADE MATH SETUP
    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEndPx = statusBarHeightPx + with(density) { 120.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        
        // --- 1. THE SCROLLING CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f } 
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = fadeEndPx 
                    )
                    drawContent()
                    drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(statusBarHeightDp + 80.dp))

            // --- PROFILE PICTURE & BIO ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(surfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = subTextColor)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Darlene Robertson", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = "Actress & Singer", fontSize = 14.sp, color = subTextColor, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // --- STATS ROW ---
            val dividerBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    subTextColor.copy(alpha = 0.4f),
                    Color.Transparent
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("200", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Posts", fontSize = 12.sp, color = subTextColor)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("97.5K", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Followers", fontSize = 12.sp, color = subTextColor)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("3.25M", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Following", fontSize = 12.sp, color = subTextColor)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- DYNAMIC PILL BUTTONS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryOrange)
                ) {
                    Text(if (isOwnProfile) "Edit Profile" else "Follow", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { if (isOwnProfile) showUploadSheet = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = glassColor)
                ) {
                    Text(if (isOwnProfile) "Share Profile" else "Message", color = textColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Collections", color = primaryOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(90.dp).height(2.dp).background(primaryOrange, RoundedCornerShape(1.dp)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Videos", color = subTextColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(90.dp).height(2.dp).background(Color.Transparent))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Photos", color = subTextColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(90.dp).height(2.dp).background(Color.Transparent))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DUMMY GRID ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- 2. THE FLOATING TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable { showUploadSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload", tint = textColor, modifier = Modifier.size(24.dp))
            }

            Text(
                text = "@$username", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = textColor
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                }
            }
        }

        // --- 3. THE UPLOAD BOTTOM SHEET ---
        if (showUploadSheet) {
            var caption by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { showUploadSheet = false },
                containerColor = surfaceColor
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("New Post", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = "Image", tint = primaryBlue, modifier = Modifier.size(28.dp))
                            }
                            Text("Image", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = primaryBlue, modifier = Modifier.size(28.dp))
                            }
                            Text("Video", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notes, contentDescription = "Text", tint = primaryBlue, modifier = Modifier.size(28.dp))
                            }
                            Text("Text", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Write a caption...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color.DarkGray.copy(alpha = 0.5f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    Button(
                        onClick = { showUploadSheet = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) { Text("Post", color = Color.White, fontWeight = FontWeight.Bold) }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
