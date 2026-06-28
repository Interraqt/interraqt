package com.interraqt.core.screens.home

import androidx.compose.ui.res.painterResource
import com.interraqt.R // Assuming this is your root package based on previous files

import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 1. The Controller: Manages raw animation values independently of recomposition.
 */
class PremiumLikeState {
    var isVisible by mutableStateOf(false)
        private set
    
    var tapPosition by mutableStateOf(Offset.Zero)
        private set

    val scale = Animatable(0f)
    val alpha = Animatable(0f)
    val riseY = Animatable(0f)
    val glowAlpha = Animatable(0f)
    val particleProgress = Animatable(0f)
    val rippleProgress = Animatable(0f)
    val gradientShift = Animatable(0f)

    suspend fun animate(position: Offset) {
        // Reset state instantly on new tap (Interruptible)
        tapPosition = position
        isVisible = true
        
        scale.snapTo(0f)
        alpha.snapTo(1f)
        riseY.snapTo(0f)
        glowAlpha.snapTo(0f)
        particleProgress.snapTo(0f)
        rippleProgress.snapTo(0f)
        gradientShift.snapTo(0f)

        coroutineScope {
            // Ripple Effect (0-300ms)
            launch {
                rippleProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }

            // Heart Appearance & Scale (0-220ms) - Spring Overshoot Physics
            launch {
                scale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(120, easing = FastOutLinearInEasing)
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                )
            }

            // Glow & Bloom (100-350ms)
            launch {
                delay(100)
                glowAlpha.animateTo(0.5f, tween(100, easing = LinearOutSlowInEasing))
                glowAlpha.animateTo(0f, tween(400, easing = FastOutLinearInEasing))
            }

            // Premium Particle Burst (120-450ms)
            launch {
                delay(80)
                particleProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
            }

            // Gradient Flow (0-600ms)
            launch {
                gradientShift.animateTo(1f, tween(600, easing = LinearEasing))
            }

                        // Floating Physics & Instant Disappear
            launch {
                delay(200)
                // Move up without reducing opacity
                riseY.animateTo(-300f, tween(500, easing = FastOutSlowInEasing)) 
                // Instantly disappear at the exact moment the movement finishes
                isVisible = false
            }

        }
    }
}

@Composable
fun rememberPremiumLikeState() = remember { PremiumLikeState() }

/**
 * 2. Particle Engine: Generates randomized vectors for organic dispersion
 */
private class Particle(
    val angle: Float = Random.nextFloat() * 360f,
    val maxDistance: Float = Random.nextFloat() * 100f + 50f,
    val size: Float = Random.nextFloat() * 8f + 4f,
    val colorIndex: Int = Random.nextInt(3)
)

/**
 * 3. The Renderer: 100% GPU accelerated. Never triggers Compose layout passes during animation.
 */
@Composable
fun PremiumLikeOverlay(state: PremiumLikeState, modifier: Modifier = Modifier) {
    if (!state.isVisible) return

    val density = LocalDensity.current
    val particles = remember { List(16) { Particle() } }
    
    // Premium Instagram Color Palette
    val colors = listOf(
        Color(0xFFFF3366), // Pink
        Color(0xFFE1306C), // Magenta
        Color(0xFFC13584), // Purple
        Color(0xFFFD1D1D), // Orange
        Color(0xFFF56040)  // Warm Yellow
    )

    Box(modifier = modifier.fillMaxSize()) {
        
                // Canvas for Ripple & Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 👇 Shift the explosion center 70dp up so it perfectly matches the heart!
            val spawnAboveThumbPx = 70.dp.toPx()
            val center = Offset(state.tapPosition.x, state.tapPosition.y - spawnAboveThumbPx)
            
            // 1. Draw Ripple

            if (state.rippleProgress.value in 0.01f..0.99f) {
                val rippleRadius = state.rippleProgress.value * 250f
                val rippleAlpha = 1f - state.rippleProgress.value
                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha * 0.3f),
                    radius = rippleRadius,
                    center = center
                )
            }
                 
       
            // 2. Draw Particle Burst
            if (state.particleProgress.value > 0f) {
                particles.forEach { particle ->
                    val distance = particle.maxDistance * state.particleProgress.value
                    val rad = Math.toRadians(particle.angle.toDouble())
                    val x = center.x + (distance * cos(rad)).toFloat()
                    val y = center.y + (distance * sin(rad)).toFloat()
                    
                    val particleAlpha = 1f - state.particleProgress.value
                    val particleScale = 1f - (state.particleProgress.value * 0.5f)
                    
                    drawCircle(
                        color = colors[particle.colorIndex].copy(alpha = particleAlpha),
                        radius = particle.size * particleScale,
                        center = Offset(x, y)
                    )
                }
            }
        }

                // 3. Heart Renderer
        // 👇 MANUALLY CHANGE HEART SIZE HERE (e.g. 100.dp for smaller, 140.dp for larger)
        val heartSize = 100.dp 
        
        val heartSizePx = with(density) { heartSize.toPx() }
        
        // 👇 MANUALLY CHANGE HEIGHT OFFSET HERE (70.dp pushes it above your thumb)
        val spawnAboveThumbPx = with(density) { 70.dp.toPx() } 

                Icon(
            painter = painterResource(id = R.drawable.ic_custom_heart),

         
            contentDescription = null,
            tint = Color.White, 
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (state.tapPosition.x - (heartSizePx / 2)).roundToInt(),
                        // 👇 Subtracting spawnAboveThumbPx pushes the start position higher
                        y = (state.tapPosition.y - (heartSizePx / 2) - spawnAboveThumbPx).roundToInt()
                    )
                }

                .size(heartSize)
                                .graphicsLayer {
                    // GPU Accelerated Transforms
                    scaleX = state.scale.value
                    scaleY = state.scale.value
                    translationY = state.riseY.value
                    alpha = state.alpha.value
                    rotationZ = (state.scale.value - 1f) * 15f 
                    
                    

                    // 🚨 ADD THIS: Forces the GPU to perfectly mask the gradient to the vector path!
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    // Animated Gradient Flow
                    val shift = state.gradientShift.value * size.height
                    val brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, shift),
                        end = Offset(size.width, size.height + shift)
                    )
                    onDrawWithContent {
                        drawContent()
                        // 🚨 UPDATE THIS: Change SrcAtop to SrcIn
                        drawRect(brush, blendMode = BlendMode.SrcIn)
                    }
                }

        )
    }
}
