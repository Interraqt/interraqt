package com.interraqt.core.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionsTab(isOwnProfile: Boolean, postsCount: Int, subTextColor: Color, surfaceColor: Color) {
    if (postsCount == 0) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (isOwnProfile) "The whole world is waiting for you to Interraqt, Share a moment" else "No posts yet.",
                color = subTextColor, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
            }
        }
    }
}
