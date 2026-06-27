package com.interraqt.core.navigation

import androidx.annotation.DrawableRes
import com.interraqt.core.R

// 🚨 FIX: Changed from ImageVector to Int (@DrawableRes) so it can accept your custom XML files!
sealed class Screen(val title: String, @DrawableRes val selectedIcon: Int, @DrawableRes val unselectedIcon: Int) {
    
    // 🚨 UPDATE: Links to your newly uploaded XML drawables
    object Home : Screen("Home", R.drawable.ic_home_filled, R.drawable.ic_home_outline)
    object Chat : Screen("Chat", R.drawable.ic_chat_filled, R.drawable.ic_chat_outline)
    object Explore : Screen("Explore", R.drawable.ic_search_filled, R.drawable.ic_search_outline)
    object Video : Screen("Video", R.drawable.ic_video_filled, R.drawable.ic_video_outline)
    
    // Note: Make sure you also have profile XMLs created in your drawable folder! 
    // If you named them differently, just update the names below.
    object Profile : Screen("Profile", R.drawable.ic_profile_filled, R.drawable.ic_profile_outline) 
}
