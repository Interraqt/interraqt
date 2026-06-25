package com.interraqt.core.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), 
            containerColor = glassColor, 
            contentColor = textColor
        )

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
