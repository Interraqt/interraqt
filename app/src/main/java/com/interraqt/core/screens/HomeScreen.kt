package com.interraqt.core.screens

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreatePost: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328)
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)

    val firestore = FirebaseFirestore.getInstance()
    
    // State to hold the real data
    var allPosts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var randomizedPosts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var usersMap by remember { mutableStateOf<Map<String, FeedUserProfile>>(emptyMap()) }
    
    // Bottom Sheet States
    var showOptionsForPost by remember { mutableStateOf<FeedPost?>(null) }
    var showCommentsForPost by remember { mutableStateOf<FeedPost?>(null) }
    
    val pullRefreshState = rememberPullToRefreshState()
    var refreshKey by remember { mutableIntStateOf(0) }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            randomizedPosts = allPosts.shuffled() // 🚨 Randomizes feed instantly on refresh
            delay(800)
            pullRefreshState.endRefresh()
        }
    }

    DisposableEffect(refreshKey) {
        val listener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                    allPosts = fetchedPosts
                    if (randomizedPosts.isEmpty()) randomizedPosts = fetchedPosts.shuffled() // Initial shuffle
                    
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
    val fadeEndPx = statusBarHeightPx + with(density) { 100.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        // 1. THE MAIN FEED WITH FEATHER EFFECT
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f } // Required for DstIn blend mode
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = statusBarHeightPx,
                        endY = fadeEndPx // 🚨 Smoothly feathers posts out as they slide under the top bar
                    )
                    drawContent()
                    drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(top = statusBarHeightDp + 70.dp, bottom = 100.dp) 
        ) {
            // MOMENTS (STORIES) TRAY
            item {
                MomentsTray(textColor = textColor, subTextColor = subTextColor, primaryOrange = primaryOrange)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = subTextColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            }

            items(randomizedPosts, key = { it.postId }) { post ->
                val userProfile = usersMap[post.userId] ?: FeedUserProfile()
                
                FeedPostCard(
                    post = post,
                    userProfile = userProfile,
                    bgColor = bgColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    primaryOrange = primaryOrange,
                    onOptionsClick = { showOptionsForPost = post },
                    onCommentClick = { showCommentsForPost = post }
                )
                
                // 🚨 Seamless ultra-thin line separation instead of chunky boxes
                HorizontalDivider(color = subTextColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            }
            
            if (randomizedPosts.isEmpty()) {
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
            containerColor = bgColor, 
            contentColor = primaryOrange
        )

        // 2. THE LIQUID GLASS TOP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeightDp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Create Post Button (Larger)
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToCreatePost() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post", tint = textColor, modifier = Modifier.size(26.dp))
                }

                // Center: App Brand (Larger)
                Text(
                    text = "Interraqt", 
                    fontSize = 26.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = textColor
                )

                // Right: Notifications Button (Larger)
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(glassColor).clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Notifications", tint = textColor, modifier = Modifier.size(26.dp))
                }
            }
        }

        // 3. BOTTOM SHEETS
        if (showOptionsForPost != null) {
            PostOptionsBottomSheet(
                textColor = textColor, 
                bgColor = bgColor, 
                onDismiss = { showOptionsForPost = null }
            )
        }
        
        if (showCommentsForPost != null) {
            CommentsBottomSheet(
                textColor = textColor, 
                bgColor = bgColor, 
                primaryOrange = primaryOrange,
                onDismiss = { showCommentsForPost = null }
            )
        }
    }
}

// MOMENTS (STORIES) COMPONENT
@Composable
fun MomentsTray(textColor: Color, subTextColor: Color, primaryOrange: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Moments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // Your own Moment add button
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(65.dp).clip(CircleShape).border(2.dp, subTextColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add Moment", tint = textColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add", color = subTextColor, fontSize = 12.sp)
                }
            }
            // Dummy Moments placeholders
            items(8) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(65.dp).clip(CircleShape).border(2.5.dp, primaryOrange, CircleShape).padding(4.dp)) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(subTextColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "User", tint = textColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("User ${index+1}", color = textColor, fontSize = 12.sp)
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
    bgColor: Color, // 🚨 Main app background only
    textColor: Color,
    subTextColor: Color,
    primaryOrange: Color,
    onOptionsClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    val context = LocalContext.current
    
    val timeAgo = remember(post.timestamp) {
        if (post.timestamp > 0) DateUtils.getRelativeTimeSpanString(post.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString() else ""
    }

    // Bouncy Interactive States
    var isLiked by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    val likeScale by animateFloatAsState(targetValue = if (isLiked) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val likeColor by animateColorAsState(targetValue = if (isLiked) primaryOrange else textColor)

    val saveScale by animateFloatAsState(targetValue = if (isSaved) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val saveColor by animateColorAsState(targetValue = if (isSaved) primaryOrange else textColor)

    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) { // Seamless background
        
        // Section A: Header
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
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Default", tint = subTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(text = userProfile.username, fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                    if (timeAgo.isNotEmpty()) Text(text = timeAgo, color = subTextColor, fontSize = 12.sp) // 🚨 Time exactly below username
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 🚨 Functional, animated Follow button
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    color = if (isFollowing) subTextColor else primaryOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { isFollowing = !isFollowing }.padding(horizontal = 8.dp)
                )
                
                IconButton(onClick = onOptionsClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = textColor)
                }
            }
        }

        // Section B: Media Carousel
        if (post.mediaUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
            
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f), // 🚨 4:5 Perfect Ratio
                    beyondBoundsPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState) // 🚨 Fluid Swipe
                ) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(post.mediaUrls[page]).crossfade(true).build(),
                        contentDescription = "Post Media",
                        modifier = Modifier.fillMaxSize(), // Fullscreen preview disabled as requested
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

        // Section C: Action Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                // Like Button (Bouncy + Numbers)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isLiked = !isLiked }) {
                    Icon(if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = likeColor, modifier = Modifier.size(28.dp).graphicsLayer { scaleX = likeScale; scaleY = likeScale })
                    if (post.likesCount > 0 || isLiked) Text(text = "${post.likesCount + if (isLiked) 1 else 0}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                }
                
                // Comment Button (Numbers)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCommentClick() }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = textColor, modifier = Modifier.size(26.dp))
                    if (post.commentsCount > 0) Text(text = "${post.commentsCount}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                }
                
                // Share Button (No Numbers)
                Icon(Icons.Outlined.Send, contentDescription = "Share", tint = textColor, modifier = Modifier.size(26.dp))
            }
            // Save Button (Bouncy, No Numbers)
            Icon(
                imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, 
                contentDescription = "Save", 
                tint = saveColor, 
                modifier = Modifier.size(28.dp).graphicsLayer { scaleX = saveScale; scaleY = saveScale }.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isSaved = !isSaved }
            )
        }

        // Section D: Caption
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            if (post.caption.isNotBlank()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(userProfile.username) }
                        append("  ")
                        append(post.caption)
                    },
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// PREMIUM BOTTOM SHEETS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(textColor: Color, bgColor: Color, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = bgColor) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            val options = listOf(
                Icons.Outlined.ContentCopy to "Copy link",
                Icons.Outlined.BookmarkBorder to "Save",
                Icons.Outlined.SentimentSatisfied to "Interested",
                Icons.Outlined.SentimentDissatisfied to "Not interested",
                Icons.Outlined.Flag to "Report"
            )
            options.forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = text, tint = textColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(textColor: Color, bgColor: Color, primaryOrange: Color, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = bgColor, modifier = Modifier.fillMaxHeight(0.7f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Comments", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
            HorizontalDivider(color = textColor.copy(alpha = 0.1f))
            
            // Dummy Comments List
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(3) {
                    Row(modifier = Modifier.padding(vertical = 12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(textColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "User", tint = textColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Username", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                            Text("This is an amazing post! 🔥", color = textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            // Comment Input Box
            Box(modifier = Modifier.fillMaxWidth().background(bgColor).padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(textColor.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Add a comment...", color = textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        }
    }
}
