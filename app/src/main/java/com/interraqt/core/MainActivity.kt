package com.interraqt.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

// 1. We define the three possible states the app can be in
enum class AppScreen {
    Login, Signup, Main
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 2. We set the Bouncer as the very first thing the app loads
            RootNavigation() 
        }
    }
}

// 3. The Bouncer logic
@Composable
fun RootNavigation() {
    val auth = FirebaseAuth.getInstance()
    
    // Check if the user is already logged in right now
    var currentScreen by remember { 
        mutableStateOf(if (auth.currentUser != null) AppScreen.Main else AppScreen.Login) 
    }

    // Route the user to the correct screen and handle button clicks
    when (currentScreen) {
        AppScreen.Login -> LoginScreen(
            onNavigateToSignup = { currentScreen = AppScreen.Signup },
            onLoginSuccess = { currentScreen = AppScreen.Main }
        )
        AppScreen.Signup -> SignupScreen(
            onNavigateToLogin = { currentScreen = AppScreen.Login },
            onSignupSuccess = { currentScreen = AppScreen.Main }
        )
        AppScreen.Main -> InterraqtApp() // This is your secure main feed!
    }
}

// 4. Your existing app structure remains completely untouched and safe below
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InterraqtApp() {
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
                4 -> ProfileScreen()
            }
        }
    }
}
