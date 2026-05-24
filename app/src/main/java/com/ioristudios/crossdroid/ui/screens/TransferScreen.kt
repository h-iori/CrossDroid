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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import com.ioristudios.crossdroid.ui.components.ChatBubbleItem
import com.ioristudios.crossdroid.ui.components.GlowingButton
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
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
    val isPaused by viewModel.isTransferPaused.collectAsState()
    val isComplete by viewModel.isTransferComplete.collectAsState()
    val isActive by viewModel.isTransferActive.collectAsState()

    val totalFiles = bubbles.size
    val completedFiles = bubbles.count { it.status == "Completed" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Active Transfer Stream",
            viewModel = viewModel,
            showBackButton = false
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
            
            Column {
                Text(
                    text = device?.name ?: "Unknown Terminal",
                    style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = TextStrong
                )
                Text(
                    text = "Transferring $completedFiles of $totalFiles files",
                    style = CustomTypography.labelSmall,
                    color = TextSecondary
                )
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
                ChatBubbleItem(bubble = bubble)
            }
        }

        // Complete Success panel overlay
        AnimatedVisibility(
            visible = isComplete,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
                    .neonGlow(ColorSuccess, borderRadius = Radii.OverlaySheet, glowRadius = 14.dp, opacity = 0.2f)
                    .background(BgElevated)
                    .border(width = 1.dp, color = ColorSuccess.copy(alpha = 0.3f), shape = RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
                    .padding(Spacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = ColorSuccess,
                    modifier = Modifier.size(IconSize.Huge)
                )
                
                Spacer(modifier = Modifier.height(Spacing.Small))
                
                Text(
                    text = "TRANSFER COMPLETE",
                    style = CustomTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextStrong
                )
                Text(
                    text = "All files successfully saved to peer terminal logs.",
                    style = CustomTypography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = Spacing.Small)
                )
                
                Spacer(modifier = Modifier.height(Spacing.Medium))
                
                GlowingButton(
                    text = "VIEW HISTORY LOG",
                    onClick = {
                        viewModel.navigateTo(Screen.HISTORY, context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = ColorSuccess,
                    hapticIntensity = "strong"
                )
            }
        }

        // Controller Actions (Pause/Resume/Cancel) - Visible only while active
        AnimatedVisibility(
            visible = isActive && !isComplete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgElevated)
                    .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Pause button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(Radii.ButtonSmall))
                        .background(if (isPaused) NeonPrimary.copy(alpha = 0.15f) else BgSurface)
                        .border(
                            width = 1.dp,
                            color = if (isPaused) NeonHighlight else BgSurface,
                            shape = RoundedCornerShape(Radii.ButtonSmall)
                        )
                        .clickable { viewModel.toggleTransferPause(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause Toggle",
                            tint = if (isPaused) NeonHighlight else TextStrong,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = if (isPaused) "RESUME" else "PAUSE",
                            color = if (isPaused) NeonHighlight else TextStrong,
                            style = CustomTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Cancel button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(Radii.ButtonSmall))
                        .background(ColorError.copy(alpha = 0.10f))
                        .border(width = 1.dp, color = ColorError.copy(alpha = 0.3f), shape = RoundedCornerShape(Radii.ButtonSmall))
                        .clickable { viewModel.cancelTransfer(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = ColorError,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = "CANCEL",
                            color = ColorError,
                            style = CustomTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
