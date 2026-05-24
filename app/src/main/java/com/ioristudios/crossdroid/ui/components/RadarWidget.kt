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
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgMain
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

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonPrimary.copy(alpha = 0.16f),
                    AccentCyan.copy(alpha = 0.06f),
                    BgMain.copy(alpha = 0.04f)
                ),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center
        )

        val circlesCount = 4
        for (i in 1..circlesCount) {
            val radius = maxRadius * (i.toFloat() / circlesCount)
            drawCircle(
                color = if (i == circlesCount) {
                    AccentCyan.copy(alpha = 0.42f)
                } else {
                    NeonPrimary.copy(alpha = 0.07f + 0.04f * i)
                },
                radius = radius,
                center = center,
                style = Stroke(width = if (i == circlesCount) 1.4.dp.toPx() else 1.dp.toPx())
            )
        }

        drawLine(
            color = AccentCyan.copy(alpha = 0.18f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = AccentCyan.copy(alpha = 0.18f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
        )

        for (step in 0..7) {
            val angleRad = Math.toRadians((step * 45).toDouble())
            val endX = (center.x + maxRadius * Math.cos(angleRad)).toFloat()
            val endY = (center.y + maxRadius * Math.sin(angleRad)).toFloat()
            drawLine(
                color = AccentCyan.copy(alpha = 0.08f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    AccentCyan.copy(alpha = 0.08f),
                    NeonPrimary.copy(alpha = 0.26f),
                    AccentCyan.copy(alpha = 0.38f)
                ),
                center = center
            ),
            startAngle = angle - 60f,
            sweepAngle = 60f,
            useCenter = true
        )

        val angleRad = Math.toRadians(angle.toDouble())
        val endX = (center.x + maxRadius * Math.cos(angleRad)).toFloat()
        val endY = (center.y + maxRadius * Math.sin(angleRad)).toFloat()
        drawLine(
            color = NeonHighlight,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx()
        )

        drawCircle(
            color = NeonHighlight.copy(alpha = 0.85f),
            radius = 4.dp.toPx(),
            center = center
        )
    }
}
