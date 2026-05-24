package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
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
fun ConfirmationPopup(
    visible: Boolean,
    deviceName: String,
    filesCount: Int,
    totalSize: String,
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
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Glassmorphic Dialog Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
                        .neonGlow(
                            color = NeonPrimary,
                            borderRadius = Radii.OverlaySheet,
                            glowRadius = 18.dp,
                            opacity = 0.3f
                        )
                        .background(BgElevated)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeonHighlight.copy(alpha = 0.4f), BgSurface)
                            ),
                            shape = RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet)
                        )
                        .padding(Spacing.Large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing Header Icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(NeonPrimary.copy(alpha = 0.15f))
                            .border(width = 1.dp, color = NeonPrimary.copy(alpha = 0.3f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Incoming Transfer",
                            tint = NeonHighlight,
                            modifier = Modifier.size(IconSize.Large)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    Text(
                        text = "Incoming Transfer Request",
                        style = CustomTypography.headlineMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextStrong,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.Small))

                    // Peer details
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radii.ButtonSmall))
                            .background(BgSurface)
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Sender",
                            tint = ColorSuccess,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = deviceName,
                            style = CustomTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextStrong
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    Text(
                        text = "Wants to share $filesCount files ($totalSize) with you.",
                        style = CustomTypography.bodyMedium,
                        color = TextBody,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.Large))

                    // Action buttons
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
                                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                                        colors = listOf(ColorSuccess, Color(0xFF00FFC4))
                                    )
                                )
                                .neonGlow(
                                    color = ColorSuccess,
                                    borderRadius = Radii.ButtonSmall,
                                    glowRadius = 8.dp,
                                    opacity = 0.25f
                                )
                                .border(width = 1.dp, color = ColorSuccess, shape = RoundedCornerShape(Radii.ButtonSmall))
                                .clickable { onAccept() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Accept",
                                color = BgMain,
                                style = CustomTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
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
