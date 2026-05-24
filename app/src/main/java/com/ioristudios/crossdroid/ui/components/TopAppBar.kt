package com.ioristudios.crossdroid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.R
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.theme.AccentBlue
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BrandFontFamily
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.SubheadingFontFamily
import com.ioristudios.crossdroid.ui.theme.TextBody
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong

@Composable
fun TopAppBar(
    title: String,
    viewModel: CrossDroidViewModel,
    showBackButton: Boolean = false,
    showSearch: Boolean = false,
    subtitle: String? = null,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchMode = viewModel.searchMode.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgMain)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                BackIconButton(onClick = { viewModel.navigateBack(context) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.Small),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showSearch && searchMode) {
                    SearchField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) }
                    )
                } else if (title == "CrossDroid") {
                    BrandHeader()
                } else {
                    Column {
                        Text(
                            text = title,
                            style = CustomTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextStrong
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = CustomTypography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (showSearch) {
                BackIconButton(onClick = { viewModel.toggleSearchMode(context) }) {
                    Icon(
                        imageVector = if (searchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchMode) "Close search" else "Search files",
                        tint = if (searchMode) AccentBlue else TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            }

            if (!showBackButton) {
                IconButton(
                    onClick = {
                        HapticHelper.triggerMedium(context)
                        onMenuClick()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle.copy(alpha = 0.70f))
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.transfer_logo),
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
        )

        Box(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "CrossDroid",
                style = CustomTypography.titleLarge.copy(
                    fontFamily = BrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong
            )
            Text(
                text = "by IORI STUDIOS",
                style = CustomTypography.labelMedium.copy(
                    fontFamily = SubheadingFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 1.0.sp
                ),
                color = TextSecondary.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
private fun BackIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(BgPanelMuted.copy(alpha = 0.65f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(100.dp))
    ) {
        content()
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(Radii.ButtonSmall))
            .background(BgPanelMuted)
            .border(1.dp, BorderSubtle, RoundedCornerShape(Radii.ButtonSmall))
            .padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(IconSize.Small)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = CustomTypography.bodyMedium.copy(color = TextBody),
            cursorBrush = SolidColor(AccentBlue),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.Small),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "Search files...",
                        style = CustomTypography.bodyMedium,
                        color = TextMuted
                    )
                }
                innerTextField()
            }
        )
    }
}
