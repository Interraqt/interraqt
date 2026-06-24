package com.interraqt.core.screens.createpost

com.interraqt.core.ui.components.SmartCursorTextField
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.interraqt.core.network.CloudflareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class MediaAttachment(val uri: Uri, val isVideo: Boolean)

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
    
    val highOpacityGlassColor = if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val liquidPickerBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    var selectedMedia by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var isFullscreenVisible by remember { mutableStateOf(false) }
    var showFullscreenDialog by remember { mutableStateOf(false) }
    var initialFullscreenPage by remember { mutableIntStateOf(0) }
    
    var caption by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }

    var isBoxFocused by remember { mutableStateOf(false) }
    var emptyBoxTapSecondTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isFullscreenVisible) {
        if (isFullscreenVisible) {
            showFullscreenDialog = true
        } else {
            delay(350) 
            showFullscreenDialog = false
        }
    }

    DisposableEffect(showFullscreenDialog) {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        if (showFullscreenDialog) {
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null 
                    ) {
                        if (!isBoxFocused) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            emptyBoxTapSecondTrigger++
                        }
                    }
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Column {
                    if (selectedMedia.isNotEmpty()) {
                        MediaPreviewCarousel(
                            selectedMedia = selectedMedia,
                            onMediaClick = { index ->
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                initialFullscreenPage = index
                                isFullscreenVisible = true
                            },
                            onRemoveMedia = { media ->
                                selectedMedia = selectedMedia - media
                            }
                        )
                    }

                    PostCaptionTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        maxLength = 1000, 
                        focusRequester = focusRequester,
                        onFocusStateChange = { isBoxFocused = it },
                        emptyBoxTapSecondTrigger = emptyBoxTapSecondTrigger,
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

                    MediaActionBar(
                        captionLength = caption.text.length,
                        mediaCount = selectedMedia.size,
                        onAddImageClick = {
                            if (selectedMedia.size < 3) {
                                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddVideoClick = {
                            if (selectedMedia.size < 3) {
                                videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            } else {
                                Toast.makeText(context, "Maximum 3 items allowed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        liquidPickerBg = liquidPickerBg,
                        textColor = textColor
                    )
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

        if (showFullscreenDialog) {
            FullscreenMediaViewer(
                isFullscreenVisible = isFullscreenVisible,
                selectedMedia = selectedMedia,
                initialPage = initialFullscreenPage,
                onDismiss = { isFullscreenVisible = false },
                isDark = isDark,
                highOpacityGlassColor = highOpacityGlassColor
            )
        }
    }
}
