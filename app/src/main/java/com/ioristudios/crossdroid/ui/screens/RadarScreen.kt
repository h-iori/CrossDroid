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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.NearbyDeviceChip
import com.ioristudios.crossdroid.ui.components.RadarWidget
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
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

@Composable
fun RadarScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Nearby devices lists that we display as discoverable targets
    val radarDevices = MockData.deviceNodes.filter { it.status == "Nearby" || it.status == "Paired" }
    val chosenFiles = viewModel.selectedFiles.collectAsState().value.toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Radar Discovery",
            viewModel = viewModel,
            showBackButton = true
        )

        // 1. Radar animation viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            contentAlignment = Alignment.Center
        ) {
            // Spinning canvas sweep
            RadarWidget(
                modifier = Modifier.size(280.dp)
            )

            // Orbiting Device Nodes positioned dynamically
            if (radarDevices.size >= 2) {
                NearbyDeviceChip(
                    device = radarDevices[0],
                    onClick = {
                        viewModel.startTransferFlow(radarDevices[0], chosenFiles, isIncoming = false, context = context)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 40.dp, y = 30.dp)
                )

                NearbyDeviceChip(
                    device = radarDevices[1],
                    onClick = {
                        viewModel.startTransferFlow(radarDevices[1], chosenFiles, isIncoming = false, context = context)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-30).dp, y = (-40).dp)
                )
            }
            if (radarDevices.size >= 3) {
                NearbyDeviceChip(
                    device = radarDevices[2],
                    onClick = {
                        viewModel.startTransferFlow(radarDevices[2], chosenFiles, isIncoming = false, context = context)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-20).dp, y = 50.dp)
                )
            }
        }

        // 2. Discoverable devices list layout below
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
                .background(BgElevated)
                .border(
                    width = 1.dp,
                    color = BgSurface,
                    shape = RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet)
                )
                .padding(Spacing.Medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth broadcast",
                    tint = NeonHighlight,
                    modifier = Modifier.size(IconSize.Small)
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = "Nearby Peer Terminals",
                    style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = TextStrong
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(radarDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radii.ButtonSmall))
                            .background(BgSurface)
                            .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(Radii.ButtonSmall))
                            .clickable {
                                viewModel.startTransferFlow(device, chosenFiles, isIncoming = false, context = context)
                            }
                            .padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = CustomTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextStrong
                            )
                            Text(
                                text = "Signal strength: ${device.signalStrength}/5 • ${device.osType.uppercase()}",
                                style = CustomTypography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "CONNECT",
                            color = NeonHighlight,
                            style = CustomTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
