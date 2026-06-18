package com.interraqt.core.auth

import android.widget.Toast
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var identifier by remember { mutableStateOf("") } 
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current // Used for popup error messages
    val auth = FirebaseAuth.getInstance() // Real Firebase Engine

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
                .imePadding() 
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Interraqt",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Email, Username, or Mobile") },
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
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            // REAL Forgot Password Logic
            TextButton(
                onClick = { 
                    if (identifier.isEmpty()) {
                        Toast.makeText(context, "Please enter your email in the top field first", Toast.LENGTH_LONG).show()
                    } else {
                        auth.sendPasswordResetEmail(identifier).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, task.exception?.localizedMessage ?: "Error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text("Forgot password?", color = primaryBlue, fontWeight = FontWeight.Medium)
            }

            // REAL Login Logic
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    
                    if (identifier.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    auth.signInWithEmailAndPassword(identifier, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, task.exception?.localizedMessage ?: "Login Failed", Toast.LENGTH_LONG).show()
                            }
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
