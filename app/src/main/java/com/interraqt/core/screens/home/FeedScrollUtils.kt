package com.interraqt.core.screens.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.foundation.pager.PagerState
import kotlin.math.abs

/**
 * An enterprise-grade scroll interceptor that ensures smooth nested horizontal navigation swiping.
 * Consumes internal carousel drags safely while cleanly bubbling out edge gestures to parent tabs.
 */
fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    // 1. Pre-scroll hooks capture multi-direction momentum changes instantly before rendering
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Enforce interaction checks exclusively on physical user touch inputs
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        // If the Pager is currently executing a smooth scroll animation, lock out the parent views
        if (pagerState.isScrollInProgress) {
            return Offset(available.x, 0f)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Ignore automated framework events or layout passes
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  
            val isSwipingRight = available.x > 0f 
            
            // 2. Pair canScroll checks with animation status verification
            val atEnd = !pagerState.canScrollForward && !pagerState.isScrollInProgress
            val atBeginning = !pagerState.canScrollBackward && !pagerState.isScrollInProgress
            
            // Clean hand-off to allow parent navigation components to swap active tabs
            if (isSwipingLeft && atEnd) return Offset.Zero
            if (isSwipingRight && atBeginning) return Offset.Zero
            
            // Prevent child-level micro-movements from leaking upward
            return Offset(available.x, 0f)
        }
        return Offset.Zero
    }
}
