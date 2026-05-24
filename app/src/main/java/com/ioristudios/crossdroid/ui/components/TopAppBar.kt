package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
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
fun TopAppBar(
    title: String,
    viewModel: CrossDroidViewModel,
    showBackButton: Boolean = false,
    showSearch: Boolean = false,
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
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = { viewModel.navigateTo(Screen.HOME, context) }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            } else {
                IconButton(onClick = { 
                    HapticHelper.triggerMedium(context)
                    onMenuClick() 
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            }

            // Title block or Search text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.Small),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showSearch && searchMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(Radii.ButtonSmall))
                            .background(BgElevated)
                            .neonGlow(NeonPrimary, borderRadius = Radii.ButtonSmall, glowRadius = 4.dp, opacity = 0.2f)
                            .padding(horizontal = Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            textStyle = CustomTypography.bodyMedium.copy(color = TextBody),
                            cursorBrush = SolidColor(NeonHighlight),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.Small),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
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
                } else {
                    Text(
                        text = title,
                        style = CustomTypography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextStrong
                    )
                }
            }

            // Search Icon actions
            if (showSearch) {
                IconButton(onClick = { viewModel.toggleSearchMode(context) }) {
                    Icon(
                        imageVector = if (searchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search FileToggle",
                        tint = if (searchMode) NeonPrimary else TextStrong,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                }
            }
        }

        // Cyberpunk Glowing bottom bar line separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NeonPrimary,
                            NeonHighlight,
                            NeonPrimary.copy(alpha = 0.1f)
                        )
                    )
                )
        )
    }
}
