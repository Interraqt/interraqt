package com.interraqt.core.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush


import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore

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
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)

    val firestore = FirebaseFirestore.getInstance() // Passed to inner components
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    var showOptionsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showCommentsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FeedPost?>(null) }

    var isTopBarVisible by remember { mutableStateOf(true) }
    val topBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 🚨 Only update state if it actually changed, saving performance!
                if (available.y < -15f && isTopBarVisible) isTopBarVisible = false 
                if (available.y > 15f && !isTopBarVisible) isTopBarVisible = true   
                return Offset.Zero
            }
        }
    }

    

    val topBarOffset by animateDpAsState(if (isTopBarVisible) 0.dp else (-100).dp, spring(stiffness = Spring.StiffnessMediumLow), label = "")
    val topBarAlpha by animateFloatAsState(if (isTopBarVisible) 1f else 0f, tween(300), label = "")

        val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isTopBarVisible = true 
                
                // 🚨 FIX: If posts are empty, ViewModel's init block is already fetching them. 
                // Only do the 10-minute check if we are resuming from the background!
                if (viewModel.posts.isNotEmpty()) {
                    if (System.currentTimeMillis() - viewModel.lastFetchTime > 600_000) {
                        viewModel.loadPosts(isRefresh = true) 
                    } else {
                        viewModel.checkForNewPosts() 
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.loadPosts(isRefresh = true) { pullRefreshState.endRefresh() } }
    }

            val shouldLoadMore = remember {
        derivedStateOf {
            // 🚨 FIX: Replaced layoutInfo with firstVisibleItemIndex. 
            // This stops the app from doing math 120 times a second. It now only recalculates 
            // when a new post reaches the top of the screen!
            val totalItems = listState.layoutInfo.totalItemsCount
            val currentItem = listState.firstVisibleItemIndex 
            
            viewModel.hasMore && !viewModel.isLoadingMore && totalItems > 0 && currentItem >= totalItems - 8
        }
    }


 
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) viewModel.loadPosts() }

    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(bgColor).nestedScroll(topBarScrollConnection).nestedScroll(pullRefreshState.nestedScrollConnection)) {
      
                        LazyColumn(
            state = listState, 
            modifier = Modifier.fillMaxSize(), 
            contentPadding = PaddingValues(top = statusBarHeightDp + 64.dp, bottom = 100.dp),
            // Utilizes the newly tuned physics engine for natural inertia
            flingBehavior = rememberBoostedFlingBehavior(velocityMultiplier = 1.4f) 
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
                items(items = viewModel.posts, key = { it.postId }, contentType = { "FeedPost" }) { post ->
                    val userProfile = viewModel.usersMap[post.userId] ?: FeedUserProfile()
                    FeedPostCard(
                        post = post,
                        userProfile = userProfile,
                        currentUserId = viewModel.currentUserId,
                        shortTime = viewModel.getShortTime(post.timestamp),
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
                
                                // 🚨 FIX: If they rapid-scroll to the bottom before the background fetch finishes,
                // this displays a classic Instagram-style circular spinner!
                if (viewModel.isLoadingMore && viewModel.posts.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryOrange, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                
                if (!viewModel.hasMore && viewModel.posts.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("You're all caught up!", color = subTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }


                        PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), containerColor = Color.Transparent, contentColor = primaryOrange)

        HomeTopBar(


        
                    topBarOffsetProvider = { topBarOffset }, 
            topBarAlphaProvider = { topBarAlpha },   
            statusBarHeightDp = statusBarHeightDp,
            bgColor = bgColor,
            glassColor = glassColor,
            textColor = textColor,
            onNavigateToCreatePost = onNavigateToCreatePost
        )

        
        if (showOptionsForPost != null) {
            PostOptionsBottomSheet(
                isOwnProfile = showOptionsForPost!!.userId == viewModel.currentUserId,
                textColor = textColor, bgColor = bgColor, surfaceColor = surfaceColor,
                onDismiss = { showOptionsForPost = null },
                onDeleteRequest = { showDeleteDialog = showOptionsForPost; showOptionsForPost = null }
            )
        }
        
        if (showCommentsForPost != null) {
            CommentsBottomSheet(
                post = showCommentsForPost!!, currentUserId = viewModel.currentUserId, firestore = firestore, 
                textColor = textColor, subTextColor = subTextColor, bgColor = bgColor, primaryOrange = primaryOrange, glassColor = glassColor,
                onDismiss = { showCommentsForPost = null },
                onCommentAdded = { viewModel.incrementCommentCount(showCommentsForPost!!.postId) }
            )
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null }, containerColor = surfaceColor,
                title = { Text("Delete Post", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete this post? This action cannot be undone.", color = textColor) },
                confirmButton = { TextButton(onClick = { viewModel.deletePost(showDeleteDialog!!.postId); showDeleteDialog = null }) { Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel", color = Color.Gray) } }
            )
        }
    }
}
