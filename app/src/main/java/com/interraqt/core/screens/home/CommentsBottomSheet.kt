package com.interraqt.core.screens.home

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.UUID

// --- Data Models & Caching ---
data class UserProfile(val username: String, val profileImageUrl: String)

/**
 * Shared memory cache to prevent redundant Firestore fetches during rapid scrolling.
 */
object UserCache {
    private val cache = mutableMapOf<String, UserProfile>()
    
    fun get(userId: String): UserProfile? = cache[userId]
    fun put(userId: String, profile: UserProfile) { cache[userId] = profile }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // State management
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var pendingComments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Bottom Sheet Physics Configuration
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    // Real-time Firestore Listener
    DisposableEffect(post.postId) {
        val listener = firestore.collection("posts")
            .document(post.postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Fetch newest first for premium UI feel
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { it.toObject(PostComment::class.java) }
                    comments = fetched
                    
                    // Reconcile optimistic UI: Remove pending comments that have now arrived from the server
                    pendingComments = pendingComments.filter { pending -> 
                        fetched.none { it.commentId == pending.commentId }
                    }
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    // Submit Action with Optimistic UI Update
    val submitComment = { text: String ->
        if (text.isNotBlank()) {
            val commentId = UUID.randomUUID().toString()
            val newComment = PostComment(
                commentId = commentId, 
                userId = currentUserId, 
                text = text.trim(), 
                timestamp = System.currentTimeMillis()
            )
            
            // 1. Optimistic Update (Immediate Feedback)
            pendingComments = listOf(newComment) + pendingComments
            onCommentAdded()
            
            // Scroll to top to see new comment
            coroutineScope.launch { listState.animateScrollToItem(0) }

            // 2. Background Sync
            firestore.collection("posts").document(post.postId)
                .collection("comments").document(commentId)
                .set(newComment)
                .addOnFailureListener {
                    // Graceful rollback on failure
                    pendingComments = pendingComments.filterNot { it.commentId == commentId }
                }
                
            firestore.collection("posts").document(post.postId)
                .update("commentsCount", FieldValue.increment(1))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle(color = subTextColor.copy(alpha = 0.3f))
                Text(
                    text = "Comments", 
                    color = textColor, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(color = subTextColor.copy(alpha = 0.08f))
            }
        },
        modifier = Modifier.fillMaxHeight(0.9f) // Allows room for interaction
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- Comments Feed ---
            val allComments = pendingComments + comments
            
            if (isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(5) { ShimmerCommentPlaceholder(glassColor) }
                }
            } else if (allComments.isEmpty()) {
                EmptyCommentsState(textColor = textColor, subTextColor = subTextColor)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Leave room for composer
                ) {
                    items(allComments, key = { it.commentId }) { comment ->
                        CommentFeedItem(
                            comment = comment,
                            firestore = firestore,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            glassColor = glassColor,
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        )
                    }
                }
            }

            // --- Floating Comment Composer ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor, bgColor),
                            startY = 0f
                        )
                    )
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                PremiumCommentComposer(
                    glassColor = glassColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    primaryOrange = primaryOrange,
                    onSubmit = submitComment
                )
            }
        }
    }
}

@Composable
fun CommentFeedItem(
    comment: PostComment,
    firestore: FirebaseFirestore,
    textColor: Color,
    subTextColor: Color,
    glassColor: Color,
    modifier: Modifier = Modifier
) {
    var userProfile by remember { mutableStateOf(UserCache.get(comment.userId)) }

    // Fetch user profile only if missing from cache
    LaunchedEffect(comment.userId) {
        if (userProfile == null) {
            firestore.collection("users").document(comment.userId).get()
                .addOnSuccessListener { doc ->
                    val profile = UserProfile(
                        username = doc.getString("username") ?: "User",
                        profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    )
                    UserCache.put(comment.userId, profile)
                    userProfile = profile
                }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Avatar
        if (userProfile?.profileImageUrl?.isNotEmpty() == true) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfile?.profileImageUrl)
                    .crossfade(300)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(glassColor),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder initial
                Text(
                    text = userProfile?.username?.take(1)?.uppercase() ?: "",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userProfile?.username ?: "...", 
                    fontWeight = FontWeight.Bold, 
                    color = textColor, 
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatTimestamp(comment.timestamp), 
                    color = subTextColor, 
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.text, 
                color = textColor, 
                fontSize = 14.sp, 
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Reply", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {})
                Spacer(modifier = Modifier.width(24.dp))
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = subTextColor, modifier = Modifier.size(14.dp))
            }
        }
        
        // Like Button
        IconButton(
            onClick = { /* Add like functionality */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = subTextColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PremiumCommentComposer(
    glassColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryOrange: Color,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glassColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = textColor, fontSize = 15.sp),
            cursorBrush = SolidColor(primaryOrange),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text("Write a comment...", color = subTextColor, fontSize = 15.sp)
                }
                innerTextField()
            }
        )

        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryOrange.copy(alpha = 0.1f))
                    .clickable { 
                        onSubmit(text)
                        text = "" 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = primaryOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCommentsState(textColor: Color, subTextColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            tint = subTextColor.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No comments yet",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Be the first to start the conversation.", // Enhanced text flow
            color = subTextColor,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ShimmerCommentPlaceholder(glassColor: Color) {
    val shimmerColors = listOf(
        glassColor.copy(alpha = 0.6f),
        glassColor.copy(alpha = 0.2f),
        glassColor.copy(alpha = 0.6f),
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Row(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(brush))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.4f).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.9f).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.6f).clip(RoundedCornerShape(4.dp)).background(brush))
        }
    }
}

// --- Utilities ---
fun formatTimestamp(timeInMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timeInMillis
    return when {
        diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
        diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m"
        diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h"
        else -> "${diff / DateUtils.DAY_IN_MILLIS}d"
    }
}
