package com.interraqt.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.* // Powers the sliding animations
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
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.interraqt.core.auth.LoginScreen
import com.interraqt.core.auth.SignupScreen
import com.interraqt.core.navigation.BottomNavigationBar
import com.interraqt.core.screens.*

enum class AppScreen {
    Login, Signup, Main
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
    
    var currentScreen by remember { 
        mutableStateOf(if (auth.currentUser != null) AppScreen.Main else AppScreen.Login) 
    }

    // Wrap the screens in an animation engine
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (initialState == AppScreen.Login && targetState == AppScreen.Signup) {
                // Slide left when going to Signup
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith 
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            } else if (initialState == AppScreen.Signup && targetState == AppScreen.Login) {
                // Slide right when going back to Login
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith 
                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            } else {
                // Smooth fade when entering the main app
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "ScreenTransition"
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.Login -> LoginScreen(
                onNavigateToSignup = { currentScreen = AppScreen.Signup },
                onLoginSuccess = { currentScreen = AppScreen.Main }
            )
            AppScreen.Signup -> SignupScreen(
                onNavigateToLogin = { currentScreen = AppScreen.Login },
                onSignupSuccess = { currentScreen = AppScreen.Main }
            )
            AppScreen.Main -> InterraqtApp(
                onLogout = { currentScreen = AppScreen.Login } 
            ) 
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InterraqtApp(onLogout: () -> Unit) { 
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.Black else Color.White

    Scaffold(
        containerColor = bgColor, 
        bottomBar = { 
            BottomNavigationBar(
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            ) 
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(bgColor)
        ) { page ->
            when (page) {
                0 -> HomeScreen()
                1 -> ChatScreen()
                2 -> ExploreScreen()
                3 -> VideoScreen()
                4 -> ProfileScreen(onLogout = onLogout) 
            }
        }
    }
}
