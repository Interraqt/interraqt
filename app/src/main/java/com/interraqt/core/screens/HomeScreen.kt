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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.interraqt.core.R

// Custom Icons Object
object HomeScreenIcons {
    val Like @Composable get() = painterResource(id = R.drawable.ic_like)
    val BookmarkOutline @Composable get() = painterResource(id = R.drawable.ic_bookmark_outline)
    val BookmarkFilled @Composable get() = painterResource(id = R.drawable.ic_bookmark_filled)
    val Comment @Composable get() = painterResource(id = R.drawable.ic_comment)
    val Share @Composable get() = painterResource(id = R.drawable.ic_share)
    val Interested @Composable get() = painterResource(id = R.drawable.ic_interested)
    val NotInterested @Composable get() = painterResource(id = R.drawable.ic_not_interested)
}

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
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
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
    var showDeleteDialog by remember { mutableStateOf<FeedPost?>(null) }
    
    val pullRefreshState = rememberPullToRefreshState()

    fun getShortTime(time: Long): String {
        if (time == 0L) return ""
        val diff = System.currentTimeMillis() - time
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    fun loadPosts(isRefresh: Boolean = false) {
        if (isLoadingMore || (!hasMore && !isRefresh)) return
        isLoadingMore = true
        
        var query = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            
        if (!isRefresh && lastVisible != null) {
            query = query.startAfter(lastVisible!!)
        }

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

    // 🚨 Lightning Fast Pre-fetching: Triggers when 5 posts away from bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && totalItems > 0 && lastVisibleItem >= totalItems - 5
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
        // 1. MAIN FEED (Fluid Scroll, No Feathering)
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
        }

        // 2. STANDARD PULL TO REFRESH
        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), 
            containerColor = glassColor, 
            contentColor = textColor
        )

        // 3. LIQUID GLASS TOP BAR (Synced ProfileScreen Sizes)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.95f))
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
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToCreatePost() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, contentDescription = "Create", tint = textColor, modifier = Modifier.size(24.dp)) }

                Text("Interraqt", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)

                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(24.dp)) }
            }
        }

        // 4. BOTTOM SHEETS & DIALOGS
        if (showOptionsForPost != null) {
            PostOptionsBottomSheet(
                isOwnProfile = showOptionsForPost!!.userId == currentUserId,
                textColor = textColor, 
                bgColor = bgColor, 
                surfaceColor = surfaceColor,
                onDismiss = { showOptionsForPost = null },
                onDeleteRequest = { 
                    showDeleteDialog = showOptionsForPost
                    showOptionsForPost = null
                }
            )
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
                onDismiss = { showCommentsForPost = null },
                onCommentAdded = { 
                    // Instantly update local post count so the pill stretches immediately
                    posts = posts.map { if (it.postId == showCommentsForPost!!.postId) it.copy(commentsCount = it.commentsCount + 1) else it }
                }
            )
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = surfaceColor,
                title = { Text("Delete Post", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete this post? This action cannot be undone.", color = textColor) },
                confirmButton = {
                    TextButton(onClick = {
                        val postToDelete = showDeleteDialog!!
                        firestore.collection("posts").document(postToDelete.postId).delete()
                        posts = posts.filter { it.postId != postToDelete.postId } // Instantly remove from UI
                        showDeleteDialog = null
                    }) { Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun MomentsTray(textColor: Color, subTextColor: Color, primaryOrange: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Moments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(65.dp).clip(CircleShape).border(2.dp, subTextColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add Moment", tint = textColor, modifier = Modifier.size(28.dp))
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

    // Real-time optimistic states
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

    // 🚨 Optimistic UI: Screen updates instantly, DB updates in background
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

    val likeScale by animateFloatAsState(targetValue = if (isLiked) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val saveScale by animateFloatAsState(targetValue = if (isSaved) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userProfile.profileImageUrl.isNotEmpty()) {
                    // 🚨 Instant image loading with Coil caching
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
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Larger Follow Button
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // Larger Menu Button
                IconButton(onClick = onOptionsClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "More", tint = textColor, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Media Carousel (Soft Swiping)
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

        // 🚨 CUSTOM ICON ACTION BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Like Button
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
                
                // Comment Button
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
                
                // Share Button
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(glassColor).clickable { }, contentAlignment = Alignment.Center) {
                    Icon(HomeScreenIcons.Share, contentDescription = "Share", tint = textColor, modifier = Modifier.size(24.dp))
                }
            }
            // Save Button
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

// 🚨 SETTINGS STYLE PREMIUM BOTTOM SHEETS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    isOwnProfile: Boolean,
    textColor: Color, 
    bgColor: Color, 
    surfaceColor: Color,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 40.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onDeleteRequest(); onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Delete Post", color = Color(0xFFD32F2F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Copy link", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HomeScreenIcons.Interested, contentDescription = "Interested", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Interested", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HomeScreenIcons.NotInterested, contentDescription = "Not interested", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Not interested", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    
                    Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Flag, contentDescription = "Report", tint = textColor, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Report", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

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
            val commentId = java.util.UUID.randomUUID().toString()
            val commentMap = PostComment(commentId, currentUserId, commentText.trim(), System.currentTimeMillis())
            
            // 🚨 Optimistic UI: Update immediately locally
            comments = comments + commentMap
            onCommentAdded()
            commentText = ""

            // Push to backend
            firestore.collection("posts").document(post.postId).collection("comments").document(commentId).set(commentMap)
            firestore.collection("posts").document(post.postId).update("commentsCount", FieldValue.increment(1))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = bgColor, 
        modifier = Modifier.wrapContentHeight(), // 🚨 Wraps perfectly to content instead of forcing dead space
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
            
            // Sleek Floating Input Box
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
