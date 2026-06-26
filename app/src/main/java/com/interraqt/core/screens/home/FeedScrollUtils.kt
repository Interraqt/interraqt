package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * An advanced scroll interceptor that explicitly cuts off diagonal twitches
 * and isolates sharp vertical vs horizontal movements.
 */
@OptIn(ExperimentalFoundationApi::class)
fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero

        // 🛠️ FIX: Hard-intercept diagonal flings and instant vertical overrides.
        // If the user's intent is moving mostly up/down, we MUST consume the horizontal portion 
        // entirely so the HorizontalPager never spots a false horizontal drag trigger.
        if (abs(available.y) > abs(available.x)) {
            return Offset(available.x, 0f) // Absorb the X movement completely
        }

        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero

        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  
            val isSwipingRight = available.x > 0f 
            
            val atEnd = !pagerState.canScrollForward
            val atBeginning = !pagerState.canScrollBackward
            
            // Seamless handoff back to the parent layout/tabs at edges
            if ((isSwipingLeft && atEnd) || (isSwipingRight && atBeginning)) {
                return Offset.Zero 
            }
        }
        return Offset.Zero
    }
}
