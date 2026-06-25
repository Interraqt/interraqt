package com.interraqt.core.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    isOwnProfile: Boolean,
    textColor: Color, 
    bgColor: Color, 
    surfaceColor: Color,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 40.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onDeleteRequest(); onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Delete Post", color = Color(0xFFD32F2F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Copy link", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HomeScreenIcons.Interested, contentDescription = "Interested", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Interested", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HomeScreenIcons.NotInterested, contentDescription = "Not interested", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Not interested", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Flag, contentDescription = "Report", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Report", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
