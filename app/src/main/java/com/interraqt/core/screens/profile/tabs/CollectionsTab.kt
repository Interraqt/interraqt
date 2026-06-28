package com.interraqt.core.screens.profile.tabs

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.aspectRatio
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.interraqt.core.screens.home.FeedPost


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
fun CollectionsTab(
    targetUid: String, 
    firestore: FirebaseFirestore, 
    isOwnProfile: Boolean, 
    postsCount: Int, 
    subTextColor: Color, 
    surfaceColor: Color
) {
    // 🚨 State to hold the fetched posts
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }

    // 🚨 Firestore listener to fetch posts for this specific user
    LaunchedEffect(targetUid) {
        if (targetUid.isNotEmpty()) {
            firestore.collection("posts")
                .whereEqualTo("userId", targetUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        posts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                    }
                }
        }
    }

    
    if (postsCount == 0) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (isOwnProfile) "The whole world is waiting for you to Interraqt, Share a moment" else "No posts yet.",
                color = subTextColor, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
        } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 🚨 Splits the list of posts into groups of 3
            val rows = posts.chunked(3)
            
            rows.forEach { rowPosts ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 3) {
                        if (i < rowPosts.size) {
                            val post = rowPosts[i]
                            val imageUrl = post.mediaUrls.firstOrNull() ?: ""
                            
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Post preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f) // Automatically makes it a perfect square
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(surfaceColor)
                            )
                        } else {
                            // Invisible spacer to keep the grid perfectly aligned if a row has 1 or 2 items
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }

}
