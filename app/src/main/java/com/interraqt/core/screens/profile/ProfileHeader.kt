package com.interraqt.core.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ProfileHeader(
    bannerImageUrl: String,
    profileImageUrl: String,
    displayName: String,
    bio: String,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color,
    statusBarHeightDp: Dp
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        
        if (bannerImageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(bannerImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize() 
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent, 
                                0.55f to Color.Transparent, 
                                0.65f to bgColor.copy(alpha = 0.25f),
                                0.85f to bgColor.copy(alpha = 0.85f),
                                0.95f to bgColor.copy(alpha = 0.98f),
                                1.0f to bgColor, 
                                startY = 0f, 
                                endY = size.height 
                            )
                        )
                    }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(statusBarHeightDp + 80.dp))

            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(surfaceColor), contentAlignment = Alignment.Center) {
                if (profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(profileImageUrl).crossfade(true).build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = subTextColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) 
        }
    }

    Text(text = displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
    if (bio.isNotEmpty()) {
        Text(text = bio, fontSize = 14.sp, color = subTextColor, modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp), textAlign = TextAlign.Center)
    }
    
    Spacer(modifier = Modifier.height(32.dp)) 

    val dividerBrush = Brush.verticalGradient(listOf(Color.Transparent, subTextColor.copy(alpha = 0.4f), Color.Transparent))

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(postsCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("Posts", fontSize = 12.sp, color = subTextColor)
        }
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(followersCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("Followers", fontSize = 12.sp, color = subTextColor)
        }
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(followingCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("Following", fontSize = 12.sp, color = subTextColor)
        }
    }
}
