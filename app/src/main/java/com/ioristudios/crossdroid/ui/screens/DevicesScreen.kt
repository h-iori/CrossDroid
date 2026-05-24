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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
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
fun DevicesScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val devices = MockData.deviceNodes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Devices Pairing Hub",
            viewModel = viewModel
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                Text(
                    text = "Active & Paired Nodes",
                    style = CustomTypography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = TextStrong,
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )
            }

            items(devices) { device ->
                val isConnected = device.status == "Connected"
                val deviceColor = if (device.osType == "android") ColorSuccess else Color(0xFF00E5FF)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radii.CardStandard))
                        .background(BgElevated)
                        .border(
                            width = 1.dp,
                            color = if (isConnected) NeonPrimary.copy(alpha = 0.5f) else BgSurface,
                            shape = RoundedCornerShape(Radii.CardStandard)
                        )
                        .padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Device OS Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(deviceColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (device.osType == "android") Icons.Default.PhoneAndroid else Icons.Default.Computer,
                            contentDescription = "Device OS Type",
                            tint = deviceColor,
                            modifier = Modifier.size(IconSize.Standard)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.Medium))

                    // Name and last seen status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = device.name,
                            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = TextStrong
                        )
                        Text(
                            text = "${device.status} • ${device.lastSeen}",
                            style = CustomTypography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    // Status Indicator Dot or Signal Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isConnected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ColorSuccess)
                                    .neonGlow(ColorSuccess, borderRadius = 4.dp, glowRadius = 4.dp, opacity = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(
                                text = "Connected",
                                style = CustomTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ColorSuccess
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Signal Strength",
                                tint = TextMuted,
                                modifier = Modifier.size(IconSize.Small)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Tiny))
                            Text(
                                text = "${device.signalStrength}/5",
                                style = CustomTypography.labelMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(72.dp)) // bottom navbar buffer
    }
}
