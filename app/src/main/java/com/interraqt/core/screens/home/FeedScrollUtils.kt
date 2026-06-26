package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * A stateless, high-performance scroll interceptor.
 * Calculates vector trajectories instantly to prevent axis-locking bugs and recomposition lag.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberDirectionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection {
    // 🚨 FIX 1: Removed mutableStateOf. The connection is now purely mathematical and lag-free.
    return remember(pagerState) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                // 🚨 FIX 2: STATELESS LOCK
                // Instantly block horizontal pager movement if the user is scrolling vertically.
                // Returning Offset(x = available.x) tells the Pager "The swipe was consumed, do not move".
                // Leaving y = 0f allows the vertical main feed to scroll smoothly.
                if (abs(available.y) > abs(available.x)) {
                    return Offset(x = available.x, y = 0f)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                // 🚨 FIX 3: PROTECT TABS
                if (abs(available.x) > 0f) {
                    val isSwipingLeft = available.x < 0f
                    val isSwipingRight = available.x > 0f

                    val atEnd = !pagerState.canScrollForward
                    val atBeginning = !pagerState.canScrollBackward

                    // If at the edge of the carousel, let the swipe pass through to switch Bottom Tabs
                    if ((isSwipingLeft && atEnd) || (isSwipingRight && atBeginning)) {
                        return Offset.Zero
                    }

                    // Otherwise, consume the leftover swipe so the Bottom Tabs don't twitch
                    return Offset(x = available.x, y = 0f)
                }
                
                return Offset.Zero
            }
            
            // Fling overrides are no longer necessary because we aren't maintaining state!
        }
    }
}
