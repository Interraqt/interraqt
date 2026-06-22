package com.interraqt.core.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    val view = LocalView.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val primaryOrange = Color(0xFFFF6328)
    
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
    val highOpacityGlassColor = if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val liquidPickerBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fullscreenMediaUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }

    DisposableEffect(fullscreenMediaUri) {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        if (fullscreenMediaUri != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        if (fullscreenMediaUri != null) {
            fullscreenMediaUri = null
        } else if (!isPublishing) {
            onNavigateBack()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris -> 
            val availableSlots = 3 - selectedMediaUris.size
            if (availableSlots > 0) selectedMediaUris = selectedMediaUris + uris.take(availableSlots)
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris -> 
            val availableSlots = 3 - selectedMediaUris.size
            if (availableSlots > 0) selectedMediaUris = selectedMediaUris + uris.take(availableSlots)
        }
    )

    fun publishPost() {
        if (caption.text.trim().isEmpty() && selectedMediaUris.isEmpty()) {
            Toast.makeText(context, "Post cannot be empty", Toast.LENGTH_SHORT).show()
            return 
        }
        
        isPublishing = true
        coroutineScope.launch {
            val uploadedUrls = mutableListOf<String>()
            for (uri in selectedMediaUris) {
                val url = CloudflareManager.uploadImage(context, uri, isBanner = true)
                if (url != null) uploadedUrls.add(url)
            }

            if (selectedMediaUris.isNotEmpty() && uploadedUrls.isEmpty()) {
                isPublishing = false
                Toast.makeText(context, "Media upload failed", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val postId = UUID.randomUUID().toString()
            val postMap = hashMapOf(
                "postId" to postId,
                "userId" to currentUserId,
                "caption" to caption.text.trim(),
                "mediaUrls" to uploadedUrls, 
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
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp, 
                        shape = RoundedCornerShape(24.dp), 
                        ambientColor = Color.Black.copy(alpha = 0.05f), 
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(surfaceColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null 
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(top = 16.dp, bottom = 12.dp) // 🚨 Reduced bottom padding to perfectly hug the icons
            ) {
                Column {
                    if (selectedMediaUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            items(selectedMediaUris) { uri ->
                                Box(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(112.dp) 
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
                                        contentDescription = "Media Preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp)) 
                                            .clickable { 
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                fullscreenMediaUri = uri 
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable { selectedMediaUris = selectedMediaUris - uri },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    PostCaptionTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        maxLength = 1000, 
                        placeholderText = "What's happening?",
                        focusRequester = focusRequester,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 8.dp),
                        primaryColor = primaryOrange,
                        surfaceColor = surfaceColor, 
                        textColor = textColor,
                        subTextColor = Color.Gray
                    )

                    // 🚨 STREAMLINED BOTTOM BAR (Counter is aligned to the right!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // Pushes icons left, counter right
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(liquidPickerBg)
                                    .clickable { 
                                        if (selectedMediaUris.size < 3) {
                                            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        } else {
                                            Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 🚨 Modern Adaptive Outlined Icon
                                Icon(Icons.Outlined.Image, contentDescription = "Add Image", tint = textColor, modifier = Modifier.size(22.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(liquidPickerBg)
                                    .clickable { 
                                        if (selectedMediaUris.size < 3) {
                                            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                        } else {
                                            Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 🚨 Modern Adaptive Outlined Icon
                                Icon(Icons.Outlined.Videocam, contentDescription = "Add Video", tint = textColor, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        // 🚨 Horizontally Aligned Character Counter
                        Text(
                            text = "${caption.text.length}/1000",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }

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

        // 🚨 PERFECTLY SYMMETRICAL IMMERSIVE VIEWER ANIMATIONS
        AnimatedVisibility(
            visible = fullscreenMediaUri != null,
            enter = fadeIn(animationSpec = tween(250)) + scaleIn(animationSpec = tween(250), initialScale = 0.9f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250), targetScale = 0.9f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures { } } 
            ) {
                if (fullscreenMediaUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(fullscreenMediaUri).crossfade(true).build(),
                        contentDescription = "Fullscreen Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(highOpacityGlassColor)
                        .clickable { fullscreenMediaUri = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCaptionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    focusRequester: FocusRequester,
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
            maxLines = 6, 
            // 🚨 INTELLIGENT KEYBOARD AUTO-CAPITALIZATION
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused && !isFocused) { forceCursorToEnd = true; showHandle = false }
                    if (!state.isFocused) { showHandle = false; forceCursorToEnd = false }
                    isFocused = state.isFocused
                },
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = surfaceColor, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                focusedTextColor = textColor, unfocusedTextColor = textColor, cursorColor = primaryColor
            ),
            placeholder = { Text(placeholderText, color = subTextColor) }
            // Note: supportingText (Counter) is removed from here to perfectly align it in the Action Bar below!
        )
    }
}
