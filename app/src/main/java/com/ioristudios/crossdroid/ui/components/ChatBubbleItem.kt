package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import com.ioristudios.crossdroid.data.FileKind
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.TransferBubble
import com.ioristudios.crossdroid.ui.TransferStatus
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun ChatBubbleItem(
    bubble: TransferBubble,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onPause: ((String) -> Unit)? = null,
    onResume: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null
) {
    val isOutgoing = !bubble.isIncoming
    val accent = if (isOutgoing) NeonHighlight else AccentCyan
    val isActive = bubble.status in listOf(
        TransferStatus.Pending,
        TransferStatus.Sending,
        TransferStatus.Receiving,
        TransferStatus.Paused
    )
    val statusColor = when (bubble.status) {
        TransferStatus.Completed -> ColorSuccess
        TransferStatus.Failed,
        TransferStatus.Canceled -> ColorError
        TransferStatus.Paused -> NeonHighlight
        else -> accent
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 316.dp)
                .clip(bubbleShape)
                .then(
                    if (isActive && !readOnly) {
                        Modifier.neonGlow(
                            color = accent,
                            borderRadius = Radii.CardStandard,
                            glowRadius = 7.dp,
                            opacity = 0.12f
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = if (isOutgoing) {
                        Brush.linearGradient(
                            listOf(
                                NeonPrimary.copy(alpha = 0.22f),
                                BgSurface.copy(alpha = 0.96f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                BgElevated.copy(alpha = 0.98f),
                                BgPanelMuted.copy(alpha = 0.78f)
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) accent.copy(alpha = 0.34f) else BorderSubtle.copy(alpha = 0.9f),
                    shape = bubbleShape
                )
                .padding(Spacing.Medium)
        ) {
            FileBubbleHeader(
                bubble = bubble,
                accent = accent,
                statusColor = statusColor,
                readOnly = readOnly,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel
            )

            if (!readOnly || isActive) {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                CyberpunkProgressBar(
                    progress = bubble.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            FileBubbleFooter(
                bubble = bubble,
                statusColor = statusColor,
                readOnly = readOnly
            )
        }
    }
}

@Composable
private fun FileBubbleHeader(
    bubble: TransferBubble,
    accent: Color,
    statusColor: Color,
    readOnly: Boolean,
    onPause: ((String) -> Unit)?,
    onResume: ((String) -> Unit)?,
    onCancel: ((String) -> Unit)?
) {
    val fileIcon = when (bubble.file.type) {
        FileType.ALL -> Icons.Default.Description
        FileType.VIDEO -> Icons.Default.PlayCircle
        FileType.IMAGE -> Icons.Default.Image
        FileType.MUSIC -> Icons.Default.Audiotrack
        FileType.DOCUMENT -> Icons.Default.Description
    }
    val resolvedFileIcon = if (bubble.file.kind == FileKind.FOLDER) Icons.Default.Folder else fileIcon

    val statusIcon = when (bubble.status) {
        TransferStatus.Completed -> Icons.Default.CheckCircle
        TransferStatus.Failed,
        TransferStatus.Canceled -> Icons.Default.Error
        TransferStatus.Paused -> Icons.Default.PauseCircle
        else -> null
    }
    val canControl = !readOnly && bubble.status in listOf(
        TransferStatus.Pending,
        TransferStatus.Sending,
        TransferStatus.Receiving,
        TransferStatus.Paused
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = resolvedFileIcon,
                contentDescription = "File type",
                tint = accent,
                modifier = Modifier.size(IconSize.Small)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bubble.file.name,
                style = CustomTypography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bubble.file.size,
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (canControl) {
            Spacer(modifier = Modifier.width(Spacing.Small))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.11f))
                        .border(1.dp, accent.copy(alpha = 0.24f), CircleShape)
                        .clickable {
                            if (bubble.status == TransferStatus.Paused) {
                                onResume?.invoke(bubble.id)
                            } else {
                                onPause?.invoke(bubble.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (bubble.status == TransferStatus.Paused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                        contentDescription = if (bubble.status == TransferStatus.Paused) "Resume item" else "Pause item",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ColorError.copy(alpha = 0.11f))
                        .border(1.dp, ColorError.copy(alpha = 0.24f), CircleShape)
                        .clickable { onCancel?.invoke(bubble.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel item",
                        tint = ColorError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else if (statusIcon != null) {
            Spacer(modifier = Modifier.width(Spacing.Small))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = bubble.status.name,
                    tint = statusColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun FileBubbleFooter(
    bubble: TransferBubble,
    statusColor: Color,
    readOnly: Boolean
) {
    val statusText = when (bubble.status) {
        TransferStatus.Sending -> "Sending ${(bubble.progress * 100).toInt()}%"
        TransferStatus.Receiving -> "Receiving ${(bubble.progress * 100).toInt()}%"
        TransferStatus.Completed -> if (readOnly) "Recorded complete" else "Completed"
        TransferStatus.Paused -> "Paused"
        TransferStatus.Canceled -> if (readOnly) "Recorded canceled" else "Canceled"
        TransferStatus.Failed -> "Recorded failed"
        else -> "Pending"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusText,
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.sp
            ),
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        AnimatedVisibility(
            visible = !readOnly && bubble.status in listOf(TransferStatus.Sending, TransferStatus.Receiving),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = bubble.speed,
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.sp
                ),
                color = TextMuted
            )
        }
    }
}
