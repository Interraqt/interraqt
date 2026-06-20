package com.interraqt.core.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // 🚨 Keyboard & Focus Managers
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isDark = isSystemInDarkTheme()
    
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328)

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // 🚨 FADE MATH SETUP
    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEndPx = statusBarHeightPx + with(density) { 90.dp.toPx() }

    BackHandler { onNavigateBack() }

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                displayName = doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Update your name"
                username = doc.getString("username") ?: ""
                bio = doc.getString("bio")?.takeIf { it.isNotBlank() } ?: "Welcome to Interraqt! You can update your bio in the edit profile section."
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveProfile: () -> Unit = {
        if (displayName.length > 24) {
            Toast.makeText(context, "Name cannot exceed 24 characters", Toast.LENGTH_SHORT).show()
        } else if (bio.length > 100) {
            Toast.makeText(context, "Bio cannot exceed 100 characters", Toast.LENGTH_SHORT).show()
        } else if (username.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
        } else {
            isSaving = true
            currentUser?.uid?.let { uid ->
                val updates = hashMapOf<String, Any>(
                    "name" to displayName.trim(),
                    "username" to username.trim().lowercase(),
                    "bio" to bio.trim()
                )
                firestore.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        isSaving = false
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        onNavigateBack() 
                    }
                    .addOnFailureListener {
                        isSaving = false
                        Toast.makeText(context, "Update Failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // 🚨 1. DISMISS KEYBOARD ON TAP OUTSIDE
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            }
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = primaryOrange, modifier = Modifier.align(Alignment.Center))
        } else {
            // 🚨 2. THE SCROLLING FORM
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() // 🚨 Lifts UI automatically when keyboard opens
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Spacer pushes content safely below the pinned top bar
                Spacer(modifier = Modifier.height(statusBarHeightDp + 80.dp))

                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = subTextColor)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(primaryOrange)
                            .clickable {
                                Toast.makeText(context, "ImgBB upload coming soon!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text("Name", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 24) displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = surfaceColor, focusedBorderColor = primaryOrange,
                        unfocusedBorderColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor
                    ),
                    singleLine = true,
                    supportingText = { Text("${displayName.length}/24", color = subTextColor, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Username", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = surfaceColor, focusedBorderColor = primaryOrange,
                        unfocusedBorderColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Bio", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 100) bio = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = surfaceColor, focusedBorderColor = primaryOrange,
                        unfocusedBorderColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor
                    ),
                    supportingText = { Text("${bio.length}/100", color = subTextColor, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
                )

                // Extra space at bottom ensures smooth scroll clearance
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            // 🚨 3. THE PINNED TOP BAR (Placed after the Column so it floats on top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                if (isSaving) {
                    CircularProgressIndicator(color = primaryOrange, modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        saveProfile()
                    }) {
                        Text("Save", color = primaryOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
