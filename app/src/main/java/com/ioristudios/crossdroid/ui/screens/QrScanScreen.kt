package com.ioristudios.crossdroid.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.GlowingButton
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextBody
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun QrScanScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Infinite animation driving the vertical position of the scan line laser
    val infiniteTransition = rememberInfiniteTransition(label = "LaserLineTransition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Scan QR Code",
            viewModel = viewModel,
            showBackButton = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Align receiver QR code in frame",
                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextStrong,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Or tap the viewfinder to simulate a detection",
                style = CustomTypography.labelMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // Simulated Viewfinder Frame
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f),
                contentAlignment = Alignment.Center
            ) {
                val sizeDp = minOf(maxWidth * 0.75f, maxHeight * 0.9f)
                Box(
                    modifier = Modifier
                        .size(sizeDp)
                        .clip(RoundedCornerShape(Radii.CardStandard))
                        .neonGlow(NeonPrimary, borderRadius = Radii.CardStandard, glowRadius = 16.dp, opacity = 0.2f)
                        .background(Color(0xFF07070B))
                        .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(Radii.CardStandard))
                        .clickable {
                            // Simulate successful QR detection
                            val defaultDevice = MockData.deviceNodes.first { it.osType == "windows" }
                            val filesToSend = viewModel.selectedFiles.value.toList()
                            HapticHelper.triggerSuccess(context)
                            viewModel.startTransferFlow(defaultDevice, filesToSend, isIncoming = false, context = context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer corners glow brackets drawn on canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 3.dp.toPx()
                        val len = 24.dp.toPx()
                        
                        // Top Left Bracket
                        drawLine(NeonPrimary, Offset(0f, 0f), Offset(len, 0f), stroke)
                        drawLine(NeonPrimary, Offset(0f, 0f), Offset(0f, len), stroke)
                        
                        // Top Right Bracket
                        drawLine(NeonPrimary, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke)
                        drawLine(NeonPrimary, Offset(size.width, 0f), Offset(size.width, len), stroke)
                        
                        // Bottom Left Bracket
                        drawLine(NeonPrimary, Offset(0f, size.height), Offset(len, size.height), stroke)
                        drawLine(NeonPrimary, Offset(0f, size.height), Offset(0f, size.height - len), stroke)
                        
                        // Bottom Right Bracket
                        drawLine(NeonPrimary, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke)
                        drawLine(NeonPrimary, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke)
                        
                        // Laser sliding scan line
                        val laserY = size.height * laserPosition
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, NeonHighlight, NeonHighlight, Color.Transparent)
                            ),
                            start = Offset(0f, laserY),
                            end = Offset(size.width, laserY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Cyberpunk icon inside
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        tint = NeonHighlight.copy(alpha = 0.25f),
                        modifier = Modifier.size((sizeDp * 0.35f).coerceAtLeast(32.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            Text(
                text = "Scanning for receiver...",
                style = CustomTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        // Alternative input selections (Enter Code or Radar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgElevated)
                .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowingButton(
                text = "ENTER CODE",
                onClick = { viewModel.navigateTo(Screen.ENTER_CODE, context) },
                modifier = Modifier.weight(1f),
                glowColor = NeonHighlight,
                hapticIntensity = "medium"
            )

            GlowingButton(
                text = "RADAR DISCOVERY",
                onClick = { viewModel.navigateTo(Screen.RADAR, context) },
                modifier = Modifier.weight(1f),
                glowColor = Color(0xFF00E5FF),
                hapticIntensity = "medium"
            )
        }
    }
}
