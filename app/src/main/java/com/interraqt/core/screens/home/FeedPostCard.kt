package com.interraqt.core.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FeedPostCard(
    post: FeedPost,
    userProfile: FeedUserProfile,
    currentUserId: String,
    shortTime: String,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryOrange: Color,
    glassColor: Color,
    firestore: FirebaseFirestore,
    onOptionsClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    val isOwnProfile = post.userId == currentUserId

    var isLiked by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var localLikesCount by remember { mutableIntStateOf(post.likesCount) }

    LaunchedEffect(post.postId) {
        if (currentUserId.isNotEmpty()) {
            firestore.collection("posts").document(post.postId).collection("likes").document(currentUserId).get().addOnSuccessListener { isLiked = it.exists() }
            firestore.collection("users").document(currentUserId).collection("savedPosts").document(post.postId).get().addOnSuccessListener { isSaved = it.exists() }
            if (!isOwnProfile) {
                firestore.collection("users").document(currentUserId).collection("following").document(post.userId).get().addOnSuccessListener { isFollowing = it.exists() }
            }
        }
    }

        // 🚨 Added ": () -> Unit" to explicitly tell Kotlin to ignore the Firebase return type
    val toggleLike: () -> Unit = {
        isLiked = !isLiked
        localLikesCount += if (isLiked) 1 else -1
        val postRef = firestore.collection("posts").document(post.postId)
        if (isLiked) {
            postRef.collection("likes").document(currentUserId).set(mapOf("timestamp" to System.currentTimeMillis()))
            postRef.update("likesCount", FieldValue.increment(1))
        } else {
            postRef.collection("likes").document(currentUserId).delete()
            postRef.update("likesCount", FieldValue.increment(-1))
        }
    }

    val toggleSave: () -> Unit = {
        isSaved = !isSaved
        val saveRef = firestore.collection("users").document(currentUserId).collection("savedPosts").document(post.postId)
        if (isSaved) saveRef.set(mapOf("timestamp" to System.currentTimeMillis())) else saveRef.delete()
    }
    
    val toggleFollow: () -> Unit = {
        isFollowing = !isFollowing
        val followingRef = firestore.collection("users").document(currentUserId).collection("following").document(post.userId)
        val followersRef = firestore.collection("users").document(post.userId).collection("followers").document(currentUserId)
        if (isFollowing) {
            followingRef.set(mapOf("timestamp" to System.currentTimeMillis()))
            followersRef.set(mapOf("timestamp" to System.currentTimeMillis()))
        } else {
            followingRef.delete()
            followersRef.delete()
        }
    }

    val likeScale by animateFloatAsState(targetValue = if (isLiked) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val saveScale by animateFloatAsState(targetValue = if (isSaved) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
        
        PostHeader(
            userProfile = userProfile,
            shortTime = shortTime,
            isOwnProfile = isOwnProfile,
            isFollowing = isFollowing,
            textColor = textColor,
            subTextColor = subTextColor,
            primaryOrange = primaryOrange,
            glassColor = glassColor,
            onToggleFollow = toggleFollow,
            onOptionsClick = onOptionsClick
        )

        PostMediaCarousel(mediaUrls = post.mediaUrls)

        PostActionBar(
            isLiked = isLiked,
            localLikesCount = localLikesCount,
            isSaved = isSaved,
            commentsCount = post.commentsCount,
            likeScale = likeScale,
            saveScale = saveScale,
            textColor = textColor,
            primaryOrange = primaryOrange,
            glassColor = glassColor,
            onToggleLike = toggleLike,
            onToggleSave = toggleSave,
            onCommentClick = onCommentClick
        )

        if (post.caption.isNotBlank()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(userProfile.username) }
                    append("  ")
                    append(post.caption)
                },
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
        }
    }
}
