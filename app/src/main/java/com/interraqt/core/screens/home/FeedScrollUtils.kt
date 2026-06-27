package com.interraqt.core.screens.home

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults


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

                // 🚨 FIX: Vertical Bias Threshold. 
                // We multiply X by 0.5f. This forces the carousel to ONLY trigger on 
                // highly deliberate, straight horizontal swipes. 
                // Rapid, curved "L" swipes will smoothly fall back to vertical scrolling!
                if (abs(available.y) > (abs(available.x) * 1.2f)) {
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

                // 🚨 FIX: Apply the same Vertical Bias here so an arced vertical swipe 
                // doesn't accidentally trigger a Bottom Tab switch at the edges.
                if (abs(available.y) > (abs(available.x) * 1.2f)) {
                    return Offset.Zero 
                }

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

/**
 * Tricks Compose into thinking the user swiped harder than they actually did.
 * This removes the "heavy" friction from LazyColumn and makes scrolling effortless.
 */
@Composable
fun rememberBoostedFlingBehavior(velocityMultiplier: Float = 1.5f): FlingBehavior {
    val baseBehavior = ScrollableDefaults.flingBehavior()
    return remember(baseBehavior, velocityMultiplier) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                // 🚨 FIX: We take your thumb's natural speed and multiply it!
                return with(baseBehavior) {
                    performFling(initialVelocity * velocityMultiplier)
                }
            }
        }
    }
}




