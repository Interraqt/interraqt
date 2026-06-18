package com.interraqt.core.auth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var identifier by remember { mutableStateOf("") } // Email, Username, or Phone
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Theme Support
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val primaryBlue = Color(0xFF0B57D0)

    Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                // This makes the screen automatically lift up above the keyboard!
                .imePadding() 
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // The Wordmark
            Text(
                text = "Interraqt",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Identifier Field (Accepts both capital and lowercase)
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Email, Username, or Mobile") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp), // Perfectly round boxes
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = fieldColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = primaryBlue,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = fieldColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = primaryBlue,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                keyboardOptions = KeyboardOptions(
                    // Starts with a capital letter as requested
                    capitalization = KeyboardCapitalization.Sentences, 
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )

            // Forgot Password (Centered above button)
            TextButton(
                onClick = { /* Handle Password Reset */ },
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text("Forgot password?", color = primaryBlue, fontWeight = FontWeight.Medium)
            }

            // Login Button with Loading Animation
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    isLoading = true
                    coroutineScope.launch {
                        delay(1500) // Simulating network request
                        isLoading = false
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Anchored to the very bottom
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", color = textColor)
                TextButton(onClick = onNavigateToSignup) {
                    Text("Sign Up", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
