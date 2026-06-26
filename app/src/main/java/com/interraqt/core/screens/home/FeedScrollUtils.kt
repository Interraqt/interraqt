package com.interraqt.core.screens.home

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
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * Creates and remembers a robust directional scroll connection that locks the scroll axis 
 * per-gesture. Guarantees clean reset even during high-frequency vertical/horizontal handoffs.
 */
@Composable
fun rememberDirectionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection {
    // Track lock state using a standard state object
    var currentGestureLock by remember { mutableStateOf<Orientation?>(null) }

    return remember(pagerState) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Ignore programmatic scrolls (animations, snap-to-page, etc.)
                if (source != NestedScrollSource.Drag) return Offset.Zero

                val deltaX = abs(available.x)
                val deltaY = abs(available.y)

                // 1. Establish the lock. 
                // Production tip: Increase threshold slightly to 5f-8f to give the user 
                // a tiny "dead zone" to establish clear intent, preventing micro-jitters.
                if (currentGestureLock == null && (deltaX > 5f || deltaY > 5f)) {
                    currentGestureLock = if (deltaY > deltaX) {
                        Orientation.Vertical
                    } else {
                        Orientation.Horizontal
                    }
                }

                // 2. Enforce the axis locking
                return when (currentGestureLock) {
                    Orientation.Vertical -> {
                        // Vertical scroll intent: absorb horizontal movement entirely
                        // This prevents the horizontal Pager from breaking out of the vertical scroll.
                        Offset(x = available.x, y = 0f)
                    }
                    Orientation.Horizontal -> {
                        // Horizontal swipe intent: let it flow through completely to the pager
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

            // 3. FIX: intercept at onPreFling
            // onPreFling is guaranteed to run BEFORE flings hit the system, 
            // right when the dragging finger lifts off the glass.
            override suspend fun onPreFling(available: Velocity): Velocity {
                currentGestureLock = null // Instantly clear lock for the next gesture
                return Velocity.Zero // Let the velocity pass naturally
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                // Backup safety reset
                currentGestureLock = null 
                return super.onPostFling(consumed, available)
            }
        }
    }
}
