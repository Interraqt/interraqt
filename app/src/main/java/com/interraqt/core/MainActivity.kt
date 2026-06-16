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
import com.interraqt.core.navigation.BottomNavigationBar
import com.interraqt.core.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InterraqtApp()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InterraqtApp() {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // Detect system theme for the entire app background
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.Black else Color.White

    Scaffold(
        containerColor = bgColor, // Applies theme to the space behind the navigation bar
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
