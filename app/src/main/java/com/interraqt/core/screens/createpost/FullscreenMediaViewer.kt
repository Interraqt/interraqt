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
import android.view.TextureView
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

    AnimatedVisibility(
        visible = isFullscreenVisible,
        enter = fadeIn(animationSpec = tween(300)) + 
                scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), initialScale = 0.8f, transformOrigin = TransformOrigin.Center),
        exit = fadeOut(animationSpec = tween(250)) + 
               scaleOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow), targetScale = 0.8f, transformOrigin = TransformOrigin.Center),
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
                beyondBoundsPageCount = 1, // 🚨 FIX: Pre-loads 1 adjacent item so there are NO MORE BLACK SCREENS when you start swiping
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState) // 🚨 FIX: Adds fluid, premium swipe physics
            ) { page ->
                val mediaItem = selectedMedia[page]
                val isCurrentPage = pagerState.currentPage == page
                
                // 🚨 INSTAGRAM SWIPE EFFECT: Calculates distance from center and applies fade/scale
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
                        // 🚨 ExoPlayer is now allowed to prep off-screen so there is ZERO LAG when you arrive
                        val exoPlayer = remember { 
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE 
                                playWhenReady = true 
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
                            if (isCurrentPage && !isScrolling) {
                                exoPlayer.play()
                            } else {
                                exoPlayer.pause()
                                if (!isCurrentPage) exoPlayer.seekTo(0) // Cleanly resets adjacent videos to frame 0
                            }
                        }
                        
                        // 🚨 TEXTUREVIEW: Crucial for Xiaomi/Android devices so the `graphicsLayer` scale animation doesn't crash the hardware
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { textureView ->
                                exoPlayer.setVideoTextureView(textureView)
                            },
                            onRelease = { textureView ->
                                exoPlayer.clearVideoTextureView(textureView) 
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

            // 🚨 PROFILE STYLED BUTTON: Edge-to-Edge safe positioning
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
