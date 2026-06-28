package com.interraqt.core.screens.profile.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OtherUserProfileActions(
    isFollowing: Boolean,
    toggleFollow: () -> Unit,
    onMessageClick: () -> Unit,
    surfaceColor: Color,
    primaryOrange: Color,
    glassColor: Color,
    textColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { toggleFollow() },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) surfaceColor else primaryOrange)
        ) {
            Text(
                text = if (isFollowing) "Following" else "Follow", 
                color = if (isFollowing) textColor else Color.White, 
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = { onMessageClick() },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = glassColor)
        ) {
            Text("Message", color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}
