package com.interraqt.core.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun PostHeader(
    userProfile: FeedUserProfile,
    shortTime: String,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    textColor: Color,
    subTextColor: Color,
    primaryOrange: Color,
    glassColor: Color,
    onToggleFollow: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (userProfile.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(userProfile.profileImageUrl)
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Profile",
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(glassColor), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Default", tint = subTextColor)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(text = userProfile.username, fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 15.sp)
                if (shortTime.isNotEmpty()) Text(text = shortTime, color = subTextColor, fontSize = 12.sp)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isOwnProfile) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    color = if (isFollowing) textColor else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFollowing) glassColor else primaryOrange)
                        .clickable { onToggleFollow() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            IconButton(onClick = onOptionsClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More", tint = textColor, modifier = Modifier.size(30.dp))
            }
        }
    }
}
