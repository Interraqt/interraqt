package com.interraqt.core

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.core.tween


import android.view.WindowManager
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.interraqt.core.auth.LoginScreen
import com.interraqt.core.auth.SignupScreen
import com.interraqt.core.navigation.BottomNavigationBar
import com.interraqt.core.screens.*
import kotlin.math.abs
import com.interraqt.core.screens.createpost.CreatePostScreen

import com.interraqt.core.screens.home.HomeScreen

enum class AppScreen {
    Login, Signup, Main, Settings, EditProfile, OtherProfile, CreatePost
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 🚨 FORCES EDGE-TO-EDGE INTO THE CAMERA NOTCH DURING FULLSCREEN
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            RootNavigation() 
        }
    }
}


@Composable
fun RootNavigation() {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    
    var currentScreen by remember { 
        mutableStateOf(if (auth.currentUser != null) AppScreen.Main else AppScreen.Login) 
    }
    
    // 🚨 ADDED: Navigation memory tracker to flawlessy route the back button
    var previousScreen by remember { mutableStateOf(AppScreen.Main) }
    
    var savedTab by remember { mutableIntStateOf(0) }
    var viewedUserId by remember { mutableStateOf("") } 
    var globalUsername by remember { mutableStateOf("...") }

    DisposableEffect(auth.currentUser) {
        val uid = auth.currentUser?.uid
        val listener = if (uid != null) {
            firestore.collection("users").document(uid).addSnapshotListener { doc, _ ->
                globalUsername = doc?.getString("username") ?: "Unknown"
            }
        } else null
        
        onDispose { listener?.remove() }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (initialState == AppScreen.Login && targetState == AppScreen.Signup) {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            } else if (initialState == AppScreen.Signup && targetState == AppScreen.Login) {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            } else if (targetState == AppScreen.Settings || targetState == AppScreen.EditProfile || targetState == AppScreen.OtherProfile || targetState == AppScreen.CreatePost) {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 2 })
            } else if ((initialState == AppScreen.Settings || initialState == AppScreen.EditProfile || initialState == AppScreen.OtherProfile || initialState == AppScreen.CreatePost) && targetState == AppScreen.Main) {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 2 }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "ScreenTransition"
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.Login -> LoginScreen(
                onNavigateToSignup = { currentScreen = AppScreen.Signup },
                onLoginSuccess = { savedTab = 0; currentScreen = AppScreen.Main }
            )
            AppScreen.Signup -> SignupScreen(
                onNavigateToLogin = { currentScreen = AppScreen.Login },
                onSignupSuccess = { savedTab = 0; currentScreen = AppScreen.Main }
            )
            AppScreen.Main -> InterraqtApp(
                initialTab = savedTab, 
                onTabChange = { savedTab = it }, 
                onNavigateToSettings = { 
                    previousScreen = currentScreen
                    currentScreen = AppScreen.Settings 
                },
                onNavigateToEditProfile = { 
                    previousScreen = currentScreen
                    currentScreen = AppScreen.EditProfile 
                },
                onNavigateToCreatePost = { 
                    // 🚨 Remember we came from Main before opening CreatePost
                    previousScreen = currentScreen
                    currentScreen = AppScreen.CreatePost 
                },
                onNavigateToUserProfile = { uid -> 
                    viewedUserId = uid 
                    previousScreen = currentScreen
                    currentScreen = AppScreen.OtherProfile 
                }, 
                onLogout = { savedTab = 0; currentScreen = AppScreen.Login }
            )
            AppScreen.Settings -> SettingsScreen(
                username = globalUsername, 
                onNavigateToEditProfile = { 
                    previousScreen = currentScreen
                    currentScreen = AppScreen.EditProfile 
                },
                onNavigateBack = { currentScreen = previousScreen },
                onLogout = { savedTab = 0; currentScreen = AppScreen.Login }
            )
            AppScreen.EditProfile -> EditProfileScreen(
                onNavigateBack = { 
                    // 1. Go back to where you came from (Settings or Main)
                    currentScreen = previousScreen 
                    
                    // 2. 🚨 FIX: Reset the memory so Settings can securely go back to Main!
                    previousScreen = AppScreen.Main 
                }
            )
            AppScreen.CreatePost -> CreatePostScreen(
                // 🚨 Routes the Back button EXACTLY to the previous screen memory variable
                onNavigateBack = { currentScreen = previousScreen }
            )
            AppScreen.OtherProfile -> ProfileScreen(
                profileUid = viewedUserId, 
                onNavigateToSettings = { },
                onNavigateToEditProfile = { },
                onNavigateToCreatePost = { }, 
                onNavigateBack = { currentScreen = previousScreen }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InterraqtApp(
    initialTab: Int, 
    onTabChange: (Int) -> Unit, 
    onNavigateToSettings: () -> Unit, 
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit, 
    onLogout: () -> Unit
) { 
    // 🚨 FIX 1: Replaced HorizontalPager with a direct, instant State tracker
        var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    // 👇 ADDED: A trigger to tell the Home screen to scroll to the top
    var homeTabRetapTrigger by remember { mutableIntStateOf(0) } 

    // 🚨 FIX 2: THE SILVER BULLET! This permanently memorizes your scroll position on every tab!
    val saveableStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(selectedTab) {
        onTabChange(selectedTab)
    }

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0F16) else Color(0xFFF8F9FA)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT 
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    Scaffold(
        containerColor = bgColor, 
        bottomBar = { 
                        BottomNavigationBar(
                selectedIndex = selectedTab,
                onTabSelected = { index ->
                    // 👇 If already on Home and tapped Home again, fire the trigger!
                    if (index == 0 && selectedTab == 0) {
                        homeTabRetapTrigger++ 
                    } else {
                        selectedTab = index // Normal switch
                    }
                }
            ) 

            
        }
    ) { innerPadding ->
        
        // 🚨 FIX 3: AnimatedContent provides the beautiful native "Back Gesture" slide and fade!
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val duration = 300
                if (targetState > initialState) {
                    // Sliding Forward (Right to Left)
                    (slideInHorizontally(animationSpec = tween(duration)) { width -> width } + fadeIn(animationSpec = tween(duration))) togetherWith 
                    (slideOutHorizontally(animationSpec = tween(duration)) { width -> -width / 2 } + fadeOut(animationSpec = tween(duration)))
                } else {
                    // Sliding Backward (Left to Right) - The classic Back Gesture!
                    (slideInHorizontally(animationSpec = tween(duration)) { width -> -width / 2 } + fadeIn(animationSpec = tween(duration))) togetherWith 
                    (slideOutHorizontally(animationSpec = tween(duration)) { width -> width } + fadeOut(animationSpec = tween(duration)))
                }
            },
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding()) 
                .fillMaxSize()
                .background(bgColor),
            label = "TabAnimation"
        ) { page ->
            
            // 🚨 FIX 4: Wraps the screens so their LazyColumns never lose your scroll progress!
            saveableStateHolder.SaveableStateProvider(key = page) {
                when (page) {
                                        0 -> HomeScreen(
                        onNavigateToCreatePost = onNavigateToCreatePost,
                        homeTabRetapTrigger = homeTabRetapTrigger // 👇 Passed the trigger!
                    ) 

                    1 -> ChatScreen()
                    2 -> ExploreScreen(onNavigateToUserProfile = onNavigateToUserProfile) 
                    3 -> VideoScreen()
                    4 -> ProfileScreen(
                        profileUid = null, 
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToCreatePost = onNavigateToCreatePost,
                        onNavigateBack = null
                    ) 
                }
            }
        }
    }
}

