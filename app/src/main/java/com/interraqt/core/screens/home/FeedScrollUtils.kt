package com.interraqt.core.screens.home

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // If there is leftover horizontal swipe (the image pager didn't use it)
        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  // Finger dragging left (going to next tab)
            val isSwipingRight = available.x > 0f // Finger dragging right (going to previous tab)
            
            val atEnd = !pagerState.canScrollForward
            val atBeginning = !pagerState.canScrollBackward
            
            // ISSUE 2 FIX: If at the edge of the images, let the swipe bubble up to switch bottom nav tabs!
            if (isSwipingLeft && atEnd) return Offset.Zero
            if (isSwipingRight && atBeginning) return Offset.Zero
            
            // ISSUE 1 FIX: If in the middle of images, consume the leftover so tabs don't twitch
            return Offset(available.x, 0f)
        }
        return Offset.Zero
    }
}
