package com.interraqt.core.screens.home

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring


import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.ColorPainter

import coil.request.CachePolicy
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMediaCarousel(mediaUrls: List<String>) {
    if (mediaUrls.isEmpty()) return
    
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { mediaUrls.size })
    
    // Remembers the fixed connection that resets cleanly onPreFling
    val nestedScrollConnection = rememberDirectionalScrollConnection(pagerState)

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
         
                                    modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(Color.Black)
                .nestedScroll(nestedScrollConnection),
            beyondBoundsPageCount = 1,
          
                                                            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.1f,
                // 🚨 FIX: Uses real physics instead of a robotic timer!
                // Calculates your thumb's actual speed for a butter-smooth glide, 
                // but applies "Medium Stiffness" brakes to stop the momentum quickly.
                // The lock is released instantly without ruining the animation!
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) 




        ) { page ->
            
                        val isDark = isSystemInDarkTheme()
            val skeletonColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

            // 🚨 FIX: Replaced SubcomposeAsyncImage with AsyncImage + ColorPainter.
            // Subcomposition causes heavy frame drops during rapid scrolls. 
            // ColorPainter draws the exact same skeleton UI directly on the GPU with zero lag!
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUrls[page])
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Post Media",
                modifier = Modifier.fillMaxSize(),
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
