package com.ioristudios.crossdroid.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a premium cyberpunk neon glow behind the composable.
 */
fun Modifier.neonGlow(
    color: Color = NeonGlow,
    borderRadius: Dp = 16.dp,
    glowRadius: Dp = 16.dp,
    opacity: Float = 0.35f,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        
        // Use software rendering for blur filter if necessary
        if (glowRadius > 0.dp) {
            frameworkPaint.maskFilter = BlurMaskFilter(glowRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        
        frameworkPaint.color = color.copy(alpha = opacity).toArgb()
        
        val left = offsetX.toPx()
        val top = offsetY.toPx()
        val right = size.width + offsetX.toPx()
        val bottom = size.height + offsetY.toPx()
        
        canvas.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}
