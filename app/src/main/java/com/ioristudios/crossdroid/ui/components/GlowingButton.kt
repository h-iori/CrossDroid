package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowColor: Color = NeonPrimary,
    glowRadius: Dp = 12.dp,
    hapticIntensity: String = "medium"
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        label = "ButtonPressScaleAnimation"
    )

    val opacity = animateFloatAsState(
        targetValue = if (enabled) (if (isPressed) 0.45f else 0.25f) else 0f,
        label = "ButtonGlowAlpha"
    )

    val buttonColor = if (enabled) {
        Brush.horizontalGradient(
            colors = listOf(NeonPrimary, NeonHighlight)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF1E1E28), Color(0xFF161622))
        )
    }

    val textColor = if (enabled) TextStrong else TextStrong.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(Radii.ButtonSmall))
            .then(
                if (enabled) {
                    Modifier.neonGlow(
                        color = glowColor,
                        borderRadius = Radii.ButtonSmall,
                        glowRadius = glowRadius,
                        opacity = opacity.value
                    )
                } else Modifier
            )
            .background(buttonColor)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(NeonHighlight, NeonPrimary)
                    } else {
                        listOf(Color(0xFF2A2A38), Color(0xFF1F1F2C))
                    }
                ),
                shape = RoundedCornerShape(Radii.ButtonSmall)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                when (hapticIntensity) {
                    "light" -> HapticHelper.triggerLight(context)
                    "medium" -> HapticHelper.triggerMedium(context)
                    "strong" -> HapticHelper.triggerStrong(context)
                }
                onClick()
            }
            .padding(vertical = Spacing.Medium, horizontal = Spacing.Large),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = CustomTypography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}
