package com.interraqt.core.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
fun SignupScreen(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } 
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance() 
    
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler { onNavigateToLogin() }

    val isDark = isSystemInDarkTheme()
    
    // 🚨 HYBRID THEME: Premium Dark Mode + Original Light Mode
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val fieldColor = if (isDark) Color(0xFF161C24) else Color.White
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
                    value = username, onValueChange = { username = it }, label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, 
                        focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color.White else primaryBlue, unfocusedLabelColor = if (isDark) Color.White else Color.DarkGray,
                        cursorColor = primaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email ID") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, 
                        focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color.White else primaryBlue, unfocusedLabelColor = if (isDark) Color.White else Color.DarkGray,
                        cursorColor = primaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.offset(x = -12.dp) 
                        ) {
                            Icon(imageVector = image, contentDescription = "Toggle Password", tint = if (isDark) Color.Gray else Color.DarkGray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = fieldColor, unfocusedBorderColor = Color.Transparent, 
                        focusedBorderColor = primaryBlue, focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color.White else primaryBlue, unfocusedLabelColor = if (isDark) Color.White else Color.DarkGray,
                        cursorColor = primaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                    singleLine = true
                )

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        
                        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid
                                if (userId != null) {
                                    val userData = hashMapOf(
                                        "username" to username.trim().lowercase(),
                                        "email" to email.trim()
                                    )
                                    
                                    firestore.collection("users").document(userId).set(userData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onSignupSuccess()
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Failed to save profile data.", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    isLoading = false
                                    onSignupSuccess()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, task.exception?.localizedMessage ?: "Signup Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    if (isLoading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) } 
                    else { Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, 
                            onClick = { onNavigateToLogin() }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp), 
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Already have an account? ", color = textColor, fontSize = 15.sp)
                    Text("Log In", color = primaryBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
