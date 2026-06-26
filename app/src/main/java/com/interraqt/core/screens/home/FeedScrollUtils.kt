package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * Creates and remembers a smooth directional scroll connection that locks the scroll axis 
 * per-gesture to completely eliminate jitter and heavy vertical scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberDirectionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection {
    // Keeps track of the locked direction for the CURRENT active gesture.
    // Clears out automatically when the drag ends.
    var currentGestureLock by remember { mutableStateOf<Orientation?>(null) }

    return remember(pagerState) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                val deltaX = abs(available.x)
                val deltaY = abs(available.y)

                // 1. If no axis is locked yet, wait for a clear intent threshold (e.g., > 3 pixels)
                if (currentGestureLock == null && (deltaX > 3f || deltaY > 3f)) {
                    currentGestureLock = if (deltaY > deltaX) {
                        Orientation.Vertical
                    } else {
                        Orientation.Horizontal
                    }
                }

                // 2. Enforce the lock for the remainder of this continuous touch gesture
                return when (currentGestureLock) {
                    Orientation.Vertical -> {
                        // User is scrolling vertically. Absorb ALL horizontal deltas 
                        // so the Pager never sees them and stays perfectly still.
                        Offset(x = available.x, y = 0f)
                    }
                    Orientation.Horizontal -> {
                        // User is swiping horizontally. Let it pass through normally.
                        Offset.Zero
                    }
                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                // Only allow edge-handoff if the user actually intended to swipe horizontally
                if (currentGestureLock == Orientation.Horizontal && abs(available.x) > 0f) {
                    val isSwipingLeft = available.x < 0f  
                    val isSwipingRight = available.x > 0f 
                    
                    val atEnd = !pagerState.canScrollForward
                    val atBeginning = !pagerState.canScrollBackward
                    
                    if ((isSwipingLeft && atEnd) || (isSwipingRight && atBeginning)) {
                        return Offset.Zero 
                    }
                }
                return Offset.Zero
            }

            // 3. CRITICAL FOR SMOOTHNESS: Reset the lock when the user lifts their finger
            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                currentGestureLock = null // Free the lock so the next touch starts fresh
                return super.onPostFling(consumed, available)
            }
        }
    }
}
