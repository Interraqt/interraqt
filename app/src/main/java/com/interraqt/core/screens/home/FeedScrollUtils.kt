package com.interraqt.core.screens.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * 🚨 INSTAGRAM SCROLL ALGORITHM
 * Instantly measures thumb angle. If vertical > horizontal, it locks the pager. 
 * If horizontal > vertical, it locks the feed.
 */
fun instagramNestedScrollConnection(
    onHorizontalScroll: (Boolean) -> Unit
): NestedScrollConnection = object : NestedScrollConnection {
    var isHorizontalDrag = false
    var isVerticalDrag = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Detect primary direction on the very first pixel of drag
        if (!isHorizontalDrag && !isVerticalDrag) {
            if (abs(available.x) > abs(available.y) * 1.2f) { // 1.2x multiplier for strictness
                isHorizontalDrag = true
                onHorizontalScroll(true) // Enable horizontal image swipes
            } else {
                isVerticalDrag = true
                onHorizontalScroll(false) // Lock horizontal image swipes
            }
        }
        
        // If it's a horizontal scroll, consume the event so vertical list doesn't bob up and down
        return if (isHorizontalDrag) available else Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // Reset flags instantly when the user lifts their finger
        isHorizontalDrag = false
        isVerticalDrag = false
        onHorizontalScroll(true) 
        return Velocity.Zero
    }
}
