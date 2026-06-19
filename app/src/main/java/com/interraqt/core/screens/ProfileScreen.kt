package com.interraqt.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(username: String, onNavigateToSettings: () -> Unit) { // 🚨 Receives instant username
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val primaryBlue = Color(0xFF0B57D0)

    var showUploadSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        
        // 🚨 Double padding fixed. This now sits perfectly below the icons!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("@$username", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            
            IconButton(onClick = onNavigateToSettings) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.width(22.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(16.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(primaryBlue)
                    .clickable { showUploadSheet = true }
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload", tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

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
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = "Image", tint = primaryBlue, modifier = Modifier.size(28.dp)) }
                            Text("Image", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Videocam, contentDescription = "Video", tint = primaryBlue, modifier = Modifier.size(28.dp)) }
                            Text("Video", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Notes, contentDescription = "Text", tint = primaryBlue, modifier = Modifier.size(28.dp)) }
                            Text("Text", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = caption, onValueChange = { caption = it },
                        placeholder = { Text("Write a caption...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = primaryBlue, unfocusedBorderColor = Color.DarkGray.copy(alpha = 0.5f), focusedTextColor = textColor, unfocusedTextColor = textColor)
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
