package com.interraqt.core.screens.createpost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
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
fun MediaActionBar(
    captionLength: Int,
    mediaCount: Int,
    onAddImageClick: () -> Unit,
    onAddVideoClick: () -> Unit,
    liquidPickerBg: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(liquidPickerBg)
                    .clickable { onAddImageClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Image, contentDescription = "Add Image", tint = textColor, modifier = Modifier.size(22.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(liquidPickerBg)
                    .clickable { onAddVideoClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Videocam, contentDescription = "Add Video", tint = textColor, modifier = Modifier.size(24.dp))
            }
        }
        
        Text(
            text = "$captionLength/1000",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}
