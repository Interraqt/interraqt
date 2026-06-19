package com.interraqt.core.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var identifier by remember { mutableStateOf("") } 
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } 
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current 
    
    val auth = FirebaseAuth.getInstance() 
    val firestore = FirebaseFirestore.getInstance() 

    val interactionSource = remember { MutableInteractionSource() }

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF2A2A2A) else Color.White
    val primaryBlue = Color(0xFF0B57D0)

    Surface(
        color = bgColor, 
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            }
    ) {
        // 1. Forces the floating label's background mask to be 100% transparent
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = Color.Transparent 
            )
        ) {
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
                    label = { Text("Email or Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), 
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = fieldColor, 
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = primaryBlue, 
                        focusedTextColor = textColor, 
                        unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color.White else primaryBlue,
                        unfocusedLabelColor = if (isDark) Color.White else Color.DarkGray,
                        cursorColor = primaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            // 2. Physically offsets the icon to the left so it doesn't hug the right edge
                            modifier = Modifier.offset(x = (-12).dp) 
                        ) {
                            Icon(imageVector = image, contentDescription = "Toggle Password", tint = if (isDark) Color.Gray else Color.DarkGray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = fieldColor, 
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = primaryBlue, 
                        focusedTextColor = textColor, 
                        unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color.White else primaryBlue,
                        unfocusedLabelColor = if (isDark) Color.White else Color.DarkGray,
                        cursorColor = primaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                    singleLine = true
                )

                TextButton(
                    onClick = { 
                        if (identifier.isEmpty()) {
                            Toast.makeText(context, "Please enter your email in the top field first", Toast.LENGTH_LONG).show()
                        } else {
                            auth.sendPasswordResetEmail(identifier).addOnCompleteListener { task ->
                                if (task.isSuccessful) { Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show() } 
                                else { Toast.makeText(context, task.exception?.localizedMessage ?: "Error", Toast.LENGTH_LONG).show() }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Text("Forgot password?", color = if (isDark) Color.White else primaryBlue, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (identifier.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        val trimmedIdentifier = identifier.trim()

                        if (trimmedIdentifier.contains("@")) {
                            auth.signInWithEmailAndPassword(trimmedIdentifier, password).addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) { onLoginSuccess() } 
                                else { Toast.makeText(context, task.exception?.localizedMessage ?: "Login Failed", Toast.LENGTH_LONG).show() }
                            }
                        } else {
                            firestore.collection("users").whereEqualTo("username", trimmedIdentifier.lowercase())
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (!documents.isEmpty) {
                                        val linkedEmail = documents.documents[0].getString("email") ?: ""
                                        auth.signInWithEmailAndPassword(linkedEmail, password).addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) { onLoginSuccess() } 
                                            else { Toast.makeText(context, "Incorrect Password", Toast.LENGTH_LONG).show() }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Username not found", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Database Error", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    if (isLoading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) } 
                    else { Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, 
                            onClick = { onNavigateToSignup() }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp), 
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Don't have an account? ", color = textColor, fontSize = 15.sp)
                    Text("Sign Up", color = primaryBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
