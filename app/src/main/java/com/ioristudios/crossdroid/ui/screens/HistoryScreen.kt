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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.HistorySession
import com.ioristudios.crossdroid.ui.buildHistorySessions
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
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
fun HistoryScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyItems by viewModel.historyRecords.collectAsState()
    val sessions = buildHistorySessions(historyItems)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Transfer history",
            subtitle = "${sessions.size} sessions recorded",
            viewModel = viewModel,
            onMenuClick = { viewModel.setSidebarVisible(true) }
        )

        if (sessions.isEmpty()) {
            HistoryEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.Medium,
                    top = Spacing.Medium,
                    end = Spacing.Medium,
                    bottom = Spacing.Huge
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                item {
                    HistoryOverview(
                        sessions = sessions.size,
                        files = historyItems.size,
                        failed = sessions.count { !it.isSuccess }
                    )
                }

                item {
                    Text(
                        text = "Recent sessions",
                        style = CustomTypography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        ),
                        color = TextStrong,
                        modifier = Modifier.padding(top = Spacing.Small)
                    )
                }

                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    HistorySessionRow(
                        session = session,
                        onClick = { viewModel.openHistorySession(session, context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryOverview(
    sessions: Int,
    files: Int,
    failed: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .neonGlow(
                color = NeonPrimary,
                borderRadius = Radii.CardStandard,
                glowRadius = 12.dp,
                opacity = 0.11f
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        BgPanel,
                        BgPanelMuted.copy(alpha = 0.98f),
                        AccentCyan.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonPrimary.copy(alpha = 0.32f),
                        AccentCyan.copy(alpha = 0.24f),
                        BorderSubtle
                    )
                ),
                shape = RoundedCornerShape(Radii.CardStandard)
            )
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$sessions sessions / $files files",
                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextStrong
            )
            Text(
                text = if (failed == 0) "All recorded transfers are complete" else "$failed sessions need attention",
                style = CustomTypography.labelMedium,
                color = if (failed == 0) TextSecondary else ColorError
            )
        }
    }
}

@Composable
private fun HistorySessionRow(
    session: HistorySession,
    onClick: () -> Unit
) {
    val accent = if (session.isIncoming) AccentCyan else NeonHighlight
    val statusColor = if (session.isSuccess) ColorSuccess else ColorError
    val direction = if (session.isIncoming) "Received from" else "Sent to"
    val fileSummary = session.records.take(2).joinToString(", ") { it.fileName }
        .let { summary ->
            if (session.records.size > 2) "$summary +${session.records.size - 2} more" else summary
        }
    val sizeSummary = session.records.joinToString(", ") { it.size }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (session.isIncoming) Icons.Default.FileDownload else Icons.Default.FileUpload,
                contentDescription = if (session.isIncoming) "Received session" else "Sent session",
                tint = accent,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$direction ${session.deviceName}",
                style = CustomTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${session.records.size} ${if (session.records.size == 1) "file" else "files"} - $fileSummary",
                style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${session.date} - $sizeSummary",
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(
                label = if (session.isSuccess) "Complete" else "Failed",
                color = statusColor
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open transfer details",
                tint = TextMuted,
                modifier = Modifier.size(IconSize.Small)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.24f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.sp
            )
        )
    }
}

@Composable
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(Spacing.Large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NeonPrimary.copy(alpha = 0.12f))
                .border(1.dp, NeonPrimary.copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(IconSize.Standard)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = "No transfer history",
            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextStrong
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Completed sends and receives will appear here as searchable transfer sessions.",
            style = CustomTypography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = Spacing.Large),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
