package com.interraqt.core.screens.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

fun directionalScrollConnection(
    onHorizontalScroll: (Boolean) -> Unit
): NestedScrollConnection = object : NestedScrollConnection {
    var isHorizontalDrag = false
    var isVerticalDrag = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!isHorizontalDrag && !isVerticalDrag) {
            if (abs(available.x) > abs(available.y) * 1.2f) { 
                isHorizontalDrag = true
                onHorizontalScroll(true) 
            } else if (abs(available.y) > abs(available.x)) {
                isVerticalDrag = true
                onHorizontalScroll(false) 
            }
        }
        
        // 🚨 FIX: We MUST return Offset.Zero here so the HorizontalPager actually gets the swipe!
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // 🚨 FIX: If the user is swiping horizontally and reaches the end of the image carousel,
        // we consume the "leftover" swipe here so it DOES NOT trigger the bottom navigation tabs!
        if (isHorizontalDrag && available.x != 0f) {
            return Offset(available.x, 0f)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        isHorizontalDrag = false
        isVerticalDrag = false
        onHorizontalScroll(true) 
        return Velocity.Zero
    }
}
