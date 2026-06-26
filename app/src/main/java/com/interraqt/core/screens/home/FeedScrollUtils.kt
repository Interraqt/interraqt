package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * An enterprise-grade scroll interceptor that allows normal carousel swiping
 * while safely handling boundary limits.
 */
@OptIn(ExperimentalFoundationApi::class)
fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero

        // Allow normal carousel swiping. Only intercept if the Pager is already actively scrolling
        // and we explicitly need to lock vertical container movements.
        if (pagerState.isScrollInProgress) {
            return Offset.Zero // 🛠️ FIX: Change from Offset(available.x, 0f) to let Pager receive drag
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero

        // Only intercept leftover scroll when the Pager hits the hard edges
        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  
            val isSwipingRight = available.x > 0f 
            
            val atEnd = !pagerState.canScrollForward
            val atBeginning = !pagerState.canScrollBackward
            
            // If swiping further left at the last item, or further right at the first item,
            // consume it to stop parent containers from jarringly bouncing.
            if ((isSwipingLeft && atEnd) || (isSwipingRight && atBeginning)) {
                return Offset(available.x, 0f) // 🛠️ FIX: Only consume at physical boundaries
            }
        }
        return Offset.Zero
    }
}
