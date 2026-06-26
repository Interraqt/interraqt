package com.interraqt.core.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi // 🚨 REQUIRED IMPORT
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * An enterprise-grade scroll interceptor...
 */
@OptIn(ExperimentalFoundationApi::class) // 🚨 REQUIRED ANNOTATION
fun directionalScrollConnection(
    pagerState: PagerState
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // ... (Your code remains the same)
      
if (source != NestedScrollSource.Drag) return Offset.Zero

        
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
        // ... (Your code remains the same)
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        if (abs(available.x) > 0f) {
            val isSwipingLeft = available.x < 0f  
            val isSwipingRight = available.x > 0f 
            
            val atEnd = !pagerState.canScrollForward && !pagerState.isScrollInProgress
            val atBeginning = !pagerState.canScrollBackward && !pagerState.isScrollInProgress
            
            if (isSwipingLeft && atEnd) return Offset.Zero
            if (isSwipingRight && atBeginning) return Offset.Zero
            
            return Offset(available.x, 0f)
        }
        return Offset.Zero
    }
}
