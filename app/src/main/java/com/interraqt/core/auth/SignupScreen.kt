package com.interraqt.core.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignupScreen(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    BackHandler { onNavigateToLogin() }

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val primaryBlue = Color(0xFF0B57D0)

    Scaffold(
        containerColor = bgColor,
        // Pins the arrow to the absolute top, pushed down safely below the battery/status bar
        topBar = {
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.statusBarsPadding().padding(top = 8.dp, start = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().imePadding().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = textColor, fontSize = 15.sp)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Log In", color = primaryBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Interraqt", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = textColor, modifier = Modifier.padding(bottom = 40.dp))
            
            // "Create account" moved down above the inputs
            Text("Create account", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp, start = 8.dp))

            // The Theme wrapper that fixes the label rectangle bug
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = fieldColor)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = fieldColor, unfocusedContainerColor = fieldColor, focusedIndicatorColor = primaryBlue, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true
                )

                OutlinedTextField(
                    value = username, onValueChange = { username = it }, label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = fieldColor, unfocusedContainerColor = fieldColor, focusedIndicatorColor = primaryBlue, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true
                )

                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email ID") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = fieldColor, unfocusedContainerColor = fieldColor, focusedIndicatorColor = primaryBlue, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true
                )

                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (Optional)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = fieldColor, unfocusedContainerColor = fieldColor, focusedIndicatorColor = primaryBlue, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true
                )

                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = fieldColor, unfocusedContainerColor = fieldColor, focusedIndicatorColor = primaryBlue, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }), singleLine = true
                )
            }

            // Real Firebase Signup
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (email.isEmpty() || password.isEmpty() || name.isEmpty() || username.isEmpty()) {
                        Toast.makeText(context, "Please fill out all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) onSignupSuccess()
                            else Toast.makeText(context, task.exception?.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
