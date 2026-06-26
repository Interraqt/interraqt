package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import coil.request.CachePolicy
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMediaCarousel(mediaUrls: List<String>) {
    if (mediaUrls.isEmpty()) return
    
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { mediaUrls.size })
    
        val nestedScroll = remember(pagerState) {
        directionalScrollConnection(pagerState)
    }

    
    Box(modifier = Modifier.fillMaxWidth().nestedScroll(nestedScroll)) {
        HorizontalPager(
            state = pagerState,
            
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(Color.Black),
            beyondBoundsPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState) 
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUrls[page])
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Post Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit 
            )
        }

        if (mediaUrls.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
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
