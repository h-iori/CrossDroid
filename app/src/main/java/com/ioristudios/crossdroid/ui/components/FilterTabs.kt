package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun FilterTabs(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeFilter by viewModel.activeFilter.collectAsState()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val tabs = listOf(
        FileType.ALL to "All Files",
        FileType.VIDEO to "Videos",
        FileType.IMAGE to "Images",
        FileType.MUSIC to "Music",
        FileType.DOCUMENT to "Docs"
    )

    var tabPositions by remember { mutableStateOf(emptyMap<Int, Pair<Dp, Dp>>()) }
    val selectedIndex = remember(activeFilter, tabs) {
        tabs.indexOfFirst { it.first == activeFilter }.coerceAtLeast(0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = Spacing.Small),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding pill selection indicator behind tabs
        if (tabPositions.size == tabs.size) {
            val targetPosition = tabPositions[selectedIndex] ?: (0.dp to 0.dp)
            val indicatorOffset by animateDpAsState(
                targetValue = targetPosition.first,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "TabIndicatorOffset"
            )
            val indicatorWidth by animateDpAsState(
                targetValue = targetPosition.second,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "TabIndicatorWidth"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(38.dp)
                    .clip(RoundedCornerShape(Radii.ButtonSmall))
                    .background(NeonPrimary.copy(alpha = 0.25f))
                    .border(
                        width = 1.dp,
                        color = NeonHighlight,
                        shape = RoundedCornerShape(Radii.ButtonSmall)
                    )
                    .neonGlow(
                        color = NeonPrimary,
                        borderRadius = Radii.ButtonSmall,
                        glowRadius = 6.dp,
                        opacity = 0.15f
                    )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (type, label) ->
                val isSelected = activeFilter == type
                
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) TextStrong else TextMuted,
                    animationSpec = tween(200),
                    label = "TabTextColor"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "TabScale"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.Tiny)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInParent()
                            val widthDp = with(density) { coordinates.size.width.toDp() }
                            val offsetDp = with(density) { position.x.toDp() }
                            if (tabPositions[index]?.first != offsetDp || tabPositions[index]?.second != widthDp) {
                                tabPositions = tabPositions + (index to (offsetDp to widthDp))
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(Radii.ButtonSmall))
                        .background(if (isSelected) Color.Transparent else BgElevated)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else BgSurface,
                            shape = RoundedCornerShape(Radii.ButtonSmall)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            HapticHelper.triggerMedium(context)
                            viewModel.setFilter(type, context)
                        }
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = type.filterIcon(),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = label,
                            color = textColor,
                            style = CustomTypography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun FileType.filterIcon() = when (this) {
    FileType.ALL -> Icons.Default.Folder
    FileType.VIDEO -> Icons.Default.Movie
    FileType.IMAGE -> Icons.Default.Image
    FileType.MUSIC -> Icons.Default.Audiotrack
    FileType.DOCUMENT -> Icons.Default.Description
}
