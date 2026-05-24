package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileItem
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.theme.BgElevated
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
fun SelectableFileCard(
    file: FileItem,
    isSelected: Boolean,
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(200),
        label = "FileCardScale"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) BgSurface else BgElevated,
        animationSpec = tween(200),
        label = "FileCardBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonPrimary else Color.Transparent,
        animationSpec = tween(200),
        label = "FileCardBorder"
    )

    val fileIcon = when (file.type) {
        FileType.VIDEO -> Icons.Default.PlayCircle
        FileType.IMAGE -> Icons.Default.Image
        FileType.MUSIC -> Icons.Default.Audiotrack
        else -> Icons.Default.Description
    }

    val iconColor = when (file.type) {
        FileType.VIDEO -> NeonHighlight
        FileType.IMAGE -> Color(0xFF00E5FF)
        FileType.MUSIC -> ColorSuccess
        else -> Color(0xFFFFD600)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(Radii.CardStandard))
            .then(
                if (isSelected) {
                    Modifier.neonGlow(
                        color = NeonPrimary,
                        borderRadius = Radii.CardStandard,
                        glowRadius = 8.dp,
                        opacity = 0.2f
                    )
                } else Modifier
            )
            .background(cardBgColor)
            .border(
                width = 1.dp,
                color = if (isSelected) borderColor else BgSurface,
                shape = RoundedCornerShape(Radii.CardStandard)
            )
            .clickable {
                viewModel.toggleFileSelected(file, context)
            }
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Circle Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
                .border(width = 1.dp, color = iconColor.copy(alpha = 0.3f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = "File Type Icon",
                tint = iconColor,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        // File description texts
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                style = CustomTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.size} • ${file.detail}",
                style = CustomTypography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        // Selection Indicator checkbox shape
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) NeonPrimary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isSelected) NeonHighlight else TextMuted,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(tween(150)),
                exit = scaleOut(tween(150))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = TextStrong,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
