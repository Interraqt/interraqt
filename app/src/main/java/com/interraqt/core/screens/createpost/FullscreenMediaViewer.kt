package com.interraqt.core.screens.createpost

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
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.absoluteValue

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

    // 🚨 RESTORED: Calculates origin point for the photo animation
    val dynamicOrigin = remember(initialPage) {
        when (initialPage) {
            0 -> TransformOrigin(0.15f, 0.35f)
            1 -> TransformOrigin(0.5f, 0.35f)
            else -> TransformOrigin(0.85f, 0.35f)
        }
    }

    // 🚨 SMART CHECK: Detects if you clicked a video or a photo
    val isOpeningVideo = remember(initialPage) { 
        selectedMedia.getOrNull(initialPage)?.isVideo == true 
    }

    AnimatedVisibility(
        visible = isFullscreenVisible,
        enter = if (isOpeningVideo) {
            fadeIn(tween(100)) // VIDEO: Quick, smooth fade
        } else {
            fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), initialScale = 0.8f, transformOrigin = dynamicOrigin) // PHOTO: Beautiful scale and bounce
        },
        exit = if (isOpeningVideo) {
            fadeOut(tween(100)) // VIDEO: Quick, smooth fade
        } else {
            fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow), targetScale = 0.8f, transformOrigin = dynamicOrigin) // PHOTO: Beautiful scale and bounce
        },
        modifier = Modifier.fillMaxSize().zIndex(10f) 
    ) {


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } } 
        ) {
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { selectedMedia.size })
            
            HorizontalPager(
                state = pagerState, 
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1, // Pre-loads 1 adjacent item for smooth swiping
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState) 
            ) { page ->
                val mediaItem = selectedMedia[page]
                val isCurrentPage = pagerState.currentPage == page
                
                // Instagram-style swipe animation
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val scale = 1f - (0.08f * pageOffset.absoluteValue.coerceIn(0f, 1f))
                val itemAlpha = 1f - (0.4f * pageOffset.absoluteValue.coerceIn(0f, 1f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = itemAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                                        if (mediaItem.isVideo) {
                        val exoPlayer = remember { 
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE 
                                // 🚨 INSTANT PLAY: Bypasses the Compose delay and commands the player to start immediately upon opening!
                                playWhenReady = isCurrentPage 
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
                        
                        val isScrolling = pagerState.isScrollInProgress
                        LaunchedEffect(isCurrentPage, isScrolling) {
                            // 🚨 FIX 2: Explicitly manage playback. Only plays if it's the exact center page and swiping has fully stopped.
                            if (isCurrentPage && !isScrolling) {
                                exoPlayer.play()
                            } else {
                                exoPlayer.pause()
                                if (!isCurrentPage) exoPlayer.seekTo(0)
                            }
                        }
                        
                        // 🚨 FIX 3: Restored PlayerView from Code 1 to perfectly handle aspect ratios (prevents stretching)
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
                                view.player = null 
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(mediaItem.uri).crossfade(true).build(),
                            contentDescription = "Fullscreen Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } 

            // Edge-to-Edge safe positioning for close button
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
