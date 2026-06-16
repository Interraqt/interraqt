package com.interraqt.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.interraqt.core.navigation.BottomNavigationBar
import com.interraqt.core.navigation.Screen
import com.interraqt.core.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InterraqtApp()
        }
    }
}

@Composable
fun InterraqtApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Explore.route) { ExploreScreen() }
            composable(Screen.Video.route) { VideoScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}
