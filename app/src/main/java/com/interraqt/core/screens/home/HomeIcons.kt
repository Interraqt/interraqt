package com.interraqt.core.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.interraqt.core.R

object HomeScreenIcons {
    val Like @Composable get() = painterResource(id = R.drawable.ic_like)
    val BookmarkOutline @Composable get() = painterResource(id = R.drawable.ic_bookmark_outline)
    val BookmarkFilled @Composable get() = painterResource(id = R.drawable.ic_bookmark_filled)
    val Comment @Composable get() = painterResource(id = R.drawable.ic_comment)
    val Share @Composable get() = painterResource(id = R.drawable.ic_share)
    val Interested @Composable get() = painterResource(id = R.drawable.ic_interested)
    val NotInterested @Composable get() = painterResource(id = R.drawable.ic_not_interested)
}
