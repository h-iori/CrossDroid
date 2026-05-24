package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextBody
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong

@Composable
fun HistoryScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val historyItems by viewModel.historyRecords.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Transfer Log History",
            viewModel = viewModel,
            onMenuClick = { viewModel.setSidebarVisible(true) }
        )

        if (historyItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(Spacing.Large),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No transfers yet",
                    style = CustomTypography.titleMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                items(historyItems) { record ->
                    val isSuccess = record.isSuccess
                    val isIncoming = record.isIncoming
                    val statusColor = if (isSuccess) ColorSuccess else ColorError
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radii.CardStandard))
                            .background(BgElevated)
                            .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(Radii.CardStandard))
                            .padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Direction Icon Bubble
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isIncoming) Color(0xFF00E5FF).copy(alpha = 0.1f) else NeonHighlight.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isIncoming) Icons.Default.FileDownload else Icons.Default.FileUpload,
                                contentDescription = if (isIncoming) "Received" else "Sent",
                                tint = if (isIncoming) Color(0xFF00E5FF) else NeonHighlight,
                                modifier = Modifier.size(IconSize.Standard)
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.Medium))

                        // Transfer metadata details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = record.fileName,
                                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = TextStrong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (isIncoming) "From" else "To"} ${record.deviceName} • ${record.size}",
                                style = CustomTypography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = record.date,
                                style = CustomTypography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.Small))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .border(width = 1.dp, color = statusColor.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = Spacing.Small, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSuccess) "Success" else "Failed",
                                color = statusColor,
                                style = CustomTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
