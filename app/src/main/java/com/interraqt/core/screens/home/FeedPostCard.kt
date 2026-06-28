package com.interraqt.core.screens.home

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable


import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope() // 🚨 ADDED
    val likeAnimationState = rememberPremiumLikeState() // 🚨 ADDED

    
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
        
        // 🚨 ADDED: The bouncy pop animation!
        coroutineScope.launch {
            likeScale.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            likeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        }

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
        
        // 🚨 ADDED: The bouncy pop animation!
        coroutineScope.launch {
            saveScale.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            saveScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        }

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

        val likeScale = remember { Animatable(1f) }
    val saveScale = remember { Animatable(1f) }

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

                // 🚨 ADDED: Box to hold the carousel and the animation overlay
        Box(modifier = Modifier.fillMaxWidth()) {
            PostMediaCarousel(
                mediaUrls = post.mediaUrls,
                onDoubleTap = { tapOffset ->
                    // Trigger the visual animation instantly at the finger location
                    coroutineScope.launch { likeAnimationState.animate(tapOffset) }
                    
                    // Trigger the logical database like if it isn't already liked
                    if (!isLiked) toggleLike() 
                }
            )

            // 🚨 ADDED: The 120fps GPU overlay. It remains entirely invisible and consumes no resources when idle.
            PremiumLikeOverlay(state = likeAnimationState)
        }


                PostActionBar(
            isLiked = isLiked,
            localLikesCount = localLikesCount,
            isSaved = isSaved,
            commentsCount = post.commentsCount,
            likeScale = likeScale.value, // 🚨 Added .value
            saveScale = saveScale.value, // 🚨 Added .value

            textColor = textColor,
            primaryOrange = primaryOrange,
            glassColor = glassColor,
            onToggleLike = toggleLike,
            onToggleSave = toggleSave,
            onCommentClick = onCommentClick
        )

                if (post.caption.isNotBlank()) {
            var isExpanded by remember { mutableStateOf(false) }
            
            Text(
                text = post.caption, // 🚨 Removed username, just shows caption
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2, // 🚨 Hides extra text
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .animateContentSize()
                    .clickable { isExpanded = !isExpanded } // 🚨 Tap to read more!
            )
        }

    }
}
