package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.DeviceNode
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun NearbyDeviceChip(
    device: DeviceNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Constant pulsing animation for the discoverable node
    val infiniteTransition = rememberInfiniteTransition(label = "NodePulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val glowOpacity by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlowOpacity"
    )

    val deviceIcon = if (device.osType == "android") Icons.Default.PhoneAndroid else Icons.Default.Computer
    val deviceColor = if (device.osType == "android") ColorSuccess else Color(0xFF00E5FF)

    Row(
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(Radii.ButtonSmall))
            .neonGlow(
                color = deviceColor,
                borderRadius = Radii.ButtonSmall,
                glowRadius = 10.dp,
                opacity = glowOpacity
            )
            .background(BgElevated)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(deviceColor.copy(alpha = 0.7f), NeonPrimary.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(Radii.ButtonSmall)
            )
            .clickable {
                HapticHelper.triggerMedium(context)
                onClick()
            }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(deviceColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceIcon,
                contentDescription = "Device OS",
                tint = deviceColor,
                modifier = Modifier.size(IconSize.Small)
            )
        }
        
        Spacer(modifier = Modifier.width(Spacing.Small))
        
        Text(
            text = device.name,
            color = TextStrong,
            style = CustomTypography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp
            )
        )
    }
}
