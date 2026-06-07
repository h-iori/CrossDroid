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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.DeviceNode
// import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun DevicesScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val groupedDevices = listOf(
        "Available Devices" to devices.filter { it.status == "Available" },
        "Previously Connected" to devices.filter { it.status == "Connected" },
        "Paired & Saved" to devices.filter { it.status == "Paired" }
    ).filter { it.second.isNotEmpty() }

    val savedDevicesCount = devices.count { it.status == "Connected" || it.status == "Paired" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Device directory",
            subtitle = "$savedDevicesCount saved and trusted devices",
            viewModel = viewModel,
            onMenuClick = { viewModel.setSidebarVisible(true) }
        )

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
                DeviceOverview(
                    connected = devices.count { it.status == "Connected" },
                    paired = devices.count { it.status == "Paired" }
                )
            }

            groupedDevices.forEach { (sectionTitle, sectionDevices) ->
                item(key = "section-$sectionTitle") {
                    SectionHeader(
                        title = sectionTitle,
                        count = sectionDevices.size
                    )
                }
                itemsIndexed(
                    items = sectionDevices,
                    key = { _, item -> item.id }
                ) { index, device ->
                    DeviceDirectoryRow(device = device, index = index)
                }
            }
        }
    }
}

@Composable
private fun DeviceOverview(
    connected: Int,
    paired: Int
) {
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        visible = true
    }

    val overviewAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "overviewAlpha"
    )
    val overviewTranslationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "overviewTranslation"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = overviewAlpha
                translationY = overviewTranslationY.dp.toPx()
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.CardStandard))
            .neonGlow(
                color = NeonPrimary,
                borderRadius = Radii.CardStandard,
                glowRadius = 12.dp,
                opacity = 0.12f
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BgPanel,
                        BgPanelMuted.copy(alpha = 0.98f),
                        NeonPrimary.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonPrimary.copy(alpha = 0.38f),
                        AccentCyan.copy(alpha = 0.22f),
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
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = NeonPrimary,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pairing health",
                style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextStrong
            )
            Text(
                text = "$connected connected / $paired paired & saved",
                style = CustomTypography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = CustomTypography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            ),
            color = TextStrong
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = "$count",
            style = CustomTypography.labelMedium,
            color = TextMuted
        )
    }
}

@Composable
private fun DeviceDirectoryRow(device: DeviceNode, index: Int) {
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 30L)
        visible = true
    }

    val rowAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "rowAlpha"
    )
    val rowTranslationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 250f),
        label = "rowTranslation"
    )

    val isConnected = device.status == "Connected"
    val deviceColor = if (isConnected) NeonPrimary else AccentCyan

    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = rowAlpha
                translationY = rowTranslationY.dp.toPx()
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = if (isConnected) deviceColor.copy(alpha = 0.34f) else BorderSubtle.copy(alpha = 0.82f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = Spacing.Medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(deviceColor.copy(alpha = 0.12f))
                .border(1.dp, deviceColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (device.osType == "android") Icons.Default.PhoneAndroid else Icons.Default.Computer,
                contentDescription = "${device.osType} device",
                tint = deviceColor,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
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
                text = device.lastSeen,
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        StatusChip(
            label = device.status,
            color = deviceColor
        )
    }
}

@Composable
private fun StatusChip(
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
