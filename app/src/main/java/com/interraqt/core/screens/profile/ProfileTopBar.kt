package com.interraqt.core.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileTopBar(
    isOwnProfile: Boolean,
    displayUsername: String,
    glassColor: Color,
    textColor: Color,
    onNavigateToCreatePost: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { 
                if (isOwnProfile) onNavigateToCreatePost() else onNavigateBack?.invoke() 
            },
            contentAlignment = Alignment.Center
        ) { 
            Icon(if (isOwnProfile) Icons.Default.Add else Icons.Default.ArrowBack, contentDescription = "Action", tint = textColor, modifier = Modifier.size(24.dp)) 
        }

        Text(
            text = displayUsername, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Normal, 
            color = textColor,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(glassColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { if (isOwnProfile) onNavigateToSettings() },
            contentAlignment = Alignment.Center
        ) {
            if (isOwnProfile) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                }
            } else {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = textColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}
