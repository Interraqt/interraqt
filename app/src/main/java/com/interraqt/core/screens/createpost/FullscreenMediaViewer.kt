package com.interraqt.core.screens.createpost

import androidx.compose.ui.zIndex
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    glassColor: Color,
    textColor: Color,
    statusBarHeight: Dp
) {
    val context = LocalContext.current

    // 🚨 PSEUDO-SHARED ELEMENT: Origin tracks the exact thumbnail tapped based on index
    val dynamicOrigin = remember(initialPage) {
        when (initialPage) {
            0 -> TransformOrigin(0.15f, 0.35f)
            1 -> TransformOrigin(0.5f, 0.35f)
            else -> TransformOrigin(0.85f, 0.35f)
        }
    }

    // 🚨 ROUNDED CORNER TRANSITION: Morphs from 16dp thumbnail to 0dp edge-to-edge
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isFullscreenVisible) 0.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "corner_radius"
    )

    AnimatedVisibility(
        visible = isFullscreenVisible,
        enter = fadeIn(animationSpec = tween(250)) + 
                scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), initialScale = 0.25f, transformOrigin = dynamicOrigin),
        exit = fadeOut(animationSpec = tween(250)) + 
               scaleOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), targetScale = 0.25f, transformOrigin = dynamicOrigin),
        modifier = Modifier.fillMaxSize().zIndex(10f) // Forces to top layer safely
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } } 
        ) {
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { selectedMedia.size })
            
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val mediaItem = selectedMedia[page]
                
                                if (mediaItem.isVideo) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val exoPlayer = remember { 
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE 
                                playWhenReady = true // 🚨 INSTANT PLAYBACK (No Retriever needed!)
                            } 
                        }
                        
                        DisposableEffect(mediaItem.uri) {
                            val media = androidx.media3.common.MediaItem.fromUri(mediaItem.uri)
                            exoPlayer.setMediaItem(media)
                            exoPlayer.prepare()
                            onDispose { 
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
                                exoPlayer.seekTo(0)
                            }
                        }
                        
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    useController = false 
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                            },
                            onRelease = { view ->
                                view.player = null // 🚨 CLEAN DETACHMENT: Prevents Memory Leak Crash
                            },
                            modifier = Modifier.fillMaxSize().background(Color.Transparent)
                        )
                    }
                }


                    Box(modifier = Modifier.fillMaxSize()) {
                        val exoPlayer = remember { 
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE 
                                playWhenReady = true // 🚨 INSTANT PLAYBACK PRELOAD
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
                                exoPlayer.seekTo(0) // Resets playback cleanly
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
                            modifier = Modifier.fillMaxSize().background(Color.Transparent)
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
            
            // 🚨 PROFILE STYLED BUTTON: Matched exactly to the Profile Screen overlay styles!
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarHeight + 16.dp, start = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable { onDismiss() }, 
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}
