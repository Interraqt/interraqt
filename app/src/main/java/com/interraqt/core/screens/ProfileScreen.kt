package com.interraqt.core.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlin.system.exitProcess // Used temporarily to force app restart on logout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val redColor = Color(0xFFD32F2F)

    var name by remember { mutableStateOf("John Doe") } // Placeholders for now
    var username by remember { mutableStateOf("johndoe") }

    Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
            )

            // Edit Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = fieldColor, focusedTextColor = textColor, unfocusedTextColor = textColor, unfocusedBorderColor = Color.Transparent
                )
            )

            // Edit Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = fieldColor, focusedTextColor = textColor, unfocusedTextColor = textColor, unfocusedBorderColor = Color.Transparent
                )
            )

            // Reset Password Button
            Button(
                onClick = { /* Reset Password Logic */ },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = fieldColor, contentColor = textColor)
            ) {
                Text("Reset Password", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { 
                    FirebaseAuth.getInstance().signOut()
                    // Temporarily closes the app so the user is forced to the Bouncer when they re-open
                    exitProcess(0) 
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = redColor.copy(alpha = 0.1f), contentColor = redColor)
            ) {
                Text("Log Out", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
