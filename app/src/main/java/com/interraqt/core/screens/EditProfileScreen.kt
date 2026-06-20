package com.interraqt.core.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // 🚨 Added Coil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()

    val isDark = isSystemInDarkTheme()
    
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    
    val primaryOrange = Color(0xFFFF6328)

    var displayName by remember { mutableStateOf(TextFieldValue("")) }
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var bio by remember { mutableStateOf(TextFieldValue("")) }
    
    // 🚨 IMAGE UPLOAD STATES
    var profileImageUrl by remember { mutableStateOf("") }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEndPx = statusBarHeightPx + with(density) { 90.dp.toPx() }

    val isImeVisible = WindowInsets.isImeVisible
    
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
            coroutineScope.launch {
                delay(100)
                scrollState.animateScrollTo(0, animationSpec = tween(durationMillis = 300))
            }
        }
    }

    BackHandler { onNavigateBack() }

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                displayName = TextFieldValue(doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Update your name")
                username = TextFieldValue(doc.getString("username") ?: "")
                bio = TextFieldValue(doc.getString("bio")?.takeIf { it.isNotBlank() } ?: "Welcome to Interraqt! You can update your bio in the edit profile section.")
                // 🚨 Load existing profile picture
                profileImageUrl = doc.getString("profileImageUrl") ?: ""
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🚨 IMGBB UPLOAD LOGIC
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isUploadingImage = true
                coroutineScope.launch {
                    val url = uploadImageToImgbb(context, uri, "5433cd544443d393fe4dedcf078bac4c")
                    if (url != null) {
                        profileImageUrl = url
                        // Instantly update Firestore so it's not lost
                        currentUser?.uid?.let { uid ->
                            firestore.collection("users").document(uid).update("profileImageUrl", url)
                        }
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                    isUploadingImage = false
                }
            }
        }
    )

    val saveProfile: () -> Unit = {
        if (displayName.text.length > 24) {
            Toast.makeText(context, "Name cannot exceed 24 characters", Toast.LENGTH_SHORT).show()
        } else if (bio.text.length > 100) {
            Toast.makeText(context, "Bio cannot exceed 100 characters", Toast.LENGTH_SHORT).show()
        } else if (username.text.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
        } else {
            isSaving = true
            currentUser?.uid?.let { uid ->
                val updates = hashMapOf<String, Any>(
                    "name" to displayName.text.trim(),
                    "username" to username.text.trim().lowercase(),
                    "bio" to bio.text.trim(),
                    "profileImageUrl" to profileImageUrl // Also save here for redundancy
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() 
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
                    .verticalScroll(scrollState) 
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(statusBarHeightDp + 80.dp))

                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    // 🚨 AVATAR DISPLAY 
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(surfaceColor), contentAlignment = Alignment.Center) {
                        if (profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop // Fits image perfectly into the circle
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = subTextColor)
                        }

                        // Overlay a loading spinner if uploading
                        if (isUploadingImage) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = primaryOrange, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                    
                    // 🚨 CAMERA BUTTON
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(primaryOrange)
                            .clickable {
                                // Launch the native Android gallery picker
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text("Name", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                SmartCursorTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    maxLength = 24,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    primaryColor = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Username", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                SmartCursorTextField(
                    value = username,
                    onValueChange = { username = it },
                    maxLength = 18,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    primaryColor = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Bio", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                SmartCursorTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    maxLength = 100,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    singleLine = false,
                    primaryColor = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
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

// ====================================================================================
// 🚨 IMGBB NETWORK UPLOAD FUNCTION
// ====================================================================================
suspend fun uploadImageToImgbb(context: android.content.Context, uri: Uri, apiKey: String): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes != null) {
            val client = OkHttpClient()
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart(
                    "image", 
                    "profile_pic.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody ?: "")
                val data = jsonObject.getJSONObject("data")
                return@withContext data.getString("url") // Returns the live image link!
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}

// ====================================================================================
// THE SMART CURSOR STATE MACHINE
// ====================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCursorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    var showHandle by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var forceCursorToEnd by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed && isFocused && !forceCursorToEnd) {
            showHandle = true
        }
    }

    LaunchedEffect(showHandle, value.selection) {
        if (showHandle) {
            delay(10000)
            showHandle = false
        }
    }

    val customSelectionColors = TextSelectionColors(
        handleColor = if (showHandle || !value.selection.collapsed) primaryColor else Color.Transparent,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.text.length <= maxLength) {
                    var finalValue = newValue
                    
                    if (forceCursorToEnd) {
                        finalValue = finalValue.copy(selection = TextRange(finalValue.text.length))
                        forceCursorToEnd = false 
                    }

                    val textChanged = finalValue.text != value.text
                    if (textChanged) {
                        showHandle = false 
                    }
                    onValueChange(finalValue)
                }
            },
            interactionSource = interactionSource,
            modifier = modifier.onFocusChanged { state ->
                if (state.isFocused && !isFocused) {
                    forceCursorToEnd = true
                    showHandle = false
                }
                if (!state.isFocused) {
                    showHandle = false
                    forceCursorToEnd = false 
                }
                isFocused = state.isFocused
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp, 
                color = textColor
            ),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = surfaceColor,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = primaryColor
            ),
            singleLine = singleLine,
            supportingText = {
                Text(
                    "${value.text.length}/$maxLength",
                    color = subTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        )
    }
}
