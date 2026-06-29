package com.interraqt.core.screens.profile

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import com.interraqt.core.screens.home.GestureLockState
import com.interraqt.core.screens.home.rememberDirectionalScrollConnection


import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.interraqt.core.screens.profile.tabs.CollectionsTab
import com.interraqt.core.screens.profile.tabs.PhotosTab
import com.interraqt.core.screens.profile.tabs.VideosTab
import com.interraqt.core.screens.profile.user.LoggedUserProfileActions
import com.interraqt.core.screens.profile.user.OtherUserProfileActions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    profileUid: String? = null, 
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateBack: (() -> Unit)? = null 
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5) 
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328) 
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val targetUid = profileUid ?: currentUserId
    val isOwnProfile = currentUserId == targetUid 

    var displayUsername by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("...") }
    var bio by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") } 
    var bannerImageUrl by remember { mutableStateOf("") } 
    var postsCount by remember { mutableIntStateOf(0) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }

        // 🚨 FIX: Now it completely turns off if it's your own profile, 
    // letting MainActivity catch the swipe and take you to Home!
    BackHandler(enabled = !isOwnProfile) { 
        onNavigateBack?.invoke() 
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    val pullRefreshState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            refreshKey++
            delay(800)
            pullRefreshState.endRefresh()
        }
    }

    DisposableEffect(targetUid, refreshKey) {
        val listener = if (targetUid.isNotBlank()) {
            firestore.collection("users").document(targetUid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    displayUsername = snapshot.getString("username") ?: ""
                    displayName = snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: if (isOwnProfile) "Update your name" else "New User"
                    bio = snapshot.getString("bio")?.takeIf { it.isNotBlank() } ?: if (isOwnProfile) "Welcome to Interraqt!" else ""
                    profileImageUrl = snapshot.getString("profileImageUrl") ?: "" 
                    bannerImageUrl = snapshot.getString("bannerImageUrl") ?: ""
                    postsCount = snapshot.getLong("postsCount")?.toInt() ?: 0
                    followersCount = snapshot.getLong("followersCount")?.toInt() ?: 0
                    followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0
                }
            }
        } else null
        
        if (!isOwnProfile && currentUserId.isNotEmpty()) {
            firestore.collection("users").document(currentUserId)
                .collection("following").document(targetUid).get()
                .addOnSuccessListener { doc -> isFollowing = doc.exists() }
        }
        
        onDispose { listener?.remove() }
    }

    val toggleFollow: () -> Unit = l@{
        if (currentUserId.isBlank()) return@l

        isFollowing = !isFollowing 
        val incrementValue = if (isFollowing) 1 else -1
        followersCount += incrementValue 

        firestore.collection("users").document(targetUid).update("followersCount", FieldValue.increment(incrementValue.toLong()))
        firestore.collection("users").document(currentUserId).update("followingCount", FieldValue.increment(incrementValue.toLong()))

        if (isFollowing) {
            firestore.collection("users").document(currentUserId).collection("following").document(targetUid).set(mapOf("timestamp" to System.currentTimeMillis()))
            firestore.collection("users").document(targetUid).collection("followers").document(currentUserId).set(mapOf("timestamp" to System.currentTimeMillis()))
        } else {
            firestore.collection("users").document(currentUserId).collection("following").document(targetUid).delete()
            firestore.collection("users").document(targetUid).collection("followers").document(currentUserId).delete()
        }
    }

    val shareProfile: () -> Unit = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Connect with me on Interraqt! \nhttps://interraqt.com/$displayUsername")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
    }

        val tabPagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    // 🚨 ADDED: Gesture Lock state for smooth Profile Tab swiping
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val gestureLockState = remember { GestureLockState(touchSlop) }
    val profileNestedScrollConnection = rememberDirectionalScrollConnection(gestureLockState)

    
    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEndPx = statusBarHeightPx + with(density) { 120.dp.toPx() }

    val scrollValue = scrollState.value.toFloat()
    val topColorAlpha = when {
        scrollValue < 120f -> 0f
        scrollValue > 180f -> 1f
        else -> (scrollValue - 120f) / 60f
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor).nestedScroll(pullRefreshState.nestedScrollConnection)) {
        
        Column(
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { alpha = 0.99f } 
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 1f - topColorAlpha), Color.Black), 
                        startY = 0f, 
                        endY = fadeEndPx
                    )
                    drawContent()
                    drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                }
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            ProfileHeader(
                bannerImageUrl = bannerImageUrl,
                profileImageUrl = profileImageUrl,
                displayName = displayName,
                bio = bio,
                postsCount = postsCount,
                followersCount = followersCount,
                followingCount = followingCount,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                subTextColor = subTextColor,
                statusBarHeightDp = statusBarHeightDp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isOwnProfile) {
                LoggedUserProfileActions(
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    shareProfile = shareProfile,
                    primaryOrange = primaryOrange,
                    glassColor = glassColor,
                    textColor = textColor
                )
            } else {
                OtherUserProfileActions(
                    isFollowing = isFollowing,
                    toggleFollow = toggleFollow,
                    onMessageClick = { Toast.makeText(context, "Messaging coming soon!", Toast.LENGTH_SHORT).show() },
                    surfaceColor = surfaceColor,
                    primaryOrange = primaryOrange,
                    glassColor = glassColor,
                    textColor = textColor
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            ProfileTabs(
                tabPagerState = tabPagerState,
                primaryOrange = primaryOrange,
                subTextColor = subTextColor,
                coroutineScope = coroutineScope
            )

            Spacer(modifier = Modifier.height(24.dp))

                        HorizontalPager(
                state = tabPagerState, 
                modifier = Modifier
                    .fillMaxWidth()
                    // 1. Resets the gesture lock when the user first touches the pager
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            gestureLockState.reset()
                        }
                    }
                    // 2. Applies the custom directional lock connection
                    .nestedScroll(profileNestedScrollConnection),
                verticalAlignment = Alignment.Top // 🚨 This stops the tabs from shifting down!
            ) { page ->

                
                                when (page) {
                    // 🚨 Passed targetUid and firestore to the tab
                    0 -> CollectionsTab(targetUid, firestore, isOwnProfile, postsCount, subTextColor, surfaceColor)
                    1 -> VideosTab(subTextColor)

                    2 -> PhotosTab(subTextColor)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), containerColor = surfaceColor, contentColor = primaryOrange)

        ProfileTopBar(
            isOwnProfile = isOwnProfile,
            displayUsername = displayUsername,
            glassColor = glassColor,
            textColor = textColor,
            onNavigateToCreatePost = onNavigateToCreatePost,
            onNavigateBack = onNavigateBack,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}
