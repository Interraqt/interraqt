package com.interraqt.core.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val primaryBlue = Color(0xFF0B57D0)
    val redColor = Color(0xFFD32F2F)

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // UI States for animations and sheets
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showUploadSheet by remember { mutableStateOf(false) }
    
    // User Data States
    var currentUsername by remember { mutableStateOf("Loading...") }
    var newUsername by remember { mutableStateOf("") }
    
    // Fetch User Data
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUsername = doc.getString("username") ?: "Unknown"
                newUsername = currentUsername
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        
        // --- TOP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("@$currentUsername", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            
            // The Custom 2-Line Menu Icon
            IconButton(onClick = { showSettingsSheet = true }) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.width(22.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(16.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                }
            }
        }

        // --- CENTER UPLOAD BUTTON ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primaryBlue)
                    .clickable { showUploadSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text("Create Post", modifier = Modifier.padding(top = 16.dp), color = Color.Gray, fontWeight = FontWeight.Medium)
        }

        // --- SETTINGS BOTTOM SHEET ---
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = surfaceColor
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState())) {
                    Text("Account Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 24.dp))
                    
                    // Edit Username
                    Text("Edit Username", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = newUsername, onValueChange = { newUsername = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp), singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    Button(
                        onClick = {
                            currentUser?.uid?.let { uid ->
                                firestore.collection("users").document(uid).update("username", newUsername.trim().lowercase())
                                    .addOnSuccessListener {
                                        currentUsername = newUsername
                                        Toast.makeText(context, "Username Updated!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) { Text("Save Username", color = Color.White) }

                    Divider(color = Color.DarkGray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 24.dp))

                    // Security & Account
                    Text("Security", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    
                    Button(
                        onClick = { 
                            auth.sendPasswordResetEmail(currentUser?.email ?: "").addOnSuccessListener {
                                Toast.makeText(context, "Reset link sent to email", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.1f), contentColor = textColor)
                    ) { Text("Reset Password") }

                    Button(
                        onClick = { 
                            auth.signOut()
                            showSettingsSheet = false
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.1f), contentColor = textColor)
                    ) { Text("Log Out") }

                    // Delete Account (Deep Wipe)
                    Button(
                        onClick = { 
                            currentUser?.uid?.let { uid ->
                                // 1. Delete user's Firestore document (In a real app, cloud functions delete their posts)
                                firestore.collection("users").document(uid).delete().addOnSuccessListener {
                                    // 2. Delete the Authentication account
                                    currentUser.delete().addOnSuccessListener {
                                        Toast.makeText(context, "Account Deleted", Toast.LENGTH_SHORT).show()
                                        onLogout()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = redColor.copy(alpha = 0.1f), contentColor = redColor)
                    ) { Text("Delete Account", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // --- UPLOAD BOTTOM SHEET ---
        if (showUploadSheet) {
            var caption by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { showUploadSheet = false },
                containerColor = surfaceColor
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("New Post", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* Select Image */ }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = "Image", tint = primaryBlue, modifier = Modifier.size(28.dp)) }
                            Text("Image", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* Select Video */ }) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Videocam, contentDescription = "Video", tint = primaryBlue, modifier = Modifier.size(28.dp)) }
                            Text("Video", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* Text Only */ }) {
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
