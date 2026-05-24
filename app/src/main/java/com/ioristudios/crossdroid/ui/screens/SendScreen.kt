package com.ioristudios.crossdroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.FilterTabs
import com.ioristudios.crossdroid.ui.components.GlowingButton
import com.ioristudios.crossdroid.ui.components.SelectableFileCard
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextBody
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextStrong

@Composable
fun SendScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Filter files list based on selection category and search query
    val filteredFiles = MockData.filesList.filter { file ->
        val matchesCategory = activeFilter == FileType.ALL || file.type == activeFilter
        val matchesSearch = searchQuery.isEmpty() || file.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Select Files to Send",
            viewModel = viewModel,
            showBackButton = true,
            showSearch = true
        )

        // Filter categories row
        FilterTabs(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
        )

        // Scrollable file choices
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.Large),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No files match search/filter",
                        style = CustomTypography.titleMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    items(filteredFiles) { file ->
                        val isSelected = selectedFiles.contains(file)
                        SelectableFileCard(
                            file = file,
                            isSelected = isSelected,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Bottom action footer bar (slides in when files are selected)
        AnimatedVisibility(
            visible = selectedFiles.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgElevated)
                    .border(width = 1.dp, color = BgSurface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${selectedFiles.size} items selected",
                        style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = TextStrong
                    )
                    Text(
                        text = "Ready to broadcast",
                        style = CustomTypography.labelMedium,
                        color = TextMuted
                    )
                }

                GlowingButton(
                    text = "NEXT",
                    onClick = {
                        viewModel.navigateTo(Screen.QR_SCAN, context)
                    },
                    modifier = Modifier.width(130.dp),
                    hapticIntensity = "strong"
                )
            }
        }
    }
}
