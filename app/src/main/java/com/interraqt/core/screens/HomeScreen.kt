package com.interraqt.core.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay

// Data Models mapping directly to your Firestore structure
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreatePost: () -> Unit // 🚨 Handles routing from the top bar
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val primaryOrange = Color(0xFFFF6328)
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)

    val firestore = FirebaseFirestore.getInstance()
    
    // State to hold the real data
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var usersMap by remember { mutableStateOf<Map<String, FeedUserProfile>>(emptyMap()) }
    
    val pullRefreshState = rememberPullToRefreshState()
    var refreshKey by remember { mutableIntStateOf(0) }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            refreshKey++
            delay(800)
            pullRefreshState.endRefresh()
        }
    }

    // Fetches Posts and the Users who made them
    DisposableEffect(refreshKey) {
        val listener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                    posts = fetchedPosts
                    
                    // Finds which user profiles we haven't downloaded yet
                    val userIds = fetchedPosts.map { it.userId }.distinct()
                    val missingUsers = userIds.filter { !usersMap.containsKey(it) }
                    
                    missingUsers.forEach { uid ->
                        firestore.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"
                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                            usersMap = usersMap + (uid to FeedUserProfile(username, profileImageUrl))
                        }
                    }
                }
            }
        onDispose { listener.remove() }
    }

    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        // 1. THE MAIN FEED
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusBarHeightDp + 70.dp, bottom = 100.dp) // Pushed down to fit the Top Bar
        ) {
            items(posts, key = { it.postId }) { post ->
                val userProfile = usersMap[post.userId] ?: FeedUserProfile()
                
                FeedPostCard(
                    post = post,
                    userProfile = userProfile,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    primaryOrange = primaryOrange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryOrange)
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeightDp), 
            containerColor = surfaceColor, 
            contentColor = primaryOrange
        )

        // 2. THE LIQUID GLASS TOP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(bgColor, bgColor.copy(alpha = 0.8f), Color.Transparent),
                        startY = 0f,
                        endY = statusBarHeightPx + 180f
                    )
                    drawRect(brush = gradient)
                    drawContent()
                }
                .padding(top = statusBarHeightDp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Create Post Button
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToCreatePost() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post", tint = textColor, modifier = Modifier.size(24.dp))
                }

                // Center: App Brand
                Text(
                    text = "Interraqt", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = textColor
                )

                // Right: Notifications Button
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { /* TODO: Notifications */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// THE INDIVIDUAL POST COMPONENT
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedPostCard(
    post: FeedPost,
    userProfile: FeedUserProfile,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryOrange: Color
) {
    val context = LocalContext.current
    
    // Converts timestamp into "2h ago" format
    val timeAgo = remember(post.timestamp) {
        if (post.timestamp > 0) {
            DateUtils.getRelativeTimeSpanString(post.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
        } else ""
    }

    Surface(
        color = surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Section A: Header (Profile Pic, Username, More Options)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userProfile.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(userProfile.profileImageUrl).crossfade(true).build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Default Profile", tint = subTextColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(text = userProfile.username, fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                        if (timeAgo.isNotEmpty()) {
                            Text(text = timeAgo, color = subTextColor, fontSize = 12.sp)
                        }
                    }
                }
                
                IconButton(onClick = { /* TODO: Post Options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = textColor)
                }
            }

            // Section B: Media Carousel
            if (post.mediaUrls.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f) // Premium Instagram sizing
                    ) { page ->
                        // Currently renders images. In the next step, we will wire this to trigger the FullscreenMediaViewer!
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(post.mediaUrls[page]).crossfade(true).build(),
                            contentDescription = "Post Media",
                            modifier = Modifier.fillMaxSize().clickable { /* TODO: Open Fullscreen Viewer */ },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Dot Indicator (Only shows if > 1 media item)
                    if (post.mediaUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
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

            // Section C: Action Bar (Like, Comment, Share)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = textColor, modifier = Modifier.size(28.dp).clickable { /* TODO: Like logic */ })
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = textColor, modifier = Modifier.size(26.dp))
                    Icon(Icons.Outlined.Send, contentDescription = "Share", tint = textColor, modifier = Modifier.size(26.dp))
                }
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = textColor, modifier = Modifier.size(28.dp))
            }

            // Section D: Likes count and Caption
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                if (post.likesCount > 0) {
                    Text(text = "${post.likesCount} likes", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (post.caption.isNotBlank()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(userProfile.username)
                            }
                            append("  ")
                            append(post.caption)
                        },
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                
                if (post.commentsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View all ${post.commentsCount} comments",
                        color = subTextColor,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { /* TODO: Open comments */ }
                    )
                }
            }
        }
    }
}
