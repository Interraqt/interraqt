package com.interraqt.core.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    profileUid: String? = null, // 🚨 If null, we show the Current User's Profile
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: (() -> Unit)? = null // 🚨 Used for exiting other people's profiles
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF5F5F5) 
    val surfaceColor = if (isDark) Color(0xFF161C24) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFFA0AAB4) else Color.DarkGray
    val primaryOrange = Color(0xFFFF6328) 
    val primaryBlue = Color(0xFF0B57D0) 
    val glassColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    // 🚨 Identify who we are looking at
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val targetUid = profileUid ?: currentUserId
    val isOwnProfile = currentUserId == targetUid 
    
    var showUploadSheet by remember { mutableStateOf(false) }

    // Display Data States
    var displayUsername by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("...") }
    var bio by remember { mutableStateOf("") }
    var postsCount by remember { mutableIntStateOf(0) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    
    // Follow State
    var isFollowing by remember { mutableStateOf(false) }

    // 🚨 1. FETCH PROFILE DATA (one-time fetch to avoid listener leaks)
    LaunchedEffect(targetUid) {
        if (targetUid.isNotBlank()) {
            firestore.collection("users").document(targetUid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        displayUsername = snapshot.getString("username") ?: ""
                        displayName = snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: if (isOwnProfile) "Update your name" else "New User"
                        bio = snapshot.getString("bio")?.takeIf { it.isNotBlank() } ?: if (isOwnProfile) "Welcome to Interraqt! You can update your bio in the edit profile section." else ""
                        postsCount = snapshot.getLong("postsCount")?.toInt() ?: 0
                        followersCount = snapshot.getLong("followersCount")?.toInt() ?: 0
                        followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0
                    }
                }
        }
        
        // If looking at someone else, check if we follow them
        if (!isOwnProfile && currentUserId.isNotEmpty()) {
            firestore.collection("users").document(currentUserId)
                .collection("following").document(targetUid).get()
                .addOnSuccessListener { doc -> isFollowing = doc.exists() }
        }
    }

    // 🚨 2. FOLLOW / UNFOLLOW LOGIC
    val toggleFollow = l@{
        if (currentUserId.isBlank()) {
            Toast.makeText(context, "Please sign in to follow users.", Toast.LENGTH_SHORT).show()
            return@l
        }

        isFollowing = !isFollowing // Optimistic UI update for instant feedback
        val incrementValue = if (isFollowing) 1L else -1L

        // Update target user's Follower count
        firestore.collection("users").document(targetUid)
            .update("followersCount", FieldValue.increment(incrementValue))
            
        // Update my Following count
        firestore.collection("users").document(currentUserId)
            .update("followingCount", FieldValue.increment(incrementValue))

        // Save standard relational data
        if (isFollowing) {
            firestore.collection("users").document(currentUserId).collection("following").document(targetUid).set(mapOf("timestamp" to System.currentTimeMillis()))
            firestore.collection("users").document(targetUid).collection("followers").document(currentUserId).set(mapOf("timestamp" to System.currentTimeMillis()))
        } else {
            firestore.collection("users").document(currentUserId).collection("following").document(targetUid).delete()
            firestore.collection("users").document(targetUid).collection("followers").document(currentUserId).delete()
        }
    }

    val shareProfile = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Connect with me on Interraqt! \nhttps://interraqt.com/@$displayUsername")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
    }

    val tabPagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEndPx = statusBarHeightPx + with(density) { 120.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f } 
                .drawWithContent {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = fadeEndPx 
                    )
                    drawContent()
                    drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(statusBarHeightDp + 80.dp))

            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(surfaceColor), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = subTextColor)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            if (bio.isNotEmpty()) {
                Text(text = bio, fontSize = 14.sp, color = subTextColor, modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            val dividerBrush = Brush.verticalGradient(listOf(Color.Transparent, subTextColor.copy(alpha = 0.4f), Color.Transparent))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(postsCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Posts", fontSize = 12.sp, color = subTextColor)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(followersCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Followers", fontSize = 12.sp, color = subTextColor)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(dividerBrush))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(followingCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Following", fontSize = 12.sp, color = subTextColor)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🚨 3. DYNAMIC BUTTON RENDERING (My Profile vs Their Profile)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { if (isOwnProfile) onNavigateToEditProfile() else toggleFollow() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isOwnProfile && isFollowing) surfaceColor else primaryOrange)
                ) {
                    Text(
                        text = if (isOwnProfile) "Edit Profile" else if (isFollowing) "Following" else "Follow", 
                        color = if (!isOwnProfile && isFollowing) textColor else Color.White, 
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { if (isOwnProfile) shareProfile() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = glassColor)
                ) {
                    Text(if (isOwnProfile) "Share Profile" else "Message", color = textColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            val tabTitles = listOf("Collections", "Videos", "Photos")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = tabPagerState.currentPage == index
                    val animatedColor by animateColorAsState(targetValue = if (isSelected) primaryOrange else subTextColor, animationSpec = tween(300), label = "")
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { coroutineScope.launch { tabPagerState.animateScrollToPage(index) } }) {
                        Text(title, color = animatedColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(90.dp).height(2.dp).background(if (isSelected) primaryOrange else Color.Transparent, RoundedCornerShape(1.dp)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(state = tabPagerState, modifier = Modifier.fillMaxWidth()) { page ->
                when (page) {
                    0 -> {
                        if (postsCount == 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isOwnProfile) "The whole world is waiting for you to Interraqt, Share a moment" else "No posts yet.",
                                    color = subTextColor, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp)
                                )
                            }
                        } else {
                            // Dummy Grid for active posts
                            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                                    Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(16.dp)).background(surfaceColor))
                                }
                            }
                        }
                    }
                    1 -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No videos yet.", color = subTextColor) }
                    2 -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No photos yet.", color = subTextColor) }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // 🚨 4. DYNAMIC FLOATING TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT ICON (Upload if me, Back Arrow if them)
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { 
                    if (isOwnProfile) showUploadSheet = true else onNavigateBack?.invoke() 
                },
                contentAlignment = Alignment.Center
            ) { 
                Icon(if (isOwnProfile) Icons.Default.Add else Icons.Default.ArrowBack, contentDescription = "Action", tint = textColor, modifier = Modifier.size(24.dp)) 
            }

            Text(text = "@$displayUsername", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)

            // RIGHT ICON (Settings if me, Hidden if them)
            if (isOwnProfile) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(glassColor).clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(textColor, RoundedCornerShape(1.dp)))
                    }
                }
            } else {
                Box(modifier = Modifier.size(44.dp)) // Empty box for visual symmetry
            }
        }

        // --- UPLOAD BOTTOM SHEET ---
        if (showUploadSheet) {
            var caption by remember { mutableStateOf("") }
            ModalBottomSheet(onDismissRequest = { showUploadSheet = false }, containerColor = surfaceColor) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("New Post", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = "Image", tint = textColor) }
                            Text("Image", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Videocam, contentDescription = "Video", tint = textColor) }
                            Text("Video", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(primaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Notes, contentDescription = "Text", tint = textColor) }
                            Text("Text", color = textColor, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = caption, onValueChange = { caption = it }, placeholder = { Text("Write a caption...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = primaryBlue, unfocusedBorderColor = Color.DarkGray.copy(alpha = 0.5f), focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )

                    Button(onClick = { showUploadSheet = false }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) { 
                        Text("Post", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
