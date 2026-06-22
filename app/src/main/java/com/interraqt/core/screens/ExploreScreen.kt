package com.interraqt.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// 🚨 Added profileImageUrl to hold the image link from Firestore
data class UserSearchResult(val uid: String, val username: String, val name: String, val profileImageUrl: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToUserProfile: (String) -> Unit // 🚨 Passes the clicked user's ID out
) {
    val isDark = isSystemInDarkTheme()
    
    // 🚨 HYBRID THEME
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328)

    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // 🚨 REAL-TIME FIRESTORE SEARCH
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }

        isSearching = true
        try {
            val queryText = searchQuery.trim().lowercase()
            // Firebase "Starts With" search logic
            val snapshot = firestore.collection("users")
                .orderBy("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val username = doc.getString("username") ?: ""
                val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Update your name"
                val profileImageUrl = doc.getString("profileImageUrl") ?: "" // 🚨 Fetch the image URL
                
                // Don't show ourselves in the search results!
                if (uid != currentUserId) UserSearchResult(uid, username, name, profileImageUrl) else null
            }
            searchResults = results
        } catch (e: Exception) {
            // Silently handle exceptions to prevent crashing during typing
        } finally {
            isSearching = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            
            // --- SEARCH BAR ---
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search users (e.g. hello)", color = subTextColor) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = surfaceColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = primaryOrange,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = primaryOrange
                    ),
                    singleLine = true
                )
            }

            // --- SEARCH RESULTS LIST ---
            if (isSearching && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryOrange)
                }
            } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("No users found.", color = subTextColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(searchResults) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToUserProfile(user.uid) } // 🚨 Open Profile!
                                .background(surfaceColor)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            // 🚨 Replaced dummy icon with AsyncImage for real profile pictures
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(user.profileImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = subTextColor)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(user.name, fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                                Text("@${user.username}", color = subTextColor, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
