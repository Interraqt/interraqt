package com.interraqt.core

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

enum class AppScreen {
    Login, Signup, Main, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    
    var savedTab by remember { mutableIntStateOf(0) }
    
    var globalUsername by remember { mutableStateOf("...") }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                globalUsername = doc.getString("username") ?: "Unknown"
            }
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (initialState == AppScreen.Login && targetState == AppScreen.Signup) {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            } else if (initialState == AppScreen.Signup && targetState == AppScreen.Login) {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            } else if (targetState == AppScreen.Settings) {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 2 })
            } else if (initialState == AppScreen.Settings && targetState == AppScreen.Main) {
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
                onLoginSuccess = { 
                    savedTab = 0 
                    currentScreen = AppScreen.Main 
                }
            )
            AppScreen.Signup -> SignupScreen(
                onNavigateToLogin = { currentScreen = AppScreen.Login },
                onSignupSuccess = { 
                    savedTab = 0 
                    currentScreen = AppScreen.Main 
                }
            )
            AppScreen.Main -> InterraqtApp(
                initialTab = savedTab, 
                globalUsername = globalUsername, 
                onTabChange = { savedTab = it }, 
                onNavigateToSettings = { currentScreen = AppScreen.Settings },
                onLogout = { 
                    savedTab = 0 
                    currentScreen = AppScreen.Login 
                }
            )
            AppScreen.Settings -> SettingsScreen(
                username = globalUsername, 
                onUsernameUpdated = { globalUsername = it }, 
                onNavigateBack = { currentScreen = AppScreen.Main },
                onLogout = { 
                    savedTab = 0 
                    currentScreen = AppScreen.Login 
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InterraqtApp(
    initialTab: Int, 
    globalUsername: String,
    onTabChange: (Int) -> Unit, 
    onNavigateToSettings: () -> Unit, 
    onLogout: () -> Unit
) { 
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onTabChange(pagerState.currentPage)
    }

    val isDark = isSystemInDarkTheme()
    // 🚨 PREMIUM GLOBAL BACKGROUND 
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
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        val currentPage = pagerState.currentPage
                        if (abs(currentPage - index) > 1) {
                            val adjacentPage = if (index > currentPage) index - 1 else index + 1
                            pagerState.scrollToPage(adjacentPage)
                        }
                        pagerState.animateScrollToPage(index)
                    }
                }
            ) 
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding()) 
                .fillMaxSize()
                .background(bgColor)
        ) { page ->
            when (page) {
                0 -> HomeScreen()
                1 -> ChatScreen()
                2 -> ExploreScreen()
                3 -> VideoScreen()
                4 -> ProfileScreen(username = globalUsername, onNavigateToSettings = onNavigateToSettings) 
            }
        }
    }
}
