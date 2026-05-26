package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.DeviceNode
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.NearbyDeviceChip
import com.ioristudios.crossdroid.ui.components.RadarWidget
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.AccentGreen
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            showBackButton = true,
            subtitle = "Nearby peer terminals"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium)
        ) {

            RadarViewport(
                devices = radarDevices,
                onDeviceClick = { device ->
                    viewModel.startTransferFlow(device, chosenFiles, isIncoming = false, context = context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        DeviceSheet(
            devices = radarDevices,
            onDeviceClick = { device ->
                viewModel.startTransferFlow(device, chosenFiles, isIncoming = false, context = context)
            }
        )
    }
}


@Composable
private fun RadarViewport(
    devices: List<DeviceNode>,
    onDeviceClick: (DeviceNode) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .neonGlow(AccentCyan, borderRadius = 24.dp, glowRadius = 18.dp, opacity = 0.10f)
            .background(BgPanel)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        AccentCyan.copy(alpha = 0.38f),
                        NeonPrimary.copy(alpha = 0.35f),
                        BorderSubtle
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        val radarSize = minOf(maxWidth, maxHeight) * 1.0f
        val radius = radarSize / 2

        RadarWidget(
            modifier = Modifier.size(radarSize),
            sweepDuration = 2600
        )

        CoordinateLabel("N", Modifier.align(Alignment.TopCenter))
        CoordinateLabel("W", Modifier.align(Alignment.CenterStart))
        CoordinateLabel("E", Modifier.align(Alignment.CenterEnd))
        CoordinateLabel("S", Modifier.align(Alignment.BottomCenter))

        devices.take(4).forEachIndexed { index, device ->
            val angle = when (index) {
                0 -> Math.toRadians(-42.0)
                1 -> Math.toRadians(145.0)
                2 -> Math.toRadians(38.0)
                else -> Math.toRadians(218.0)
            }
            val distance = when (index) {
                0 -> 0.68f
                1 -> 0.78f
                2 -> 0.52f
                else -> 0.44f
            }
            NearbyDeviceChip(
                device = device,
                onClick = { onDeviceClick(device) },
                modifier = Modifier.offset(
                    x = radius * distance * cos(angle).toFloat(),
                    y = radius * distance * sin(angle).toFloat()
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(14.dp))
                .background(BgMain.copy(alpha = 0.64f))
                .border(1.dp, BorderSubtle.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
        ) {
            Text(
                text = "DISCOVERY MESH",
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = TextMuted
            )
            Text(
                text = "Tap a terminal to connect",
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CoordinateLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = CustomTypography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        ),
        color = AccentCyan.copy(alpha = 0.58f),
        modifier = modifier.padding(Spacing.Small)
    )
}

@Composable
private fun DeviceSheet(
    devices: List<DeviceNode>,
    onDeviceClick: (DeviceNode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
            .background(BgElevated)
            .border(
                width = 1.dp,
                color = BorderSubtle,
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
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(IconSize.Small)
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Available Terminals",
                    style = CustomTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.sp
                    ),
                    color = TextStrong
                )
                Text(
                    text = "Signal strength and trust state",
                    style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(IconSize.Small)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            itemsIndexed(devices, key = { _, item -> item.id }) { index, device ->
                DeviceDiscoveryRow(
                    device = device,
                    index = index,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
private fun DeviceDiscoveryRow(
    device: DeviceNode,
    index: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 30L)
        visible = true
    }

    val alphaVal by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "discoveryRowAlpha"
    )
    val translationYVal by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "discoveryRowTranslation"
    )

    val accent = if (device.osType == "android") ColorSuccess else AccentCyan

    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = alphaVal
                translationY = translationYVal.dp.toPx()
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeviceAvatar(device = device, accent = accent)

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.name,
                    style = CustomTypography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = TextStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                DeviceBadge(text = device.osType.uppercase(), accent = accent)
            }

            Spacer(modifier = Modifier.height(Spacing.Tiny))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalBars(strength = device.signalStrength, accent = accent)
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = "${device.status} | ${device.lastSeen}",
                    style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Text(
            text = "CONNECT",
            color = NeonHighlight,
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        )
    }
}

@Composable
private fun DeviceAvatar(
    device: DeviceNode,
    accent: Color
) {
    val icon: ImageVector = if (device.osType == "android") {
        Icons.Default.PhoneAndroid
    } else {
        Icons.Default.Computer
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(IconSize.Standard)
        )
    }
}

@Composable
private fun DeviceBadge(
    text: String,
    accent: Color
) {
    Text(
        text = text,
        style = CustomTypography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        ),
        color = accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = Spacing.Small, vertical = 2.dp)
    )
}

@Composable
private fun SignalBars(
    strength: Int,
    accent: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (index in 1..5) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((6 + index * 2).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index <= strength) accent else BorderSubtle)
            )
        }
    }
}
