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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.ConfirmationPopup
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
    val context = LocalContext.current
    val showPopup by viewModel.showReceivePopup.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
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
            ReceiveStatusBand()
            ReceiverHeroCard()
            ConnectionPinCard()
            TrustMetadataGrid()
            WaitingActivityCard()
        }
    }

    ConfirmationPopup(
        visible = showPopup,
        deviceName = "STUDIO-WORKSTATION",
        filesCount = 2,
        totalSize = "16.6 MB",
        fileNames = listOf("Neon_Vibes_Chill.mp3", "IORI_Studios_Logo.png"),
        onAccept = { viewModel.acceptIncomingTransfer(context) },
        onDecline = { viewModel.declineIncomingTransfer(context) }
    )
}

@Composable
private fun ReceiveStatusBand() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgElevated.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(22.dp))
            .neonGlow(AccentCyan, borderRadius = 22.dp, glowRadius = 16.dp, opacity = 0.13f)
            .background(
                Brush.linearGradient(
                    listOf(
                        BgPanel,
                        BgPanelMuted.copy(alpha = 0.98f),
                        AccentCyan.copy(alpha = 0.12f),
                        NeonPrimary.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        AccentCyan.copy(alpha = 0.48f),
                        NeonPrimary.copy(alpha = 0.32f),
                        BorderSubtle
                    )
                ),
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
    val scale by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReceiverPulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReceiverPulseAlpha"
    )

    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = AccentCyan.copy(alpha = alpha),
                radius = 30.dp.toPx() * scale,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = NeonPrimary.copy(alpha = alpha * 0.72f),
                radius = 22.dp.toPx() * scale,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.14f))
                .border(1.dp, AccentCyan.copy(alpha = 0.52f), CircleShape),
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
private fun ConnectionPinCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .background(BgElevated)
            .border(1.dp, BorderSubtle.copy(alpha = 0.86f), RoundedCornerShape(Radii.CardStandard))
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

        Text(
            text = "1 2 3 4",
            style = CustomTypography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = 3.sp
            ),
            color = NeonHighlight
        )
    }
}

@Composable
private fun TrustMetadataGrid() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        MetadataTile(
            label = "Mode",
            value = "Private",
            icon = Icons.Default.Security,
            color = NeonHighlight,
            modifier = Modifier.weight(1f)
        )
        MetadataTile(
            label = "Channel",
            value = "Local",
            icon = Icons.Default.Speed,
            color = AccentCyan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetadataTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgPanelMuted.copy(alpha = 0.86f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(IconSize.Small)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.Small))
        Column {
            Text(
                text = label,
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextMuted
            )
            Text(
                text = value,
                style = CustomTypography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                color = TextStrong,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WaitingActivityCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .background(BgElevated.copy(alpha = 0.82f))
            .border(1.dp, BorderSubtle.copy(alpha = 0.76f), RoundedCornerShape(Radii.CardStandard))
            .padding(Spacing.Medium)
    ) {
        Text(
            text = "Intake queue",
            style = CustomTypography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.sp
            ),
            color = TextStrong
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        ActivityRow("Discovery broadcast", "Running", AccentCyan)
        ActivityRow("Incoming approvals", "Manual", NeonHighlight)
        ActivityRow("History logging", "Silent", TextSecondary)
    }
}

@Composable
private fun ActivityRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = label,
            style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = CustomTypography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            ),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusDot() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(AccentCyan)
            .neonGlow(AccentCyan, borderRadius = 6.dp, glowRadius = 8.dp, opacity = 0.32f)
    )
}
