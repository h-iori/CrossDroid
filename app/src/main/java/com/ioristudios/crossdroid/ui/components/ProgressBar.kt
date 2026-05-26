package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary

@Composable
fun CyberpunkProgressBar(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(150),
        label = "ProgressFillingAnimation"
    )

    val isFinished = progress >= 1.0f

    val startColor by animateColorAsState(
        targetValue = if (isFinished) ColorSuccess else NeonPrimary,
        label = "ProgressBarStartColor"
    )

    val endColor by animateColorAsState(
        targetValue = if (isFinished) Color(0xFF00FFC4) else NeonHighlight,
        label = "ProgressBarEndColor"
    )

    // Shimmer effect variables
    val infiniteTransition = rememberInfiniteTransition(label = "ProgressBarShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    val shimmerAlpha = if (progress > 0f && progress < 1f) 0.65f else 0f

    // One-shot completion flash
    val flashAnim = remember { Animatable(0f) }
    LaunchedEffect(isFinished) {
        if (isFinished) {
            flashAnim.snapTo(1f)
            flashAnim.animateTo(0f, animationSpec = tween(450))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(BgSurface)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(startColor, endColor)
                        )
                    )
                    .drawBehind {
                        // Drawing the scanning shimmer light flare
                        if (shimmerAlpha > 0f) {
                            val width = size.width
                            val shimmerX = width * shimmerOffset
                            val shimmerWidth = 80.dp.toPx()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = shimmerAlpha),
                                        Color.Transparent
                                    ),
                                    startX = shimmerX - shimmerWidth / 2,
                                    endX = shimmerX + shimmerWidth / 2
                                )
                            )
                        }

                        // Drawing the completion flash overlay
                        val currentFlash = flashAnim.value
                        if (currentFlash > 0f) {
                            drawRect(
                                color = Color.White.copy(alpha = currentFlash)
                            )
                        }
                    }
            )
        }
    }
}
