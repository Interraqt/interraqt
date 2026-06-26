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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeViewModel : ViewModel() {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
    var usersMap by mutableStateOf<Map<String, FeedUserProfile>>(emptyMap())
    var lastVisible by mutableStateOf<DocumentSnapshot?>(null)
    var isLoadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreatePost: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328)
    
    // 🚨 FIX 4: Exactly matches ProfileScreen glass opacity
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    
    val listState = rememberLazyListState()

    var showOptionsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showCommentsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FeedPost?>(null) }
    
    val pullRefreshState = rememberPullToRefreshState()

    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) isTopBarVisible = false 
                if (available.y > 15) isTopBarVisible = true   
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
        if (viewModel.isLoadingMore || (!viewModel.hasMore && !isRefresh)) return
        viewModel.isLoadingMore = true
        
        if (isRefresh) {
            viewModel.lastVisible = null
            viewModel.hasMore = true
        }
        
        var query = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(24)
            
        if (!isRefresh && viewModel.lastVisible != null) {
            query = query.startAfter(viewModel.lastVisible!!)
        }

        query.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                viewModel.hasMore = false
            } else {
                viewModel.lastVisible = snapshot.documents.last()
                val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                
                val randomizedBatch = fetchedPosts.shuffled() 
                
                viewModel.posts = if (isRefresh) randomizedBatch else viewModel.posts + randomizedBatch
                
                val missingUsers = randomizedBatch.map { it.userId }.distinct().filter { !viewModel.usersMap.containsKey(it) }
                missingUsers.forEach { uid ->
                    firestore.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                        viewModel.usersMap = viewModel.usersMap + (uid to FeedUserProfile(username, profileImageUrl))
                    }
                }
            }
            viewModel.isLoadingMore = false
            if (isRefresh) pullRefreshState.endRefresh()
        }.addOnFailureListener {
            viewModel.isLoadingMore = false
            if (isRefresh) pullRefreshState.endRefresh()
        }
    }

    // 🚨 FIX 6: Automatically refreshes and shuffles when the app is reopened from background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadPosts(isRefresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { 
        if (viewModel.posts.isEmpty()) {
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
            viewModel.hasMore && !viewModel.isLoadingMore && totalItems > 0 && lastVisibleItem >= totalItems - 10
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) loadPosts() }

    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(nestedScrollConnection) 
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
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

            if (viewModel.posts.isEmpty() && viewModel.isLoadingMore) {
                items(3) {
                    ShimmerFeedPostCard(bgColor = bgColor, glassColor = glassColor)
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            } else {
                // 🚨 FIX 7: contentType added for buttery smooth recycling
                items(items = viewModel.posts, key = { it.postId }, contentType = { "FeedPost" }) { post ->
                    val userProfile = viewModel.usersMap[post.userId] ?: FeedUserProfile()
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
                
                // 🚨 FIX 3: "Reached the end" message
                if (!viewModel.hasMore && viewModel.posts.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("You're all caught up!", color = subTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), 
            containerColor = Color.Transparent, 
            contentColor = primaryOrange
        )

        // 🚨 FIX 5: Solid Background behind Top Bar so posts don't bleed through text
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topBarOffset)
                .alpha(topBarAlpha)
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.95f)) 
                .padding(top = statusBarHeightDp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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

                Text(
                    text = "Interraqt", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Normal, 
                    color = textColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

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
                    viewModel.posts = viewModel.posts.map { if (it.postId == showCommentsForPost!!.postId) it.copy(commentsCount = it.commentsCount + 1) else it }
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
                        viewModel.posts = viewModel.posts.filter { it.postId != postToDelete.postId }
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

// 🚨 FIX 9: Re-engineered Shimmer Math for active sweeping motion
@Composable
fun ShimmerFeedPostCard(bgColor: Color, glassColor: Color) {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(glassColor.copy(alpha = 0.1f), glassColor.copy(alpha = 0.5f), glassColor.copy(alpha = 0.1f)),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
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
