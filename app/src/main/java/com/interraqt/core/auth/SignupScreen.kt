package com.interraqt.core.auth

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
fun SignupScreen(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // System Back Gesture Support
    BackHandler { onNavigateToLogin() }

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val primaryBlue = Color(0xFF0B57D0)

    Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            // Top Bar with Back Arrow
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Interraqt", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("Create account", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))
            }

            // Name Field (Capitalizes every word)
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            // Username Field (Always lowercase)
            OutlinedTextField(
                value = username, onValueChange = { username = it }, label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            // Email Field
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email ID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            // Phone Field (Optional)
            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (Optional)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            // Password Field (Starts with Capital)
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                singleLine = true
            )

            // Sign Up Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    isLoading = true
                    coroutineScope.launch {
                        delay(1500)
                        isLoading = false
                        onSignupSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Anchored to the very bottom
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = textColor)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Log In", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
