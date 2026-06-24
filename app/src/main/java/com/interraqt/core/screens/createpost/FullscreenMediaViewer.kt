package com.interraqt.core.screens.createpost

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenMediaViewer(
    isFullscreenVisible: Boolean,
    selectedMedia: List<MediaAttachment>,
    initialPage: Int,
    onDismiss: () -> Unit,
    isDark: Boolean,
    highOpacityGlassColor: Color
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        AnimatedVisibility(
            visible = isFullscreenVisible,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                    scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), initialScale = 0.85f, transformOrigin = TransformOrigin(0.15f, 0.35f)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                   scaleOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), targetScale = 0.85f, transformOrigin = TransformOrigin(0.15f, 0.35f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures { } } 
            ) {
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { selectedMedia.size })
                
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val mediaItem = selectedMedia[page]
                    
                    if (mediaItem.isVideo) {
                        var fullscreenVideoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
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
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize().background(Color.Black)
                            )

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
                            model = ImageRequest.Builder(context).data(mediaItem.uri).crossfade(true).build(),
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
                        .clickable { onDismiss() }, 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
