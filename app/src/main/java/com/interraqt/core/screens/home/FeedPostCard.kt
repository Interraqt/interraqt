package com.interraqt.core.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalFoundationApi::class)
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
    val context = LocalContext.current
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

    val toggleLike = {
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

    val toggleSave = {
        isSaved = !isSaved
        val saveRef = firestore.collection("users").document(currentUserId).collection("savedPosts").document(post.postId)
        if (isSaved) saveRef.set(mapOf("timestamp" to System.currentTimeMillis())) else saveRef.delete()
    }
    
    val toggleFollow = {
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userProfile.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(userProfile.profileImageUrl)
                            .crossfade(200)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Profile",
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(glassColor), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Default", tint = subTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(text = userProfile.username, fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                    if (shortTime.isNotEmpty()) Text(text = shortTime, color = subTextColor, fontSize = 12.sp)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isOwnProfile) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        color = if (isFollowing) textColor else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isFollowing) glassColor else primaryOrange)
                            .clickable { toggleFollow() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                IconButton(onClick = onOptionsClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "More", tint = textColor, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (post.mediaUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
            
            // 🚨 Removed manual touch block. Compose native Pager handles horizontal/vertical slop instantly.
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
                    beyondBoundsPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState) 
                ) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.mediaUrls[page])
                            .crossfade(200)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Post Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (post.mediaUrls.size > 1) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(post.mediaUrls.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.4f)
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 40.dp)
                        .clip(CircleShape)
                        .background(glassColor)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleLike() }
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        .padding(horizontal = if (localLikesCount > 0) 12.dp else 0.dp)
                ) {
                    Icon(HomeScreenIcons.Like, contentDescription = "Like", tint = if (isLiked) primaryOrange else textColor, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = likeScale; scaleY = likeScale })
                    if (localLikesCount > 0) Text(text = "$localLikesCount", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 40.dp)
                        .clip(CircleShape)
                        .background(glassColor)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCommentClick() }
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        .padding(horizontal = if (post.commentsCount > 0) 12.dp else 0.dp)
                ) {
                    Icon(HomeScreenIcons.Comment, contentDescription = "Comment", tint = textColor, modifier = Modifier.size(24.dp))
                    if (post.commentsCount > 0) Text(text = "${post.commentsCount}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                }
                
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor).clickable { }, contentAlignment = Alignment.Center) {
                    Icon(HomeScreenIcons.Share, contentDescription = "Share", tint = textColor, modifier = Modifier.size(24.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(glassColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleSave() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (isSaved) HomeScreenIcons.BookmarkFilled else HomeScreenIcons.BookmarkOutline, 
                    contentDescription = "Save", 
                    tint = if (isSaved) primaryOrange else textColor, 
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = saveScale; scaleY = saveScale }
                )
            }
        }

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
