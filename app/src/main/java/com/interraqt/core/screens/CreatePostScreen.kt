package com.interraqt.core.screens

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.interraqt.core.network.CloudflareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class MediaAttachment(val uri: Uri, val isVideo: Boolean)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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

    var selectedMedia by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var isFullscreenVisible by remember { mutableStateOf(false) }
    var initialFullscreenPage by remember { mutableIntStateOf(0) }
    
    var caption by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }

    var isBoxFocused by remember { mutableStateOf(false) }
    var emptyBoxTapFirstFocusTrigger by remember { mutableIntStateOf(0) }
    var emptyBoxTapSecondTrigger by remember { mutableIntStateOf(0) }

    // 🚨 IMMERSIVE SYSTEM BAR HANDLER
    DisposableEffect(isFullscreenVisible) {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        if (isFullscreenVisible) {
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
        if (isFullscreenVisible) {
            isFullscreenVisible = false
        } else if (!isPublishing) {
            onNavigateBack()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris -> 
            val availableSlots = 3 - selectedMedia.size
            if (availableSlots > 0) selectedMedia = selectedMedia + uris.take(availableSlots).map { MediaAttachment(it, false) }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris -> 
            val availableSlots = 3 - selectedMedia.size
            if (availableSlots > 0) selectedMedia = selectedMedia + uris.take(availableSlots).map { MediaAttachment(it, true) }
        }
    )

    fun publishPost() {
        if (caption.text.trim().isEmpty() && selectedMedia.isEmpty()) {
            Toast.makeText(context, "Post cannot be empty", Toast.LENGTH_SHORT).show()
            return 
        }
        
        isPublishing = true
        coroutineScope.launch {
            val uploadedUrls = mutableListOf<String>()
            for (media in selectedMedia) {
                val url = CloudflareManager.uploadImage(context, media.uri, isBanner = true)
                if (url != null) uploadedUrls.add(url)
            }

            if (selectedMedia.isNotEmpty() && uploadedUrls.isEmpty()) {
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

    // 🚨 DYNAMIC SCROLL LOCK: Keeps box rigidly anchored until keyboard opens!
    val isImeVisible = WindowInsets.isImeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                // Scrolling disabled when keyboard closed = ZERO unwanted wiggle!
                .verticalScroll(state = scrollState, enabled = isImeVisible)
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
                    .pointerInput(isBoxFocused) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed }) {
                                    if (!isBoxFocused) {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                        emptyBoxTapFirstFocusTrigger++
                                    } else {
                                        emptyBoxTapSecondTrigger++
                                    }
                                }
                            }
                        }
                    }
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Column {
                    if (selectedMedia.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp) 
                        ) {
                            items(selectedMedia) { media ->
                                Box(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(112.dp) 
                                ) {
                                    if (media.isVideo) {
                                        var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
                                        var durationText by remember { mutableStateOf("") }

                                        LaunchedEffect(media.uri) {
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val retriever = MediaMetadataRetriever()
                                                    retriever.setDataSource(context, media.uri)
                                                    videoThumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                                    
                                                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                                                    if (durationMs > 0) {
                                                        val seconds = (durationMs / 1000) % 60
                                                        val minutes = (durationMs / 1000) / 60
                                                        durationText = String.format("%d:%02d", minutes, seconds)
                                                    }
                                                    retriever.release()
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }
                                        }

                                        AsyncImage(
                                            model = videoThumbnail,
                                            contentDescription = "Video Preview",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp)) 
                                                .clickable { 
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    initialFullscreenPage = selectedMedia.indexOf(media)
                                                    isFullscreenVisible = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.Black.copy(alpha = 0.2f))
                                                .clickable { 
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    initialFullscreenPage = selectedMedia.indexOf(media)
                                                    isFullscreenVisible = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.PlayCircle, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                                        }

                                        if (durationText.isNotEmpty()) {
                                            Text(
                                                text = durationText,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(media.uri).crossfade(true).build(),
                                            contentDescription = "Media Preview",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp)) 
                                                .clickable { 
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    initialFullscreenPage = selectedMedia.indexOf(media)
                                                    isFullscreenVisible = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable { selectedMedia = selectedMedia - media },
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
                        focusRequester = focusRequester,
                        onFocusStateChange = { isBoxFocused = it },
                        onFirstFocusTrigger = emptyBoxTapFirstFocusTrigger,
                        onSecondFocusTrigger = emptyBoxTapSecondTrigger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 8.dp),
                        placeholderText = "What's happening?",
                        primaryColor = primaryOrange,
                        surfaceColor = surfaceColor, 
                        textColor = textColor,
                        subTextColor = Color.Gray
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(liquidPickerBg)
                                    .clickable { 
                                        if (selectedMedia.size < 3) {
                                            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        } else {
                                            Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = "Add Image", tint = textColor, modifier = Modifier.size(22.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(liquidPickerBg)
                                    .clickable { 
                                        if (selectedMedia.size < 3) {
                                            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                        } else {
                                            Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Videocam, contentDescription = "Add Video", tint = textColor, modifier = Modifier.size(24.dp))
                            }
                        }
                        
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
            Spacer(modifier = Modifier.height(16.dp))
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

        // 🚨 THREADS PHYSICS: True Edge-to-Edge Root Overlay with Spring Animations!
        AnimatedVisibility(
            visible = isFullscreenVisible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                initialScale = 0.2f, 
                transformOrigin = TransformOrigin(0.15f, 0.35f) // Expands out from the thumbnail position!
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                animationSpec = spring(dampingRatio = 0.95f, stiffness = 350f),
                targetScale = 0.2f, 
                transformOrigin = TransformOrigin(0.15f, 0.35f)
            ),
            modifier = Modifier.fillMaxSize() // Covers status bar cleanly natively
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures { } } 
            ) {
                val pagerState = rememberPagerState(initialPage = initialFullscreenPage, pageCount = { selectedMedia.size })
                
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val mediaItem = selectedMedia[page]
                    
                    if (mediaItem.isVideo) {
                        var fullscreenVideoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
                        // 🚨 ZERO BLACK FLASH: Tracks the exact millisecond video pushes pixels
                        var isFirstFrameRendered by remember { mutableStateOf(false) }

                        LaunchedEffect(mediaItem.uri) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val retriever = MediaMetadataRetriever()
                                    retriever.setDataSource(context, mediaItem.uri)
                                    fullscreenVideoThumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    retriever.release()
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val exoPlayer = remember { 
                                androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE 
                                } 
                            }
                            
                            DisposableEffect(mediaItem.uri) {
                                isFirstFrameRendered = false
                                val media = androidx.media3.common.MediaItem.fromUri(mediaItem.uri)
                                val listener = object : androidx.media3.common.Player.Listener {
                                    override fun onRenderedFirstFrame() {
                                        isFirstFrameRendered = true
                                    }
                                }
                                exoPlayer.addListener(listener)
                                exoPlayer.setMediaItem(media)
                                exoPlayer.prepare()
                                onDispose { 
                                    exoPlayer.removeListener(listener)
                                    exoPlayer.release() 
                                }
                            }
                            
                            val isCurrentPage = pagerState.currentPage == page
                            val isScrolling = pagerState.isScrollInProgress
                            
                            LaunchedEffect(isCurrentPage, isScrolling) {
                                if (isCurrentPage && !isScrolling) {
                                    exoPlayer.play()
                                } else {
                                    exoPlayer.pause()
                                }
                            }
                            
                            AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false 
                                        // 🚨 Transparent shutter eliminates black loading screen
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // 🚨 Perfect Layering: High Quality Thumbnail stays visible until video physically moves
                            if (!isFirstFrameRendered) {
                                AsyncImage(
                                    model = fullscreenVideoThumbnail,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(mediaItem.uri).crossfade(true).build(),
                            contentDescription = "Fullscreen Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(highOpacityGlassColor)
                        .clickable { isFullscreenVisible = false }, 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PostCaptionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    focusRequester: FocusRequester,
    onFocusStateChange: (Boolean) -> Unit,
    onFirstFocusTrigger: Int,
    onSecondFocusTrigger: Int,
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
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Function to force scroll so the absolute bottom line is fully visible
    fun forceScrollToEnd() {
        coroutineScope.launch {
            delay(50) // Wait for layout measurement
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            delay(50) // Wait for text selection update
            bringIntoViewRequester.bringIntoView()
        }
    }

    // 🚨 1. FIRST TAP (Unfocused -> Focuses, Jumps to end, NO Handle)
    LaunchedEffect(onFirstFocusTrigger) {
        if (onFirstFocusTrigger > 0) {
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            showHandle = false
            forceScrollToEnd()
        }
    }

    // 🚨 2. SECOND TAP (Focused -> Jumps to end, SHOWS Handle)
    LaunchedEffect(onSecondFocusTrigger) {
        if (onSecondFocusTrigger > 0) {
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            showHandle = true
            forceScrollToEnd()
        }
    }

    // 🚨 3. DIRECT TEXT TAP (Places exactly at finger, SHOWS Handle)
    LaunchedEffect(isPressed) { 
        if (isPressed) { 
            showHandle = true 
            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
        } 
    }
    
    LaunchedEffect(showHandle, value.selection) { if (showHandle) { delay(10000); showHandle = false } }

    val customSelectionColors = TextSelectionColors(
        handleColor = if (showHandle || !value.selection.collapsed) primaryColor else Color.Transparent,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    // 🚨 INVISIBLE OVERLAY HACK: Intercepts the *very first tap* so the native field doesn't steal the cursor!
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.text.length <= maxLength) {
                        if (newValue.text != value.text) showHandle = false 
                        onValueChange(newValue)
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
                interactionSource = interactionSource,
                maxLines = 6, 
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        onFocusStateChange(state.isFocused)
                        if (!state.isFocused) showHandle = false
                    },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = surfaceColor, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textColor, unfocusedTextColor = textColor, cursorColor = primaryColor
                ),
                placeholder = { Text(placeholderText, color = subTextColor) }
            )
        }
        
        // This is the magic. It sits invisibly over the text field ONLY when closed.
        if (!isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Tapping the text field natively triggers the "First Tap" logic!
                        focusRequester.requestFocus()
                        onFocusStateChange(true)
                        // Trigger the jump-to-end logic in the parent
                        onValueChange(value.copy(selection = TextRange(value.text.length)))
                    }
            )
        }
    }
}
