package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary

@Composable
fun RadarWidget(
    modifier: Modifier = Modifier,
    sweepDuration: Int = 3000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweepAnimationTransition")
    
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(sweepDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2
        
        // 1. Concentric Circles
        val circlesCount = 4
        for (i in 1..circlesCount) {
            val radius = maxRadius * (i.toFloat() / circlesCount)
            drawCircle(
                color = NeonPrimary.copy(alpha = 0.05f + 0.04f * i),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // 2. Cross Grid Lines
        drawLine(
            color = NeonPrimary.copy(alpha = 0.15f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = NeonPrimary.copy(alpha = 0.15f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
        )
        
        // 3. Sweeping gradient slice
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonPrimary.copy(alpha = 0.25f),
                    NeonPrimary.copy(alpha = 0.4f)
                ),
                center = center
            ),
            startAngle = angle - 60f,
            sweepAngle = 60f,
            useCenter = true
        )
        
        // 4. Glowing Sweep Laser Edge
        val angleRad = Math.toRadians(angle.toDouble())
        val endX = (center.x + maxRadius * Math.cos(angleRad)).toFloat()
        val endY = (center.y + maxRadius * Math.sin(angleRad)).toFloat()
        drawLine(
            color = NeonHighlight,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
