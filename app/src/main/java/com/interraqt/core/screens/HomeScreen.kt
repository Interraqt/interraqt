package com.interraqt.core.screens

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay

// Data Models
data class FeedPost(
    val postId: String = "",
    val userId: String = "",
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

data class FeedUserProfile(
    val username: String = "Unknown",
    val profileImageUrl: String = ""
)

data class PostComment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToCreatePost: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328)
    val glassColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var usersMap by remember { mutableStateOf<Map<String, FeedUserProfile>>(emptyMap()) }
    
    var lastVisible by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    var showOptionsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showCommentsForPost by remember { mutableStateOf<FeedPost?>(null) }
    
    val pullRefreshState = rememberPullToRefreshState()

    fun getShortTime(time: Long): String {
        if (time == 0L) return ""
        val diff = System.currentTimeMillis() - time
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "Now"
        }
    }

    fun loadPosts(isRefresh: Boolean = false) {
        if (isLoadingMore || (!hasMore && !isRefresh)) return
        isLoadingMore = true
        
        var query = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            
        if (!isRefresh && lastVisible != null) query = query.startAfter(lastVisible!!)

        query.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                hasMore = false
            } else {
                lastVisible = snapshot.documents.last()
                val newPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                posts = if (isRefresh) newPosts else posts + newPosts
                
                val missingUsers = newPosts.map { it.userId }.distinct().filter { !usersMap.containsKey(it) }
                missingUsers.forEach { uid ->
                    firestore.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                        usersMap = usersMap + (uid to FeedUserProfile(username, profileImageUrl))
                    }
                }
            }
            isLoadingMore = false
            if (isRefresh) pullRefreshState.endRefresh()
        }.addOnFailureListener {
            isLoadingMore = false
            if (isRefresh) pullRefreshState.endRefresh()
        }
    }

    LaunchedEffect(Unit) { loadPosts(isRefresh = true) }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { loadPosts(isRefresh = true) }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && totalItems > 0 && lastVisibleItem >= totalItems - 2
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) loadPosts() }

    val density = LocalDensity.current
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        // 1. MAIN FEED (No Feathering, Fluid Scroll restored)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusBarHeightDp + 64.dp, bottom = 100.dp) 
        ) {
            item {
                MomentsTray(textColor = textColor, subTextColor = subTextColor, primaryOrange = primaryOrange)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), thickness = 0.5.dp)
            }

            items(posts, key = { it.postId }) { post ->
                val userProfile = usersMap[post.userId] ?: FeedUserProfile()
                
                FeedPostCard(
                    post = post,
                    userProfile = userProfile,
                    currentUserId = currentUserId,
                    shortTime = getShortTime(post.timestamp),
                    bgColor = bgColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    primaryOrange = primaryOrange,
                    glassColor = glassColor,
                    firestore = firestore,
                    onOptionsClick = { showOptionsForPost = post },
                    onCommentClick = { showCommentsForPost = post }
                )
                
                HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), thickness = 0.5.dp)
            }
            
            if (isLoadingMore && posts.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryOrange, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        // 2. PULL TO REFRESH (Slides seamlessly from under the Top Bar)
        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp + 64.dp), 
            containerColor = glassColor, 
            contentColor = textColor
        )

        // 3. TOP BAR (Highest Z-Index, Solid background)
        Box(
            modifier = Modifier
                .zIndex(2f) // 🚨 Forces the top bar above everything, including the refresh indicator
                .fillMaxWidth()
                .background(bgColor) // Solid to hide refreshing behind it
                .padding(top = statusBarHeightDp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToCreatePost() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, contentDescription = "Create", tint = textColor, modifier = Modifier.size(24.dp)) }

                Text("Interraqt", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)

                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor).clickable { },
                    contentAlignment = Alignment.Center
                ) { Icon(CustomIcons.HeartOutline, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(22.dp)) }
            }
        }

        // 4. BOTTOM SHEETS
        if (showOptionsForPost != null) {
            PostOptionsBottomSheet(textColor = textColor, bgColor = bgColor, onDismiss = { showOptionsForPost = null })
        }
        
        if (showCommentsForPost != null) {
            CommentsBottomSheet(
                post = showCommentsForPost!!,
                currentUserId = currentUserId,
                firestore = firestore,
                textColor = textColor, 
                subTextColor = subTextColor,
                bgColor = bgColor, 
                primaryOrange = primaryOrange,
                glassColor = glassColor,
                onDismiss = { showCommentsForPost = null }
            )
        }
    }
}

@Composable
fun MomentsTray(textColor: Color, subTextColor: Color, primaryOrange: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Moments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(65.dp).clip(CircleShape).border(2.dp, subTextColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = textColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add", color = subTextColor, fontSize = 12.sp)
                }
            }
        }
    }
}

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

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userProfile.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(userProfile.profileImageUrl).crossfade(true).build(),
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor), contentAlignment = Alignment.Center) {
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
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isFollowing) glassColor else primaryOrange)
                            .clickable { toggleFollow() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onOptionsClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "More", tint = textColor)
                }
            }
        }

        // Media Carousel (Pinch-to-zoom completely removed for perfectly fluid feed swiping)
        if (post.mediaUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
                    beyondBoundsPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState) 
                ) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(post.mediaUrls[page]).crossfade(true).build(),
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

        // Action Bar (Using the mathematical Pill Algorithm)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionPill(
                    icon = if (isLiked) Icons.Rounded.Favorite else CustomIcons.HeartOutline,
                    count = localLikesCount,
                    isActive = isLiked,
                    activeColor = primaryOrange,
                    inactiveColor = textColor,
                    glassColor = glassColor,
                    onClick = toggleLike
                )
                ActionPill(
                    icon = CustomIcons.Comment,
                    count = post.commentsCount,
                    isActive = false,
                    activeColor = textColor,
                    inactiveColor = textColor,
                    glassColor = glassColor,
                    onClick = onCommentClick
                )
                ActionPill(
                    icon = CustomIcons.Share,
                    count = 0,
                    isActive = false,
                    activeColor = textColor,
                    inactiveColor = textColor,
                    glassColor = glassColor,
                    onClick = { /* Share Logic */ }
                )
            }
            ActionPill(
                icon = if (isSaved) CustomIcons.SaveFilled else CustomIcons.SaveOutline,
                count = 0,
                isActive = isSaved,
                activeColor = primaryOrange,
                inactiveColor = textColor,
                glassColor = glassColor,
                onClick = toggleSave
            )
        }

        // Caption
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

// ALGORITHM: Flawless Circle to Pill Transformation
@Composable
fun ActionPill(
    icon: ImageVector, count: Int, isActive: Boolean, activeColor: Color, inactiveColor: Color, glassColor: Color, onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (isActive) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bounce")
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(glassColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            .padding(start = if (count > 0) 12.dp else 0.dp, end = if (count > 0) 16.dp else 0.dp)
    ) {
        Box(modifier = Modifier.size(if (count > 0) 24.dp else 40.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isActive) activeColor else inactiveColor, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        }
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$count", color = inactiveColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// PREMIUM BOTTOM SHEETS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(textColor: Color, bgColor: Color, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            val options = listOf(
                Icons.Rounded.ContentCopy to "Copy link",
                CustomIcons.SaveOutline to "Save",
                CustomIcons.Show to "Interested",
                CustomIcons.Hide to "Not interested",
                Icons.Rounded.Flag to "Report"
            )
            options.forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = text, tint = textColor, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: FeedPost, currentUserId: String, firestore: FirebaseFirestore, textColor: Color, subTextColor: Color, bgColor: Color, primaryOrange: Color, glassColor: Color, onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    DisposableEffect(post.postId) {
        val listener = firestore.collection("posts").document(post.postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) comments = snapshot.documents.mapNotNull { it.toObject(PostComment::class.java) }
            }
        onDispose { listener.remove() }
    }

    val submitComment = {
        if (commentText.isNotBlank()) {
            val commentId = java.util.UUID.randomUUID().toString()
            val commentMap = PostComment(commentId, currentUserId, commentText.trim(), System.currentTimeMillis())
            firestore.collection("posts").document(post.postId).collection("comments").document(commentId).set(commentMap)
            firestore.collection("posts").document(post.postId).update("commentsCount", FieldValue.increment(1))
            commentText = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor, 
        modifier = Modifier.fillMaxHeight(0.85f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = subTextColor.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Comments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))
            
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
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
                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(commenterPic).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
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
            
            // Sleek Floating Input Box
            Box(modifier = Modifier.fillMaxWidth().background(bgColor).padding(16.dp).navigationBarsPadding().imePadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(CircleShape).background(glassColor).padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = subTextColor) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryOrange),
                        modifier = Modifier.weight(1f)
                    )
                    if (commentText.isNotBlank()) {
                        IconButton(onClick = submitComment, modifier = Modifier.size(40.dp).clip(CircleShape).background(primaryOrange.copy(alpha = 0.1f))) { 
                            Icon(CustomIcons.Share, contentDescription = "Post", tint = primaryOrange, modifier = Modifier.size(20.dp)) 
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// CUSTOM RAW SVG ICONS (GPU Optimized)
// --------------------------------------------------------
object CustomIcons {
    val HeartOutline = ImageVector.Builder("HeartOutline", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Square) {
            moveTo(21.4999f, 9.63556f)
            curveTo(21.49f, 7.09969f, 20.1596f, 4.71489f, 17.5366f, 3.86991f)
            curveTo(15.7355f, 3.28869f, 13.7736f, 3.61191f, 12.25f, 5.79939f)
            curveTo(10.7264f, 3.61191f, 8.76447f, 3.28869f, 6.96339f, 3.86991f)
            curveTo(4.34014f, 4.71498f, 3.00971f, 7.10024f, 3.00008f, 9.63643f)
            curveTo(2.97582f, 14.6801f, 8.08662f, 18.5397f, 12.2487f, 20.3844f)
            lineTo(12.25f, 20.3838f)
            lineTo(12.2513f, 20.3844f)
            curveTo(16.4136f, 18.5396f, 21.5248f, 14.6797f, 21.4999f, 9.63556f)
            close()
        }
    }.build()

    val SaveOutline = ImageVector.Builder("SaveOutline", 24.dp, 24.dp, 24f, 24f).apply {
        group(translationX = 4.2f, translationY = 2.3f) {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15.5388f, 3.8536f)
                curveTo(15.5388f, 1.1027f, 13.6581f, 0f, 10.9503f, 0f)
                lineTo(4.5914f, 0f)
                curveTo(1.9669f, 0f, 0f, 1.0275f, 0f, 3.6701f)
                lineTo(0f, 18.3939f)
                curveTo(0f, 19.1197f, 0.7809f, 19.5769f, 1.4135f, 19.2220f)
                lineTo(7.7954f, 15.6421f)
                lineTo(14.1223f, 19.2160f)
                curveTo(14.7558f, 19.5729f, 15.5388f, 19.1157f, 15.5388f, 18.3889f)
                lineTo(15.5388f, 3.8536f)
                close()
                moveTo(4.0711f, 6.728f)
                lineTo(11.3894f, 6.728f)
            }
        }
    }.build()

    val SaveFilled = ImageVector.Builder("SaveFilled", 24.dp, 24.dp, 24f, 24f).apply {
        group(translationX = 4.0f, translationY = 2.0f) {
            path(fill = SolidColor(Color.Black), fillAlpha = 0.4f) {
                moveTo(7.9911f, 16.6215f); lineTo(1.4994f, 19.8641f)
                curveTo(1.0092f, 20.1302f, 0.3976f, 19.9525f, 0.1234f, 19.4643f)
                curveTo(0.0434f, 19.3108f, 0.0010f, 19.1402f, 0f, 18.9668f)
                lineTo(0f, 11.7088f)
                curveTo(0f, 12.4283f, 0.4057f, 12.8725f, 1.4729f, 13.3700f)
                lineTo(7.9911f, 16.6215f); close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.0694f, 0f); curveTo(13.7772f, 0f, 15.9735f, 1.0660f, 16f, 3.7933f)
                lineTo(16f, 3.7933f); lineTo(16f, 18.9668f)
                curveTo(15.9989f, 19.1374f, 15.9565f, 19.3051f, 15.8765f, 19.4554f)
                curveTo(15.7479f, 19.7006f, 15.5259f, 19.8826f, 15.2614f, 19.9597f)
                curveTo(14.9969f, 20.0368f, 14.7127f, 20.0022f, 14.4740f, 19.8641f)
                lineTo(14.4740f, 19.8641f); lineTo(7.9911f, 16.6215f); lineTo(1.4729f, 13.3700f)
                curveTo(0.4057f, 12.8725f, 0f, 12.4283f, 0f, 11.7088f)
                lineTo(0f, 11.7088f); lineTo(0f, 3.7933f)
                curveTo(0f, 1.0660f, 2.1962f, 0f, 4.8952f, 0f)
                lineTo(4.8952f, 0f); close()
                moveTo(11.7486f, 6.0409f); lineTo(4.2249f, 6.0409f)
                curveTo(3.7913f, 6.0409f, 3.4399f, 6.3949f, 3.4399f, 6.8316f)
                curveTo(3.4399f, 7.2682f, 3.7913f, 7.6222f, 4.2249f, 7.6222f)
                lineTo(4.2249f, 7.6222f); lineTo(11.7486f, 7.6222f)
                curveTo(12.1821f, 7.6222f, 12.5336f, 7.2682f, 12.5336f, 6.8316f)
                curveTo(12.5336f, 6.3949f, 12.1821f, 6.0409f, 11.7486f, 6.0409f)
                lineTo(11.7486f, 6.0409f); close()
            }
        }
    }.build()

    val Comment = ImageVector.Builder("Comment", 24.dp, 24.dp, 24f, 24f).apply {
        group(translationX = 1.0f, translationY = 1.0f) {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.7484f, 0.0003f)
                curveTo(13.6214f, 0.0003f, 16.3214f, 1.1173f, 18.3494f, 3.1463f)
                curveTo(22.5414f, 7.3383f, 22.5414f, 14.1583f, 18.3494f, 18.3503f)
                curveTo(16.2944f, 20.4063f, 13.5274f, 21.4943f, 10.7244f, 21.4943f)
                curveTo(9.1964f, 21.4943f, 7.6584f, 21.1713f, 6.2194f, 20.5053f)
                curveTo(5.7954f, 20.3353f, 5.3984f, 20.1753f, 5.1134f, 20.1753f)
                curveTo(4.7854f, 20.1773f, 4.3444f, 20.3293f, 3.9184f, 20.4763f)
                curveTo(3.0444f, 20.7763f, 1.9564f, 21.1503f, 1.1514f, 20.3483f)
                curveTo(0.3494f, 19.5453f, 0.7194f, 18.4603f, 1.0174f, 17.5873f)
                curveTo(1.1644f, 17.1573f, 1.3154f, 16.7133f, 1.3154f, 16.3773f)
                curveTo(1.3154f, 16.1013f, 1.1824f, 15.7493f, 0.9784f, 15.2423f)
                curveTo(-0.8946f, 11.1973f, -0.0286f, 6.3223f, 3.1484f, 3.1473f)
                curveTo(5.1764f, 1.1183f, 7.8754f, 0.0003f, 10.7484f, 0.0003f); close()
                moveTo(10.7494f, 1.5003f)
                curveTo(8.2764f, 1.5003f, 5.9534f, 2.4623f, 4.2084f, 4.2083f)
                curveTo(1.4744f, 6.9403f, 0.7304f, 11.1353f, 2.3554f, 14.6483f)
                curveTo(2.5894f, 15.2273f, 2.8154f, 15.7913f, 2.8154f, 16.3773f)
                curveTo(2.8154f, 16.9623f, 2.6144f, 17.5513f, 2.4374f, 18.0713f)
                curveTo(2.2914f, 18.4993f, 2.0704f, 19.1453f, 2.2124f, 19.2873f)
                curveTo(2.3514f, 19.4313f, 3.0014f, 19.2043f, 3.4304f, 19.0573f)
                curveTo(3.9454f, 18.8813f, 4.5294f, 18.6793f, 5.1084f, 18.6753f)
                curveTo(5.6884f, 18.6753f, 6.2354f, 18.8953f, 6.8144f, 19.1283f)
                curveTo(10.3614f, 20.7683f, 14.5564f, 20.0223f, 17.2894f, 17.2903f)
                curveTo(20.8954f, 13.6823f, 20.8954f, 7.8133f, 17.2894f, 4.2073f)
                curveTo(15.5434f, 2.4613f, 13.2214f, 1.5003f, 10.7494f, 1.5003f); close()
                moveTo(14.6963f, 10.163f)
                curveTo(15.2483f, 10.163f, 15.6963f, 10.61f, 15.6963f, 11.163f)
                curveTo(15.6963f, 11.716f, 15.2483f, 12.163f, 14.6963f, 12.163f)
                curveTo(14.1443f, 12.163f, 13.6923f, 11.716f, 13.6923f, 11.163f)
                curveTo(13.6923f, 10.61f, 14.1353f, 10.163f, 14.6873f, 10.163f)
                lineTo(14.6963f, 10.163f); close()
                moveTo(10.6875f, 10.163f)
                curveTo(11.2395f, 10.163f, 11.6875f, 10.61f, 11.6875f, 11.163f)
                curveTo(11.6875f, 11.716f, 11.2395f, 12.163f, 10.6875f, 12.163f)
                curveTo(10.1355f, 12.163f, 9.6835f, 11.716f, 9.6835f, 11.163f)
                curveTo(9.6835f, 10.61f, 10.1255f, 10.163f, 10.6785f, 10.163f)
                lineTo(10.6875f, 10.163f); close()
                moveTo(6.6783f, 10.163f)
                curveTo(7.2303f, 10.163f, 7.6783f, 10.61f, 7.6783f, 11.163f)
                curveTo(7.6783f, 11.716f, 7.2303f, 12.163f, 6.6783f, 12.163f)
                curveTo(6.1263f, 12.163f, 5.6743f, 11.716f, 5.6743f, 11.163f)
                curveTo(5.6743f, 10.61f, 6.1173f, 10.163f, 6.6693f, 10.163f)
                lineTo(6.6783f, 10.163f); close()
            }
        }
    }.build()

    val Share = ImageVector.Builder("Share", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(11.4933f, 12.4382f)
            curveTo(11.4933f, 12.4382f, -0.483351f, 9.96062f, 3.6786f, 7.55807f)
            curveTo(7.19075f, 5.53077f, 19.2947f, 2.04522f, 20.9857f, 2.94582f)
            curveTo(21.8863f, 4.63682f, 18.4007f, 16.7408f, 16.3734f, 20.2529f)
            curveTo(13.9709f, 24.4149f, 11.4933f, 12.4382f, 11.4933f, 12.4382f)
            close()
            moveTo(11.4934f, 12.4382f)
            lineTo(20.9858f, 2.9458f)
        }
    }.build()

    val Show = ImageVector.Builder("Show", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0022f, 10.0361f)
            curveTo(10.6703f, 10.0361f, 9.59021f, 11.1155f, 9.59021f, 12.4481f)
            curveTo(9.59021f, 13.7799f, 10.6704f, 14.8601f, 12.0022f, 14.8601f)
            curveTo(13.334f, 14.8601f, 14.4142f, 13.7799f, 14.4142f, 12.4481f)
            curveTo(14.4142f, 11.1155f, 13.3342f, 10.0361f, 12.0022f, 10.0361f)
            moveTo(8.09021f, 12.4481f)
            curveTo(8.09021f, 10.2867f, 9.84215f, 8.5361f, 12.0022f, 8.5361f)
            curveTo(14.1623f, 8.5361f, 15.9142f, 10.2867f, 15.9142f, 12.4481f)
            curveTo(15.9142f, 14.6083f, 14.1624f, 16.3601f, 12.0022f, 16.3601f)
            curveTo(9.842f, 16.3601f, 8.09021f, 14.6083f, 8.09021f, 12.4481f)
            close()
            moveTo(4.97577f, 6.99435f)
            curveTo(6.77017f, 5.47727f, 9.25098f, 4.39609f, 12.0022f, 4.39609f)
            curveTo(14.7529f, 4.39609f, 17.2337f, 5.47642f, 19.0282f, 6.99314f)
            curveTo(20.8033f, 8.49335f, 22.0042f, 10.5101f, 22.0042f, 12.4481f)
            curveTo(22.0042f, 14.3861f, 20.8033f, 16.4028f, 19.0282f, 17.903f)
            curveTo(17.2337f, 19.4198f, 14.7529f, 20.5001f, 12.0022f, 20.5001f)
            curveTo(9.25098f, 20.5001f, 6.77017f, 19.4189f, 4.97577f, 17.9018f)
            curveTo(3.20099f, 16.4013f, 2.00024f, 14.3846f, 2.00024f, 12.4481f)
            curveTo(2.00024f, 10.5116f, 3.20099f, 8.49485f, 4.97577f, 6.99435f)
            moveTo(5.94422f, 8.13983f)
            curveTo(4.3705f, 9.47033f, 3.50024f, 11.1046f, 3.50024f, 12.4481f)
            curveTo(3.50024f, 13.7916f, 4.3705f, 15.4258f, 5.94422f, 16.7564f)
            curveTo(7.49832f, 18.0703f, 9.64351f, 19.0001f, 12.0022f, 19.0001f)
            curveTo(14.3606f, 19.0001f, 16.5058f, 18.0709f, 18.06f, 16.7574f)
            curveTo(19.6337f, 15.4273f, 20.5042f, 13.7931f, 20.5042f, 12.4481f)
            curveTo(20.5042f, 11.1031f, 19.6337f, 9.46883f, 18.06f, 8.13878f)
            curveTo(16.5058f, 6.82525f, 14.3606f, 5.89609f, 12.0022f, 5.89609f)
            curveTo(9.64351f, 5.89609f, 7.49832f, 6.82591f, 5.94422f, 8.13983f)
            close()
        }
    }.build()

    val Hide = ImageVector.Builder("Hide", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4.552f, 15.7255f)
            curveTo(3.568f, 14.5465f, 2.995f, 13.2205f, 2.995f, 12.0035f)
            curveTo(2.995f, 8.72349f, 7.13499f, 4.70349f, 12.245f, 4.70349f)
            curveTo(14.335f, 4.70349f, 16.275f, 5.37349f, 17.835f, 6.4134f)
            moveTo(20.0951f, 8.47302f)
            curveTo(20.9861f, 9.60302f, 21.5051f, 10.853f, 21.5051f, 12.003f)
            curveTo(21.5051f, 15.283f, 17.3551f, 19.303f, 12.2451f, 19.303f)
            curveTo(11.3351f, 19.303f, 10.4461f, 19.173f, 9.61511f, 18.943f)
            moveTo(10.0111f, 14.2301f)
            curveTo(9.41608f, 13.6411f, 9.08308f, 12.8381f, 9.08608f, 12.0011f)
            curveTo(9.08208f, 10.2561f, 10.4941f, 8.83808f, 12.2401f, 8.83508f)
            moveTo(15.3552f, 12.5621f)
            curveTo(15.1212f, 13.8541f, 14.1102f, 14.8671f, 12.8182f, 15.1041f)
            moveTo(20.1372f, 4.11304f)
            lineTo(4.36316f, 19.887f)
        }
    }.build()
}
