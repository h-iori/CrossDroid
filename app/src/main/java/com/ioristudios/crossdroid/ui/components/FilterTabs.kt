package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.ioristudios.crossdroid.ui.theme.TextBody
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

    val tabs = listOf(
        FileType.ALL to "All Files",
        FileType.VIDEO to "Videos",
        FileType.IMAGE to "Images",
        FileType.MUSIC to "Music",
        FileType.DOCUMENT to "Docs"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (type, label) ->
            val isSelected = activeFilter == type
            
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) NeonPrimary.copy(alpha = 0.25f) else BgElevated,
                animationSpec = tween(250),
                label = "TabBgColor"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) NeonHighlight else BgSurface,
                animationSpec = tween(250),
                label = "TabBorderColor"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) TextStrong else TextMuted,
                animationSpec = tween(200),
                label = "TabTextColor"
            )

            val glowRadius by animateDpAsState(
                targetValue = if (isSelected) 6.dp else 0.dp,
                label = "TabGlowSize"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.Tiny)
                    .clip(RoundedCornerShape(Radii.ButtonSmall))
                    .then(
                        if (isSelected) {
                            Modifier.neonGlow(
                                color = NeonPrimary,
                                borderRadius = Radii.ButtonSmall,
                                glowRadius = glowRadius,
                                opacity = 0.15f
                            )
                        } else Modifier
                    )
                    .background(backgroundColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(Radii.ButtonSmall)
                    )
                    .clickable {
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

private fun FileType.filterIcon() = when (this) {
    FileType.ALL -> Icons.Default.Folder
    FileType.VIDEO -> Icons.Default.Movie
    FileType.IMAGE -> Icons.Default.Image
    FileType.MUSIC -> Icons.Default.Audiotrack
    FileType.DOCUMENT -> Icons.Default.Description
}
