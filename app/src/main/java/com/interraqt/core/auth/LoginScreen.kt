package com.interraqt.core.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Log in to continue to Interraqt", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = emailOrUsername, onValueChange = { emailOrUsername = it }, label = { Text("Email or Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (emailOrUsername.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill out all fields"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()
                val input = emailOrUsername.trim()

                if (!input.contains("@")) {
                    // Smart Login: Username detected, fetch email from Firestore first
                    db.collection("users").whereEqualTo("username", input).get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val loginEmail = documents.documents[0].getString("email") ?: ""
                                auth.signInWithEmailAndPassword(loginEmail, password)
                                    .addOnCompleteListener { authTask ->
                                        isLoading = false
                                        if (authTask.isSuccessful) onLoginSuccess()
                                        else errorMessage = authTask.exception?.localizedMessage
                                    }
                            } else {
                                isLoading = false
                                errorMessage = "Username not found"
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = e.localizedMessage
                        }
                } else {
                    // Standard Login: Email detected
                    auth.signInWithEmailAndPassword(input, password)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            if (authTask.isSuccessful) onLoginSuccess()
                            else errorMessage = authTask.exception?.localizedMessage
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B57D0)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Log In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Don't have an account? ", color = Color.Gray)
            Text("Sign Up", color = Color(0xFF0B57D0), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToSignup() })
        }
    }
}
