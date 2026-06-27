package com.interraqt.core.screens.home

import kotlin.math.absoluteValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMediaCarousel(mediaUrls: List<String>) {
    if (mediaUrls.isEmpty()) return
    
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { mediaUrls.size })
    
    // Acquire native touch slop configuration
    val touchSlop = LocalViewConfiguration.current.touchSlop
    
    // Initialize the Gesture Lock State for this specific carousel instance
    val gestureLockState = remember { GestureLockState(touchSlop) }
    val nestedScrollConnection = rememberDirectionalScrollConnection(gestureLockState)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // The Resetter: Instantly catches the earliest moment of a physical touch.
            // We use 'requireUnconsumed = false' to passively observe, ensuring we do not 
            // block standard click events (like double-tap to like).
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    gestureLockState.reset()
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            // Native scrolling enabled at all times. Interruptions are handled seamlessly by Foundation.
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(Color.Black)
                .nestedScroll(nestedScrollConnection),
            beyondBoundsPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                // Highly responsive flick recognition
                snapPositionalThreshold = 0.1f,
                pagerSnapDistance = PagerSnapDistance.atMost(2),
                // Cinematic, weighted physical snap
                snapAnimationSpec = tween(
    durationMillis = 350, 
    easing = FastOutSlowInEasing
)
            ) 
        ) { page ->

            val isDark = isSystemInDarkTheme()
            val skeletonColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

            // High-performance image rendering pipeline
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUrls[page])
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Post Media",
             
                modifier = Modifier
    .fillMaxSize()
    .graphicsLayer {
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
        
        // 1. Fade out slightly as it leaves the screen
        alpha = 1f - absOffset
        
        // 2. Shrink it back by 15% to create a sense of distance
        val scale = 1f - (absOffset * 0.15f)
        scaleX = scale
        scaleY = scale
        
        // 3. Parallax scroll: Move the image at 50% speed so it looks "deeper" in the screen
        translationX = pageOffset * size.width * 0.5f
    },

              
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(skeletonColor) 
            )
        }

        if (mediaUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(mediaUrls.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.4f)
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}
