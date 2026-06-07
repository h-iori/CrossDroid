package com.ioristudios.crossdroid.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel

import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
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
    val currentPin by viewModel.currentPin.collectAsState()
    
    var activeTrigger by remember { mutableStateOf(false) }
    var heroTrigger by remember { mutableStateOf(false) }
    var pinTrigger by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        activeTrigger = true
        kotlinx.coroutines.delay(80L)
        heroTrigger = true
        kotlinx.coroutines.delay(80L)
        pinTrigger = true
    }

    val activeAlpha by animateFloatAsState(
        targetValue = if (activeTrigger) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "activeAlpha"
    )
    val activeOffsetY by animateFloatAsState(
        targetValue = if (activeTrigger) 0f else 25f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "activeOffsetY"
    )

    val heroAlpha by animateFloatAsState(
        targetValue = if (heroTrigger) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroAlpha"
    )
    val heroOffsetY by animateFloatAsState(
        targetValue = if (heroTrigger) 0f else 25f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "heroOffsetY"
    )

    val pinAlpha by animateFloatAsState(
        targetValue = if (pinTrigger) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pinAlpha"
    )
    val pinOffsetY by animateFloatAsState(
        targetValue = if (pinTrigger) 0f else 25f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "pinOffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ReceiveTechGridDrift")
    val gridOffsetDp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GridOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        // Decorative background grid & radial glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Center-top cyan ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.35f),
                    radius = width * 0.8f
                )
            )
            
            // Center-bottom purple ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPrimary.copy(alpha = 0.05f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.65f),
                    radius = width * 0.8f
                )
            )

            // Dotted/dashed tech grid line drawing
            val gridSize = 40.dp.toPx()
            val offsetX = gridOffsetDp.dp.toPx()
            val offsetY = gridOffsetDp.dp.toPx()
            val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)
            
            var x = offsetX % gridSize
            while (x < width) {
                drawLine(
                    color = BorderSubtle.copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = gridPathEffect
                )
                x += gridSize
            }
            
            var y = offsetY % gridSize
            while (y < height) {
                drawLine(
                    color = BorderSubtle.copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = gridPathEffect
                )
                y += gridSize
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = "Receive Console",
                subtitle = "Secure local intake",
                viewModel = viewModel,
                showBackButton = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = activeAlpha
                        translationY = activeOffsetY.dp.toPx()
                    }
                ) {
                    ReceiveStatusBand()
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = heroAlpha
                        translationY = heroOffsetY.dp.toPx()
                    }
                ) {
                    ReceiverHeroCard()
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = pinAlpha
                        translationY = pinOffsetY.dp.toPx()
                    }
                ) {
                    ConnectionPinCard(currentPin)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReceiveStatusBand() {
    val statusTransition = rememberInfiniteTransition(label = "StatusBandBreathe")
    val breatheAlpha by statusTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgElevated.copy(alpha = 0.65f))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        AccentCyan.copy(alpha = breatheAlpha),
                        BorderSubtle.copy(alpha = (breatheAlpha * 1.5f).coerceAtMost(1f))
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot()
        Spacer(modifier = Modifier.width(Spacing.Small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ACTIVE LISTENER",
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AccentCyan
            )
            Text(
                text = "Visible to nearby trusted senders",
                style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(IconSize.Standard)
        )
    }
}

@Composable
private fun ReceiverHeroCard() {
    val transition = rememberInfiniteTransition(label = "HeroCardBorder")
    val gradientShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BorderGradientShift"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            AccentCyan.copy(alpha = 0.6f),
            NeonPrimary.copy(alpha = 0.35f),
            BorderSubtle.copy(alpha = 0.5f),
            AccentCyan.copy(alpha = 0.6f)
        ),
        start = androidx.compose.ui.geometry.Offset(gradientShift, 0f),
        end = androidx.compose.ui.geometry.Offset(gradientShift + 400f, 400f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(22.dp))
            .neonGlow(AccentCyan, borderRadius = 22.dp, glowRadius = 16.dp, opacity = 0.13f)
            .background(
                Brush.linearGradient(
                    listOf(
                        BgPanel.copy(alpha = 0.85f),
                        BgPanelMuted.copy(alpha = 0.95f),
                        AccentCyan.copy(alpha = 0.08f),
                        NeonPrimary.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(Spacing.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BroadcastNode()

        Spacer(modifier = Modifier.width(Spacing.Large))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ready to receive",
                style = CustomTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "CrossDroid is advertising this device for encrypted peer discovery.",
                style = CustomTypography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp
                ),
                color = TextBody.copy(alpha = 0.86f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BroadcastNode() {
    val transition = rememberInfiniteTransition(label = "ReceiverPulse")
    
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    val floatOffset by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatingNode"
    )

    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            
            // Draw static dashed grid circles
            val dotPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
            drawCircle(
                color = BorderSubtle.copy(alpha = 0.35f),
                radius = 22.dp.toPx(),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dotPathEffect)
            )
            drawCircle(
                color = BorderSubtle.copy(alpha = 0.25f),
                radius = 34.dp.toPx(),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dotPathEffect)
            )
            drawCircle(
                color = BorderSubtle.copy(alpha = 0.15f),
                radius = 46.dp.toPx(),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dotPathEffect)
            )

            // Draw rotating radar sweep gradient
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccentCyan.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = sweepAngle - 60f,
                sweepAngle = 60f,
                useCenter = true
            )

            // Draw leading radar sweep line
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val lineLength = 46.dp.toPx()
            val endX = center.x + lineLength * Math.cos(sweepRad).toFloat()
            val endY = center.y + lineLength * Math.sin(sweepRad).toFloat()
            drawLine(
                color = AccentCyan.copy(alpha = 0.45f),
                start = center,
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 1.5.dp.toPx()
            )

            // 3 Concentric Ripple Waves
            val waves = listOf(
                progress,
                (progress + 0.33f) % 1.0f,
                (progress + 0.66f) % 1.0f
            )

            waves.forEach { waveProgress ->
                val scale = 0.6f + waveProgress * 1.0f
                val alpha = (1.0f - waveProgress) * 0.45f
                
                drawCircle(
                    color = AccentCyan.copy(alpha = alpha),
                    radius = 28.dp.toPx() * scale,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = NeonPrimary.copy(alpha = alpha * 0.5f),
                    radius = 20.dp.toPx() * scale,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer { translationY = floatOffset.dp.toPx() }
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = 0.22f),
                            AccentCyan.copy(alpha = 0.05f)
                        )
                    )
                )
                .neonGlow(AccentCyan, borderRadius = 29.dp, glowRadius = 12.dp, opacity = 0.25f)
                .border(1.dp, AccentCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(IconSize.Large)
            )
        }
    }
}

@Composable
private fun ConnectionPinCard(pinCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .background(BgElevated.copy(alpha = 0.65f))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        BorderSubtle.copy(alpha = 0.72f),
                        AccentCyan.copy(alpha = 0.22f)
                    )
                ),
                RoundedCornerShape(Radii.CardStandard)
            )
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CONNECTION PIN",
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(Spacing.Tiny))
            Text(
                text = "Use this code when a sender requests manual pairing.",
                style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        val pinDigits = pinCode.takeIf { it.length == 6 }?.map { it.toString() } ?: listOf("-", "-", "-", "-", "-", "-")
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pinDigits.forEach { digit ->
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgPanelMuted.copy(alpha = 0.85f))
                        .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .neonGlow(AccentCyan, borderRadius = 8.dp, glowRadius = 4.dp, opacity = 0.12f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = CustomTypography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = NeonHighlight
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusDot() {
    val transition = rememberInfiniteTransition(label = "StatusDotPulse")
    val scale by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusDotScale"
    )
    val glowOpacity by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusDotGlowOpacity"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(10.dp)
            .clip(CircleShape)
            .background(AccentCyan)
            .neonGlow(AccentCyan, borderRadius = 5.dp, glowRadius = 8.dp, opacity = glowOpacity)
    )
}
