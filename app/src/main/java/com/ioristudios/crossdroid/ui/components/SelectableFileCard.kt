package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileItem
import com.ioristudios.crossdroid.data.FileKind
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.theme.AccentAmber
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.AccentGreen
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
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

@Composable
fun SelectableFileCard(
    file: FileItem,
    isSelected: Boolean,
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier,
    onOpenFolder: ((FileItem) -> Unit)? = null
) {
    val context = LocalContext.current

    val iconSpec = file.iconSpec()
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) BgSurface.copy(alpha = 0.98f) else BgElevated,
        label = "FileRowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonPrimary.copy(alpha = 0.86f) else BorderSubtle.copy(alpha = 0.72f),
        label = "FileRowBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(Radii.ButtonSmall))
            .then(
                if (isSelected) {
                    Modifier.neonGlow(
                        color = NeonPrimary,
                        borderRadius = Radii.ButtonSmall,
                        glowRadius = 8.dp,
                        opacity = 0.12f
                    )
                } else {
                    Modifier
                }
            )
            .background(cardBgColor)
            .border(1.dp, borderColor, RoundedCornerShape(Radii.ButtonSmall))
            .clickable {
                if (file.kind == FileKind.FOLDER && onOpenFolder != null) {
                    onOpenFolder(file)
                } else {
                    viewModel.toggleFileSelected(file, context)
                }
            }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconSpec.tint.copy(alpha = if (file.kind == FileKind.FOLDER) 0.18f else 0.12f))
                .border(1.dp, iconSpec.tint.copy(alpha = 0.34f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconSpec.icon,
                contentDescription = iconSpec.description,
                tint = iconSpec.tint,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = CustomTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val metadata = buildString {
                append(file.size)
                if (file.detail.isNotBlank()) append(" | ").append(file.detail)
                if (file.lastModified.isNotBlank()) append(" | ").append(file.lastModified)
            }

            Text(
                text = metadata,
                style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        SelectionControl(
            isSelected = isSelected,
            contentDescription = if (isSelected) "Deselect ${file.name}" else "Select ${file.name}",
            onClick = { viewModel.toggleFileSelected(file, context) }
        )
    }
}

@Composable
private fun SelectionControl(
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(if (isSelected) NeonPrimary else BgPanelMuted)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) NeonHighlight else TextMuted.copy(alpha = 0.72f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = contentDescription,
                    tint = TextStrong,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private data class FileIconSpec(
    val icon: ImageVector,
    val tint: Color,
    val description: String
)

private fun FileItem.iconSpec(): FileIconSpec {
    if (kind == FileKind.FOLDER) {
        return FileIconSpec(Icons.Default.Folder, NeonHighlight, "Folder")
    }
    return when (type) {
        FileType.VIDEO -> FileIconSpec(Icons.Default.Movie, NeonHighlight, "Video file")
        FileType.IMAGE -> FileIconSpec(Icons.Default.Image, AccentCyan, "Image file")
        FileType.MUSIC -> FileIconSpec(Icons.Default.Audiotrack, AccentGreen, "Audio file")
        FileType.DOCUMENT -> FileIconSpec(Icons.Default.Description, AccentAmber, "Document file")
        FileType.ALL -> FileIconSpec(Icons.Default.Description, TextSecondary, "File")
    }
}
