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
fun LoggedUserProfileActions(
    onNavigateToEditProfile: () -> Unit,
    shareProfile: () -> Unit,
    primaryOrange: Color,
    glassColor: Color,
    textColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { onNavigateToEditProfile() },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryOrange)
        ) {
            Text(
                text = "Edit Profile", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = { shareProfile() },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = glassColor)
        ) {
            Text("Share Profile", color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}
