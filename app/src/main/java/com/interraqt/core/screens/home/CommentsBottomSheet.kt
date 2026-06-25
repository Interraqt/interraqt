package com.interraqt.core.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Send
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: FeedPost,
    currentUserId: String,
    firestore: FirebaseFirestore,
    textColor: Color, 
    subTextColor: Color,
    bgColor: Color, 
    primaryOrange: Color,
    glassColor: Color,
    onDismiss: () -> Unit,
    onCommentAdded: () -> Unit
) {
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    DisposableEffect(post.postId) {
        val listener = firestore.collection("posts").document(post.postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    comments = snapshot.documents.mapNotNull { it.toObject(PostComment::class.java) }
                }
            }
        onDispose { listener.remove() }
    }

    val submitComment = {
        if (commentText.isNotBlank()) {
            val commentId = UUID.randomUUID().toString()
            val commentMap = PostComment(commentId, currentUserId, commentText.trim(), System.currentTimeMillis())
            
            comments = comments + commentMap
            onCommentAdded()
            commentText = ""

            firestore.collection("posts").document(post.postId).collection("comments").document(commentId).set(commentMap)
            firestore.collection("posts").document(post.postId).update("commentsCount", FieldValue.increment(1))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor, 
        modifier = Modifier.wrapContentHeight(),
        dragHandle = { BottomSheetDefaults.DragHandle(color = subTextColor.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Comments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))
            
            LazyColumn(modifier = Modifier.weight(1f, fill = false).padding(horizontal = 16.dp)) {
                items(comments, key = { it.commentId }) { comment ->
                    var commenterName by remember { mutableStateOf("User") }
                    var commenterPic by remember { mutableStateOf("") }
                    
                    LaunchedEffect(comment.userId) {
                        firestore.collection("users").document(comment.userId).get().addOnSuccessListener { 
                            commenterName = it.getString("username") ?: "User"
                            commenterPic = it.getString("profileImageUrl") ?: ""
                        }
                    }
                    
                    Row(modifier = Modifier.padding(vertical = 12.dp)) {
                        if (commenterPic.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(commenterPic).crossfade(true).memoryCachePolicy(CachePolicy.ENABLED).build(), 
                                contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(glassColor), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "User", tint = subTextColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(commenterName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(comment.text, color = textColor, fontSize = 14.sp)
                        }
                    }
                }
                if (comments.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No comments yet. Be the first!", color = subTextColor) } }
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().background(bgColor).padding(16.dp).navigationBarsPadding().imePadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(CircleShape).background(glassColor).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = subTextColor) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryOrange),
                        modifier = Modifier.weight(1f)
                    )
                    if (commentText.isNotBlank()) {
                        IconButton(onClick = { submitComment() }) { Icon(Icons.Rounded.Send, contentDescription = "Post", tint = primaryOrange) }
                    }
                }
            }
        }
    }
}
