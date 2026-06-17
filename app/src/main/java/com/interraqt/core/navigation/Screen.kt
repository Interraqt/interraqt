package com.interraqt.core.navigation

import com.interraqt.core.R

sealed class Screen(val title: String, val selectedIcon: Int, val unselectedIcon: Int) {
    object Home : Screen("Home", R.drawable.ic_home_filled, R.drawable.ic_home_outline)
    object Chat : Screen("Chat", R.drawable.ic_chat_filled, R.drawable.ic_chat_outline)
    object Explore : Screen("Explore", R.drawable.ic_explore_filled, R.drawable.ic_explore_outline)
    object Video : Screen("Video", R.drawable.ic_video_filled, R.drawable.ic_video_outline)
    object Profile : Screen("Profile", R.drawable.ic_profile_filled, R.drawable.ic_profile_outline)
}
