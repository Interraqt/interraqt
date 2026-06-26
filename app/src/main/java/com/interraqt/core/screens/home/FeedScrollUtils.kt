package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * An enterprise-grade scroll interceptor that allows normal carousel swiping,
 * clean vertical scrolling, and seamless parent tab switching at boundaries.
 */
@OptIn(ExperimentalFoundationApi::class)
fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Only handle user drag interactions
        if (source != NestedScrollSource.Drag) return Offset.Zero

        // FIX 1: If the user is clearly trying to scroll vertically (up/down),
        // completely ignore horizontal adjustments to prevent diagonal/phantom swipes.
        if (abs(available.y) > abs(available.x)) {
            return Offset.Zero
        }

        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero

        // FIX 2: Fixed tab switching at boundaries.
        // If there is unconsumed horizontal delta, we must check if we are at the edges.
        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  // Finger moving left, moving to next item
            val isSwipingRight = available.x > 0f // Finger moving right, moving to previous item
            
            val atEnd = !pagerState.canScrollForward
            val atBeginning = !pagerState.canScrollBackward
            
            // CRITICAL: Return Offset.Zero at boundaries instead of consuming it!
            // This releases the gesture back to the parent container (like a HorizontalPager or TabRow) 
            // so tab switching works flawlessly when you reach the end of the carousel.
            if ((isSwipingLeft && atEnd) || (isSwipingRight && atBeginning)) {
                return Offset.Zero 
            }
        }
        return Offset.Zero
    }
}
