package com.interraqt.core.screens.home

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * State machine representing the exclusive ownership of a gesture sequence.
 */
class GestureLockState(private val touchSlop: Float) {
    enum class Lock { IDLE, EVALUATING, HORIZONTAL, VERTICAL }
    
    var currentLock = Lock.IDLE
        private set
        
    private var accX = 0f
    private var accY = 0f

    /**
     * Resets the gesture sequence. Must be called immediately on finger down.
     */
    fun reset() {
        currentLock = Lock.IDLE
        accX = 0f
        accY = 0f
    }

    /**
     * Evaluates accumulated movement to determine user intention.
     */
    fun evaluate(dx: Float, dy: Float) {
        if (currentLock == Lock.HORIZONTAL || currentLock == Lock.VERTICAL) return
        
        if (currentLock == Lock.IDLE) {
            currentLock = Lock.EVALUATING
        }
        
        accX += dx
        accY += dy

        val absX = abs(accX)
        val absY = abs(accY)

        // Once the user exceeds the physical touch slop, lock the intention.
        if (absX > touchSlop || absY > touchSlop) {
            // Social Media Bias: Users scrolling a feed often have sloppy vertical swipes.
            // We apply a 0.85 multiplier to X to strongly bias ownership toward the vertical feed.
            currentLock = if (absY > absX * 0.85f) Lock.VERTICAL else Lock.HORIZONTAL
        }
    }
}

/**
 * A highly precise connection that routes scroll deltas based on locked intention.
 */
@Composable
fun rememberDirectionalScrollConnection(
    gestureState: GestureLockState
): NestedScrollConnection {
    return remember(gestureState) {
        object : NestedScrollConnection {
            
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                gestureState.evaluate(available.x, available.y)

                return when (gestureState.currentLock) {
                    GestureLockState.Lock.VERTICAL -> {
                        // EXCLUSIVE VERTICAL: 
                        // Consume all X movement so the HorizontalPager cannot process it.
                        // Pass Y through untouched for the LazyColumn.
                        Offset(x = available.x, y = 0f)
                    }
                    GestureLockState.Lock.HORIZONTAL -> {
                        // EXCLUSIVE HORIZONTAL:
                        // Consume all Y movement so the LazyColumn cannot process it.
                        // Pass X through untouched for the HorizontalPager.
                        Offset(x = 0f, y = available.y)
                    }
                    else -> Offset.Zero // Still evaluating, let Compose handle micro-jitters
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                // If locked horizontally, swallow any unconsumed X movement at the edge 
                // of the carousel so it doesn't accidentally swipe between Bottom Tabs.
                if (gestureState.currentLock == GestureLockState.Lock.HORIZONTAL) {
                    return Offset(available.x, 0f)
                }
                
                return Offset.Zero
            }
        }
    }
}

/**
 * Amplifies initial velocity for a physical, heavy-momentum feed feel.
 */
@Composable
fun rememberBoostedFlingBehavior(velocityMultiplier: Float = 1.4f): FlingBehavior {
    val baseBehavior = ScrollableDefaults.flingBehavior()
    return remember(baseBehavior, velocityMultiplier) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return with(baseBehavior) {
                    performFling(initialVelocity * velocityMultiplier)
                }
            }
        }
    }
}
