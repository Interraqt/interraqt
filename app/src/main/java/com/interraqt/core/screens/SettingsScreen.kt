package com.interraqt.core.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    username: String, // 🚨 Received instantly
    onUsernameUpdated: (String) -> Unit, // 🚨 Callback to update app globally
    onNavigateBack: () -> Unit, 
    onLogout: () -> Unit
) {
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

    val currentEmail = currentUser?.email ?: "Unknown Email"

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newUsernameInput by remember { mutableStateOf("") }

    BackHandler { onNavigateBack() }

    Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.padding(end = 16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            }

            Text("ACCOUNT DETAILS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Email Address", fontSize = 12.sp, color = Color.Gray)
                    Text(currentEmail, fontSize = 16.sp, color = textColor, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Text("Username", fontSize = 12.sp, color = Color.Gray)
                    Text("@$username", fontSize = 16.sp, color = textColor)
                }
            }

            Button(
                onClick = { 
                    newUsernameInput = username
                    showEditUsernameDialog = true 
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue.copy(alpha = 0.1f), contentColor = primaryBlue)
            ) { Text("Edit Username", fontWeight = FontWeight.Bold) }

            Text("SECURITY", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = surfaceColor, contentColor = textColor)
            ) { Text("Send Password Reset Email") }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.1f), contentColor = textColor)
            ) { Text("Log Out") }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = redColor.copy(alpha = 0.1f), contentColor = redColor)
            ) { Text("Delete Account", fontWeight = FontWeight.Bold) }
        }
    }

    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            containerColor = surfaceColor,
            title = { Text("Edit Username", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newUsernameInput, onValueChange = { newUsernameInput = it },
                    label = { Text("New Username") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = textColor, unfocusedTextColor = textColor),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalUsername = newUsernameInput.trim().lowercase()
                    currentUser?.uid?.let { uid ->
                        firestore.collection("users").document(uid).update("username", finalUsername)
                            .addOnSuccessListener {
                                onUsernameUpdated(finalUsername) // 🚨 Instantly updates app everywhere
                                showEditUsernameDialog = false
                                Toast.makeText(context, "Username Updated!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) { Text("Save", color = primaryBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showEditUsernameDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = surfaceColor,
            title = { Text("Reset Password?", color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text("We will send a secure password reset link to $currentEmail.", color = textColor) },
            confirmButton = {
                TextButton(onClick = {
                    auth.sendPasswordResetEmail(currentEmail).addOnSuccessListener {
                        showResetDialog = false
                        Toast.makeText(context, "Reset link sent!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Send Link", color = primaryBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = surfaceColor,
            title = { Text("Log Out", color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your account?", color = textColor) },
            confirmButton = {
                TextButton(onClick = {
                    auth.signOut()
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Log Out", color = redColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = surfaceColor,
            title = { Text("Delete Account", color = redColor, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone. All your posts, likes, and profile data will be permanently wiped.", color = textColor) },
            confirmButton = {
                TextButton(onClick = {
                    currentUser?.uid?.let { uid ->
                        firestore.collection("users").document(uid).delete().addOnSuccessListener {
                            currentUser.delete().addOnSuccessListener {
                                Toast.makeText(context, "Account Permanently Deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                onLogout()
                            }
                        }
                    }
                }) { Text("Delete Everything", color = redColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }
}
