package com.interraqt.core.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // 🚨 THIS IS THE FIX (Added missing import)
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.interraqt.core.network.CloudflareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val primaryOrange = Color(0xFFFF6328)

    // 🚨 UPDATED: Using TextFieldValue for the Smart Cursor logic
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }

    // 🚨 ADDED: BackHandler for the Android system back gesture
    BackHandler {
        if (!isPublishing) onNavigateBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    fun publishPost() {
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
            return 
        }
        
        isPublishing = true
        coroutineScope.launch {
            val imageUrl = CloudflareManager.uploadImage(context, selectedImageUri!!, isBanner = true)
            
            if (imageUrl != null) {
                val postId = UUID.randomUUID().toString()
                val postMap = hashMapOf(
                    "postId" to postId,
                    "userId" to currentUserId,
                    "caption" to caption.text.trim(), // Extracted text string
                    "imageUrl" to imageUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "likesCount" to 0,
                    "commentsCount" to 0
                )

                firestore.collection("posts").document(postId).set(postMap)
                    .addOnSuccessListener {
                        firestore.collection("users").document(currentUserId)
                            .update("postsCount", FieldValue.increment(1))
                            .addOnSuccessListener {
                                isPublishing = false
                                Toast.makeText(context, "Posted successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                    }
                    .addOnFailureListener {
                        isPublishing = false
                        Toast.makeText(context, "Failed to publish post", Toast.LENGTH_SHORT).show()
                    }
            } else {
                isPublishing = false
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            // 🚨 FIXED: pointerInput stops the screen from blinking grey on tap!
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp))

            // --- Image Selector Area ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square box
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor)
                    .clickable(enabled = !isPublishing) {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(selectedImageUri).crossfade(true).build(),
                        contentDescription = "Selected Post Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image", modifier = Modifier.size(48.dp), tint = primaryOrange)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to select photo", color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🚨 UPDATED: Replaced OutlinedTextField with your beautiful SmartCursorTextField logic
            PostCaptionTextField(
                value = caption,
                onValueChange = { caption = it },
                maxLength = 2200, // Standard Instagram limit
                placeholderText = "Write a caption...",
                modifier = Modifier.fillMaxWidth().height(150.dp),
                primaryColor = primaryOrange,
                surfaceColor = surfaceColor,
                textColor = textColor,
                subTextColor = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- Top Bar ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            IconButton(
                onClick = { if (!isPublishing) onNavigateBack() },
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            Text("New Post", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.Center))

            Box(modifier = Modifier.align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                TextButton(
                    onClick = { if (!isPublishing) { focusManager.clearFocus(); publishPost() } }, 
                    enabled = !isPublishing
                ) {
                    Text("Share", color = if (isPublishing) Color.Transparent else primaryOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (isPublishing) {
                    CircularProgressIndicator(color = primaryOrange, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                }
            }
        }
    }
}

// 🚨 SMART CURSOR LOGIC: Private implementation customized for the Post Screen with a Placeholder
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCaptionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    modifier: Modifier = Modifier,
    placeholderText: String,
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

    LaunchedEffect(isPressed) { if (isPressed && isFocused && !forceCursorToEnd) showHandle = true }
    LaunchedEffect(showHandle, value.selection) { if (showHandle) { delay(10000); showHandle = false } }

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
                    if (finalValue.text != value.text) showHandle = false 
                    onValueChange(finalValue)
                }
            },
            interactionSource = interactionSource,
            modifier = modifier.onFocusChanged { state ->
                if (state.isFocused && !isFocused) { forceCursorToEnd = true; showHandle = false }
                if (!state.isFocused) { showHandle = false; forceCursorToEnd = false }
                isFocused = state.isFocused
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = surfaceColor, focusedBorderColor = primaryColor, unfocusedBorderColor = Color.Transparent,
                focusedTextColor = textColor, unfocusedTextColor = textColor, cursorColor = primaryColor
            ),
            placeholder = { Text(placeholderText, color = subTextColor) },
            supportingText = { Text("${value.text.length}/$maxLength", color = subTextColor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) }
        )
    }
}
