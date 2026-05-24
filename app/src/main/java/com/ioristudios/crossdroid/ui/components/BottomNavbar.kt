package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun BottomNavbar(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Pulse animation for the central button
    val infiniteTransition = rememberInfiniteTransition(label = "CenterLogoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoGlowOpacity"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Core Navbar Background Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(BgElevated.copy(alpha = 0.95f))
                .border(
                    width = 1.dp,
                    color = BgSurface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Devices Tab
            val isDevices = currentScreen == Screen.DEVICES
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.DEVICES, context) }
                    .padding(vertical = Spacing.Small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Devices",
                    tint = if (isDevices) NeonHighlight else TextMuted,
                    modifier = Modifier
                        .size(IconSize.Standard)
                        .then(
                            if (isDevices) Modifier.neonGlow(NeonPrimary, borderRadius = 6.dp, glowRadius = 4.dp, opacity = 0.3f)
                            else Modifier
                        )
                )
                Text(
                    text = "Devices",
                    color = if (isDevices) TextStrong else TextMuted,
                    style = CustomTypography.labelSmall.copy(
                        fontWeight = if (isDevices) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            // Empty spacer in the middle of the Row to accommodate the overlapping Center Button
            Spacer(modifier = Modifier.weight(1f))

            // 2. History Tab
            val isHistory = currentScreen == Screen.HISTORY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.HISTORY, context) }
                    .padding(vertical = Spacing.Small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = if (isHistory) NeonHighlight else TextMuted,
                    modifier = Modifier
                        .size(IconSize.Standard)
                        .then(
                            if (isHistory) Modifier.neonGlow(NeonPrimary, borderRadius = 6.dp, glowRadius = 4.dp, opacity = 0.3f)
                            else Modifier
                        )
                )
                Text(
                    text = "History",
                    color = if (isHistory) TextStrong else TextMuted,
                    style = CustomTypography.labelSmall.copy(
                        fontWeight = if (isHistory) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // 3. Central Brand Button (Anchor) - Rendered on top of the navbar row
        val isHome = currentScreen == Screen.HOME
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .scale(pulseScale)
                .size(68.dp)
                .clip(CircleShape)
                .neonGlow(
                    color = NeonPrimary,
                    borderRadius = 100.dp,
                    glowRadius = 16.dp,
                    opacity = glowIntensity
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonHighlight, NeonPrimary)
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (isHome) TextStrong else NeonHighlight.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .clickable {
                    viewModel.navigateTo(Screen.HOME, context)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Transfer Logo Center",
                tint = TextStrong,
                modifier = Modifier.size(IconSize.Large)
            )
        }
    }
}
