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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.ConfirmationPopup
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.CustomTypography
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
fun ReceiveScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showPopup by viewModel.showReceivePopup.collectAsState()

    // Ripple wave expansion animation for the discoverability center
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRippleTransition")
    
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha1"
    )

    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2"
    )
    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = "Waiting to Receive",
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
                // Ripple Canvas Viewport
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = size.width / 2
                        val baseRadius = 50.dp.toPx()
                        
                        // Ripple 1
                        drawCircle(
                            color = NeonPrimary.copy(alpha = rippleAlpha1),
                            radius = baseRadius * rippleScale1,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Ripple 2
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = rippleAlpha2),
                            radius = baseRadius * rippleScale2,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Discoverability Center Base Node
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .neonGlow(NeonPrimary, borderRadius = 50.dp, glowRadius = 18.dp, opacity = 0.4f)
                            .background(BgElevated)
                            .border(width = 1.5.dp, color = NeonHighlight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = "Broadcasting Node",
                            tint = NeonHighlight,
                            modifier = Modifier.size(IconSize.Large)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Large))

                Text(
                    text = "You are discoverable",
                    style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextStrong,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Ready to pair with nearby senders.",
                    style = CustomTypography.labelMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.Huge))

                // Pairing details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radii.CardStandard))
                        .background(BgElevated)
                        .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(Radii.CardStandard))
                        .padding(Spacing.Medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR CONNECTION PIN",
                        style = CustomTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = "1 2 3 4",
                        style = CustomTypography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = 4.sp,
                            color = NeonHighlight
                        )
                    )
                }
            }
        }

        // Overlay Confirmation Dialog
        ConfirmationPopup(
            visible = showPopup,
            deviceName = "STUDIO-WORKSTATION",
            filesCount = 2,
            totalSize = "16.6 MB",
            onAccept = { viewModel.acceptIncomingTransfer(context) },
            onDecline = { viewModel.declineIncomingTransfer(context) }
        )
    }
}
