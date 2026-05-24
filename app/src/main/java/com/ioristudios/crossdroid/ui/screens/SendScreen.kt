package com.ioristudios.crossdroid.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.data.FileKind
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.FilterTabs
import com.ioristudios.crossdroid.ui.components.GlowingButton
import com.ioristudios.crossdroid.ui.components.SelectableFileCard
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
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
fun SendScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entries by viewModel.fileManagerEntries.collectAsState()
    val isLoading by viewModel.isFileManagerLoading.collectAsState()
    val error by viewModel.fileManagerError.collectAsState()
    var hasAllFilesAccess by remember { mutableStateOf(viewModel.hasAllFilesAccess()) }
    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasAllFilesAccess = viewModel.hasAllFilesAccess()
        if (hasAllFilesAccess) {
            viewModel.loadCurrentDirectory()
        }
    }

    LaunchedEffect(activeFilter, hasAllFilesAccess) {
        if (hasAllFilesAccess && activeFilter == FileType.ALL && entries.isEmpty()) {
            viewModel.loadStorageRoot()
        }
    }

    val visibleEntries = remember(entries, activeFilter, searchQuery) {
        val filtered = entries.filter { entry ->
            val matchesSearch = searchQuery.isBlank() || entry.name.contains(searchQuery, ignoreCase = true)
            val matchesType = activeFilter == FileType.ALL ||
                (entry.kind == FileKind.FILE && entry.type == activeFilter)
            matchesSearch && matchesType
        }
        if (activeFilter != FileType.ALL) {
            val dummyItems = MockData.filesList.filter { it.type == activeFilter && (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) }
            filtered + dummyItems
        } else {
            filtered
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Send Files",
            viewModel = viewModel,
            showBackButton = true,
            showSearch = true
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        FilterTabs(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = Spacing.Medium)
        )

        Spacer(modifier = Modifier.height(Spacing.Small))

        if (activeFilter == FileType.ALL) {
            BreadcrumbBar(
                breadcrumbs = viewModel.directoryBreadcrumbs(),
                onCrumbClick = { viewModel.openDirectory(it, context) },
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )
        } else {
            CategoryHint(
                activeFilter = activeFilter,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                !hasAllFilesAccess -> PermissionGate(
                    onGrantClick = {
                        allFilesSettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                isLoading -> LoadingState(modifier = Modifier.fillMaxSize())

                error != null -> ErrorState(
                    message = error ?: "Unable to load this folder.",
                    onRetry = { viewModel.loadCurrentDirectory() },
                    modifier = Modifier.fillMaxSize()
                )

                visibleEntries.isEmpty() -> EmptyState(
                    activeFilter = activeFilter,
                    query = searchQuery,
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.Medium,
                        end = Spacing.Medium,
                        top = Spacing.Small,
                        bottom = if (selectedFiles.isEmpty()) Spacing.Large else 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    items(visibleEntries, key = { it.id }) { file ->
                        SelectableFileCard(
                            file = file,
                            isSelected = selectedFiles.any { it.id == file.id },
                            viewModel = viewModel,
                            onOpenFolder = { viewModel.openDirectory(it, context) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedFiles.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            SendCommandBar(
                selectedCount = selectedFiles.size,
                folderCount = selectedFiles.count { it.kind == FileKind.FOLDER },
                onClear = { viewModel.clearSelectedFiles(context) },
                onNext = { viewModel.navigateTo(Screen.QR_SCAN, context) }
            )
        }
    }
}

@Composable
private fun SendPackageSummary(
    selectedCount: Int,
    currentPath: String,
    activeFilter: FileType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Medium)
            .clip(RoundedCornerShape(Radii.CardStandard))
            .background(
                Brush.linearGradient(
                    listOf(
                        BgElevated.copy(alpha = 0.98f),
                        BgSurface.copy(alpha = 0.88f)
                    )
                )
            )
            .border(1.dp, BorderSubtle.copy(alpha = 0.85f), RoundedCornerShape(Radii.CardStandard))
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonPrimary.copy(alpha = 0.16f))
                .border(1.dp, NeonHighlight.copy(alpha = 0.38f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (activeFilter == FileType.ALL) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(IconSize.Standard)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (selectedCount == 0) "Build transfer package" else "$selectedCount items queued",
                style = CustomTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentPath,
                style = CustomTypography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onCrumbClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            breadcrumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    Text(
                        text = "/",
                        style = CustomTypography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = Spacing.Tiny)
                    )
                }
                Text(
                    text = crumb.first,
                    style = CustomTypography.labelMedium.copy(
                        fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    color = if (index == breadcrumbs.lastIndex) NeonHighlight else TextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCrumbClick(crumb.second) }
                        .padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                )
            }
        }
    }
}

@Composable
private fun CategoryHint(activeFilter: FileType, modifier: Modifier = Modifier) {
    Text(
        text = "Showing ${activeFilter.name.lowercase().replaceFirstChar { it.uppercase() }} files in the current location",
        style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
        color = TextSecondary,
        modifier = modifier.fillMaxWidth(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PermissionGate(onGrantClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NeonPrimary.copy(alpha = 0.14f))
                .border(1.dp, NeonHighlight.copy(alpha = 0.36f), RoundedCornerShape(22.dp))
                .neonGlow(NeonPrimary, borderRadius = 22.dp, glowRadius = 16.dp, opacity = 0.14f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.Large))
        Text(
            text = "Enable all-files access",
            style = CustomTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            color = TextStrong,
            textAlign = TextAlign.Center
        )
        Text(
            text = "CrossDroid needs broad storage access to show folders and files inside the internal file manager.",
            style = CustomTypography.bodyMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = Spacing.Small)
        )
        GlowingButton(
            text = "OPEN SETTINGS",
            onClick = onGrantClick,
            modifier = Modifier.padding(top = Spacing.Medium),
            glowColor = NeonPrimary
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeonHighlight)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            color = TextStrong,
            textAlign = TextAlign.Center
        )
        GlowingButton(
            text = "RETRY",
            onClick = onRetry,
            modifier = Modifier.padding(top = Spacing.Medium),
            glowColor = NeonHighlight
        )
    }
}

@Composable
private fun EmptyState(activeFilter: FileType, query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (query.isBlank()) "No items here" else "No matches found",
            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            color = TextStrong
        )
        Text(
            text = if (activeFilter == FileType.ALL) {
                "This folder is empty or inaccessible."
            } else {
                "Try a different file type or clear search."
            },
            style = CustomTypography.bodyMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SendCommandBar(
    selectedCount: Int,
    folderCount: Int,
    onClear: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
            .background(BgElevated)
            .border(
                1.dp,
                BorderSubtle.copy(alpha = 0.9f),
                RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet)
            )
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$selectedCount selected",
                style = CustomTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp
                ),
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (folderCount == 0) "Ready for receiver pairing" else "$folderCount folders included as package items",
                style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onClear,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BgPanelMuted)
                .border(1.dp, BorderSubtle, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear selected files",
                tint = TextSecondary,
                modifier = Modifier.size(IconSize.Small)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(Radii.ButtonSmall))
                .neonGlow(NeonPrimary, borderRadius = Radii.ButtonSmall, glowRadius = 12.dp, opacity = 0.24f)
                .background(Brush.horizontalGradient(listOf(NeonPrimary, NeonHighlight)))
                .border(1.dp, NeonHighlight.copy(alpha = 0.72f), RoundedCornerShape(Radii.ButtonSmall))
                .clickable(onClick = onNext)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(IconSize.Small)
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = "SEND",
                    style = CustomTypography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                    color = Color.White
                )
            }
        }
    }
}
