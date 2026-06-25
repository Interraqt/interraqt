package com.interraqt.core.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

    // 🚨 SCROLL DETECTOR: Hides/Shows Top Bar smoothly
    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) isTopBarVisible = false // Scrolling down
                if (available.y > 15) isTopBarVisible = true   // Scrolling up
                return Offset.Zero
            }
        }
    }

    val topBarOffset by animateDpAsState(
        targetValue = if (isTopBarVisible) 0.dp else (-100).dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = ""
    )
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isTopBarVisible) 1f else 0f,
        animationSpec = tween(300), label = ""
    )

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
                val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                
                // 🚨 ALGORITHM: Shuffle posts locally before displaying
                val randomizedBatch = fetchedPosts.shuffled() 
                
                posts = if (isRefresh) randomizedBatch else posts + randomizedBatch
                
                val missingUsers = randomizedBatch.map { it.userId }.distinct().filter { !usersMap.containsKey(it) }
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

    // 🚨 SMART CACHING: Only load if the list is completely empty! Prevents reloading on scroll up.
    LaunchedEffect(Unit) { 
        if (posts.isEmpty()) {
            loadPosts(isRefresh = true) 
        }
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { loadPosts(isRefresh = true) }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && totalItems > 0 && lastVisibleItem >= totalItems - 5
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) loadPosts() }

    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(nestedScrollConnection) // Applies custom hide/show scroll lock
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // 🚨 IMMERSIVE FULLSCREEN: Draws directly under the status bar
            contentPadding = PaddingValues(top = statusBarHeightDp + 64.dp, bottom = 100.dp) 
        ) {
            item {
                MomentsTray(textColor = textColor, subTextColor = subTextColor, primaryOrange = primaryOrange)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), thickness = 0.5.dp)
            }

            // 🚨 SKELETON LOADING UI
            if (posts.isEmpty() && isLoadingMore) {
                items(3) {
                    ShimmerFeedPostCard(bgColor = bgColor, glassColor = glassColor)
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            } else {
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
        }

        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), 
            containerColor = glassColor, 
            contentColor = textColor
        )

        // 🚨 AUTO-HIDING TOP BAR (Background removed for full immersion)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = topBarOffset)
                .alpha(topBarAlpha)
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

                // 🚨 UNIFIED NOTIFICATION ICON: Now uses the custom Like icon!
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { },
                    contentAlignment = Alignment.Center
                ) { Icon(HomeScreenIcons.Like, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(24.dp)) }
            }
        }

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
                        posts = posts.filter { it.postId != postToDelete.postId }
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

// 🚨 SHIMMER SKELETON UI COMPONENT
@Composable
fun ShimmerFeedPostCard(bgColor: Color, glassColor: Color) {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(glassColor.copy(alpha = 0.2f), glassColor.copy(alpha = 0.8f), glassColor.copy(alpha = 0.2f)),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(shimmerBrush))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(modifier = Modifier.height(14.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(10.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f/5f).background(shimmerBrush))
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(shimmerBrush))
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(shimmerBrush))
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(shimmerBrush))
        }
    }
}
