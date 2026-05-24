package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.TransferBubble
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
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
fun ChatBubbleItem(
    bubble: TransferBubble,
    modifier: Modifier = Modifier
) {
    val isOutgoing = !bubble.isIncoming
    
    // Bubble shapes based on alignment
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = Radii.CardStandard,
            topEnd = Radii.CardStandard,
            bottomStart = Radii.CardStandard,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = Radii.CardStandard,
            topEnd = Radii.CardStandard,
            bottomStart = 4.dp,
            bottomEnd = Radii.CardStandard
        )
    }

    val fileIcon = when (bubble.file.type) {
        FileType.VIDEO -> Icons.Default.PlayCircle
        FileType.IMAGE -> Icons.Default.Image
        FileType.MUSIC -> Icons.Default.Audiotrack
        else -> Icons.Default.Description
    }

    val iconColor = when (bubble.file.type) {
        FileType.VIDEO -> NeonHighlight
        FileType.IMAGE -> Color(0xFF00E5FF)
        FileType.MUSIC -> ColorSuccess
        else -> Color(0xFFFFD600)
    }

    val statusIcon = when (bubble.status) {
        "Completed" -> Icons.Default.CheckCircle
        "Failed" -> Icons.Default.Error
        "Paused" -> Icons.Default.PauseCircle
        else -> null
    }

    val statusIconColor = when (bubble.status) {
        "Completed" -> ColorSuccess
        "Failed" -> ColorError
        "Paused" -> NeonHighlight
        else -> TextSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .then(
                    if (bubble.status == "Sending" || bubble.status == "Receiving") {
                        Modifier.neonGlow(
                            color = NeonPrimary,
                            borderRadius = Radii.CardStandard,
                            glowRadius = 6.dp,
                            opacity = 0.15f
                        )
                    } else Modifier
                )
                .background(if (isOutgoing) BgSurface else BgElevated)
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) NeonPrimary.copy(alpha = 0.4f) else BgSurface,
                    shape = bubbleShape
                )
                .padding(Spacing.Medium)
        ) {
            // Header: Name and icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = "File Type",
                        tint = iconColor,
                        modifier = Modifier.size(IconSize.Small)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.Small))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bubble.file.name,
                        style = CustomTypography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = TextStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bubble.file.size,
                        style = CustomTypography.labelSmall,
                        color = TextSecondary
                    )
                }

                if (statusIcon != null) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = bubble.status,
                        tint = statusIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Progress Bar
            CyberpunkProgressBar(
                progress = bubble.progress,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.Small))

            // Footer status + speed info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (bubble.status) {
                        "Sending" -> "Sending (${(bubble.progress * 100).toInt()}%)"
                        "Receiving" -> "Receiving (${(bubble.progress * 100).toInt()}%)"
                        "Completed" -> "Completed"
                        "Paused" -> "Paused"
                        "Failed" -> "Failed"
                        else -> "Pending"
                    },
                    style = CustomTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (bubble.status == "Completed") ColorSuccess else if (bubble.status == "Failed") ColorError else TextSecondary
                    )
                )

                AnimatedVisibility(
                    visible = bubble.status in listOf("Sending", "Receiving"),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = bubble.speed,
                        style = CustomTypography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = NeonHighlight
                        ),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
