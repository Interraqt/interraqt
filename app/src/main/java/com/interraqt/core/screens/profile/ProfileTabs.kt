package com.interraqt.core.screens.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileTabs(
    tabPagerState: PagerState,
    primaryOrange: Color,
    subTextColor: Color,
    coroutineScope: CoroutineScope
) {
    val tabTitles = listOf("Collections", "Videos", "Photos")
    
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        val tabWidth = maxWidth / 3
        val exactPage = tabPagerState.currentPage + tabPagerState.currentPageOffsetFraction
        val indicatorOffset = tabWidth * exactPage

        Box(modifier = Modifier.offset(x = indicatorOffset).width(tabWidth).align(Alignment.BottomStart), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.width(90.dp).height(2.dp).background(primaryOrange, RoundedCornerShape(1.dp)))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = tabPagerState.currentPage == index
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1.0f, animationSpec = tween(300), label = "")
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.width(tabWidth).clickable { coroutineScope.launch { tabPagerState.animateScrollToPage(index) } }
                ) {
                    Text(
                        text = title, 
                        color = if (isSelected) primaryOrange else subTextColor, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                        fontSize = 15.sp,
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
