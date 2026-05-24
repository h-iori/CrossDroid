package com.ioristudios.crossdroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.TransferStatus
import com.ioristudios.crossdroid.ui.components.ChatBubbleItem
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
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
fun TransferScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val device by viewModel.transferDevice.collectAsState()
    val bubbles by viewModel.transferBubbles.collectAsState()
    val isComplete by viewModel.isTransferComplete.collectAsState()
    val isActive by viewModel.isTransferActive.collectAsState()

    val totalFiles = bubbles.size
    val completedFiles = bubbles.count { it.status == TransferStatus.Completed }
    val canceledFiles = bubbles.count { it.status == TransferStatus.Canceled || it.status == TransferStatus.Failed }
    val totalSpeed = viewModel.totalTransferSpeedLabel()
    val statusLine = when {
        isActive -> "Transferring $completedFiles of $totalFiles files"
        isComplete && canceledFiles > 0 -> "$completedFiles complete / $canceledFiles canceled"
        isComplete -> "All $completedFiles files complete"
        else -> "$completedFiles of $totalFiles files processed"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Active Transfer Stream",
            viewModel = viewModel,
            showBackButton = false,
            showMenuButton = false
        )

        // Peer Terminal info header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgElevated)
                .border(width = 1.dp, color = BgSurface)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Active Link",
                    tint = NeonHighlight,
                    modifier = Modifier.size(IconSize.Small)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.Small))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device?.name ?: "Unknown Terminal",
                    style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = TextStrong
                )
                Text(
                    text = statusLine,
                    style = CustomTypography.labelSmall,
                    color = TextSecondary
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.width(Spacing.Small))
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPrimary.copy(alpha = 0.11f))
                        .border(1.dp, NeonHighlight.copy(alpha = 0.26f), RoundedCornerShape(12.dp))
                        .padding(horizontal = Spacing.Small, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = totalSpeed,
                        style = CustomTypography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.sp
                        ),
                        color = NeonHighlight,
                        maxLines = 1
                    )
                    Text(
                        text = "TOTAL SPEED",
                        style = CustomTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 0.6.sp
                        ),
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }

        // Active Chat progression list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(Spacing.Medium),
            verticalArrangement = Arrangement.Top
        ) {
            items(bubbles) { bubble ->
                ChatBubbleItem(
                    bubble = bubble,
                    onPause = { viewModel.pauseTransferItem(it, context) },
                    onResume = { viewModel.resumeTransferItem(it, context) },
                    onCancel = { viewModel.cancelTransferItem(it, context) }
                )
            }
        }

        // Bottom elegant send bar
        Box(
            modifier = Modifier
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
