package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileItem
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.data.HistoryItem
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.TransferBubble
import com.ioristudios.crossdroid.ui.TransferStatus
import com.ioristudios.crossdroid.ui.components.ChatBubbleItem
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
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
fun HistoryDetailScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.selectedHistorySession.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Transfer transcript",
            subtitle = session?.deviceName ?: "No session selected",
            viewModel = viewModel,
            showBackButton = true
        )

        if (session == null) {
            MissingSessionState()
        } else {
            val activeSession = session!!
            val bubbles = activeSession.records.map { it.toTransferBubble() }
            
            var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                visible = true
            }

            val cardAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                label = "detailCardAlpha"
            )
            val cardOffsetY by animateFloatAsState(
                targetValue = if (visible) 0f else -30f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 250f),
                label = "detailCardOffsetY"
            )

            val barAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                label = "detailBarAlpha"
            )
            val barOffsetY by animateFloatAsState(
                targetValue = if (visible) 0f else 30f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 250f),
                label = "detailBarOffsetY"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.Medium,
                    top = Spacing.Medium,
                    end = Spacing.Medium,
                    bottom = Spacing.Large
                ),
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = cardAlpha
                            translationY = cardOffsetY.dp.toPx()
                        }
                    ) {
                        SessionSummaryCard(
                            deviceName = activeSession.deviceName,
                            date = activeSession.date,
                            isIncoming = activeSession.isIncoming,
                            isSuccess = activeSession.isSuccess,
                            fileCount = activeSession.records.size
                        )
                    }
                }

                item {
                    TimelineDivider(
                        text = if (activeSession.isSuccess) "Recorded transfer complete" else "Recorded transfer failed"
                    )
                }

                items(
                    items = bubbles,
                    key = { it.id }
                ) { bubble ->
                    ChatBubbleItem(
                        bubble = bubble,
                        readOnly = true
                    )
                }
            }

            // Bottom elegant send bar
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = barAlpha
                        translationY = barOffsetY.dp.toPx()
                    }
                    .fillMaxWidth()
                    .background(BgElevated)
                    .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(Spacing.Medium)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(BgSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                        .clickable { viewModel.navigateTo(Screen.SEND, context) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach Files",
                        tint = NeonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Send files to this device...",
                        color = TextSecondary,
                        style = CustomTypography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonPrimary)
                            .neonGlow(NeonPrimary, borderRadius = 20.dp, glowRadius = 8.dp, opacity = 0.4f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    deviceName: String,
    date: String,
    isIncoming: Boolean,
    isSuccess: Boolean,
    fileCount: Int
) {
    val accent = if (isIncoming) AccentCyan else NeonHighlight
    val statusColor = if (isSuccess) ColorSuccess else ColorError
    val direction = if (isIncoming) "Incoming transfer" else "Outgoing transfer"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .neonGlow(
                color = accent,
                borderRadius = Radii.CardStandard,
                glowRadius = 12.dp,
                opacity = 0.12f
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        BgPanel,
                        BgPanelMuted.copy(alpha = 0.98f),
                        accent.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.36f),
                        NeonPrimary.copy(alpha = 0.22f),
                        BorderSubtle
                    )
                ),
                shape = RoundedCornerShape(Radii.CardStandard)
            )
            .padding(Spacing.Medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncoming) Icons.Default.FileDownload else Icons.Default.FileUpload,
                    contentDescription = direction,
                    tint = accent,
                    modifier = Modifier.size(IconSize.Standard)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = direction,
                    style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextStrong
                )
                Text(
                    text = deviceName,
                    style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                    color = TextSecondary
                )
            }

            StatusPill(
                label = if (isSuccess) "Complete" else "Failed",
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            SummaryMetric(
                label = "Files",
                value = "$fileCount",
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "Date",
                value = date,
                modifier = Modifier.weight(1.8f)
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgElevated.copy(alpha = 0.70f))
            .border(1.dp, BorderSubtle.copy(alpha = 0.74f), RoundedCornerShape(12.dp))
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
    ) {
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

@Composable
private fun TimelineDivider(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderSubtle)
        )
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.Small)
                .clip(CircleShape)
                .background(BgElevated.copy(alpha = 0.92f))
                .border(1.dp, BorderSubtle, CircleShape)
                .padding(horizontal = Spacing.Small, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = CustomTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.sp
                ),
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderSubtle)
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.24f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.sp
            ),
            color = color
        )
    }
}

@Composable
private fun MissingSessionState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(Spacing.Large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = ColorError,
            modifier = Modifier.size(IconSize.Large)
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = "No session selected",
            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextStrong
        )
        Text(
            text = "Return to history and open a transfer session.",
            style = CustomTypography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun HistoryItem.toTransferBubble(): TransferBubble {
    return TransferBubble(
        id = id,
        file = FileItem(
            name = fileName,
            size = size,
            type = inferFileType(fileName)
        ),
        progress = if (isSuccess) 1f else 0f,
        speed = "",
        status = if (isSuccess) TransferStatus.Completed else TransferStatus.Failed,
        isIncoming = isIncoming
    )
}

private fun inferFileType(fileName: String): FileType {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "mp4", "mov", "mkv", "avi" -> FileType.VIDEO
        "png", "jpg", "jpeg", "webp", "gif" -> FileType.IMAGE
        "mp3", "wav", "aac", "flac" -> FileType.MUSIC
        else -> FileType.DOCUMENT
    }
}
