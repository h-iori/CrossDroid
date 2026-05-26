package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhoneAndroid
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
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorError
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
fun ConfirmationPopup(
    visible: Boolean,
    deviceName: String,
    filesCount: Int,
    totalSize: String,
    fileNames: List<String> = emptyList(),
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Translucent background mask
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(enabled = false) {}, // absorb clicks
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    targetOffsetY = { it }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
                        .neonGlow(
                            color = AccentCyan,
                            borderRadius = Radii.OverlaySheet,
                            glowRadius = 18.dp,
                            opacity = 0.22f
                        )
                        .background(BgElevated)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(AccentCyan.copy(alpha = 0.42f), NeonPrimary.copy(alpha = 0.22f), BgSurface)
                            ),
                            shape = RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet)
                        )
                        .padding(Spacing.Large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BgSurface)
                    )

                    Spacer(modifier = Modifier.height(Spacing.Large))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.14f))
                            .border(width = 1.dp, color = AccentCyan.copy(alpha = 0.36f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Incoming Transfer",
                            tint = AccentCyan,
                            modifier = Modifier.size(IconSize.Large)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    Text(
                        text = "Incoming transfer request",
                        style = CustomTypography.headlineMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = TextStrong,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.Small))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radii.CardStandard))
                            .background(BgPanelMuted)
                            .border(1.dp, BgSurface, RoundedCornerShape(Radii.CardStandard))
                            .padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(NeonPrimary.copy(alpha = 0.13f))
                                .border(1.dp, NeonHighlight.copy(alpha = 0.26f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Sender",
                                tint = NeonHighlight,
                                modifier = Modifier.size(IconSize.Small)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deviceName,
                                style = CustomTypography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp
                                ),
                                color = TextStrong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Wants to send $filesCount files to this device",
                                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = totalSize,
                            style = CustomTypography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    if (fileNames.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BgSurface.copy(alpha = 0.72f))
                                .border(1.dp, BgSurface, RoundedCornerShape(14.dp))
                                .padding(Spacing.Small)
                        ) {
                            fileNames.take(3).forEach { fileName ->
                                FilePreviewRow(fileName = fileName)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.Large))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                    ) {
                        // Decline
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(Radii.ButtonSmall))
                                .background(ColorError.copy(alpha = 0.1f))
                                .border(width = 1.dp, color = ColorError.copy(alpha = 0.4f), shape = RoundedCornerShape(Radii.ButtonSmall))
                                .clickable { onDecline() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Decline",
                                color = ColorError,
                                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                            )
                        }

                        // Accept
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(Radii.ButtonSmall))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(AccentCyan, Color(0xFF00FFC4))
                                    )
                                )
                                .neonGlow(
                                    color = AccentCyan,
                                    borderRadius = Radii.ButtonSmall,
                                    glowRadius = 8.dp,
                                    opacity = 0.25f
                                )
                                .border(width = 1.dp, color = AccentCyan, shape = RoundedCornerShape(Radii.ButtonSmall))
                                .clickable { onAccept() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Accept",
                                color = BgMain,
                                style = CustomTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp,
                                    color = BgMain
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilePreviewRow(fileName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Small, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentCyan.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = fileName,
            style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
            color = TextBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
