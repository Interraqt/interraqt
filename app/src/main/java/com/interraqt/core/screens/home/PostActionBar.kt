package com.interraqt.core.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PostActionBar(
    isLiked: Boolean,
    localLikesCount: Int,
    isSaved: Boolean,
    commentsCount: Int,
    likeScale: Float,
    saveScale: Float,
    textColor: Color,
    primaryOrange: Color,
    glassColor: Color,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(40.dp)
                    .defaultMinSize(minWidth = 40.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleLike() }
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    .padding(horizontal = if (localLikesCount > 0) 12.dp else 0.dp)
            ) {
                Icon(HomeScreenIcons.Like, contentDescription = "Like", tint = if (isLiked) primaryOrange else textColor, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = likeScale; scaleY = likeScale })
                if (localLikesCount > 0) Text(text = "$localLikesCount", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(40.dp)
                    .defaultMinSize(minWidth = 40.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCommentClick() }
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    .padding(horizontal = if (commentsCount > 0) 12.dp else 0.dp)
            ) {
                Icon(HomeScreenIcons.Comment, contentDescription = "Comment", tint = textColor, modifier = Modifier.size(24.dp))
                if (commentsCount > 0) Text(text = "$commentsCount", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor).clickable { }, contentAlignment = Alignment.Center) {
                Icon(HomeScreenIcons.Share, contentDescription = "Share", tint = textColor, modifier = Modifier.size(24.dp))
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(glassColor)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleSave() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = if (isSaved) HomeScreenIcons.BookmarkFilled else HomeScreenIcons.BookmarkOutline, 
                contentDescription = "Save", 
                tint = if (isSaved) primaryOrange else textColor, 
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = saveScale; scaleY = saveScale }
            )
        }
    }
}
