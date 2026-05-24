package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun HomeScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "CrossDroid",
            viewModel = viewModel,
            showBackButton = false,
            onMenuClick = { viewModel.setSidebarVisible(true) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeonActionPanel(
                title = "SEND",
                description = "Select files and transfer them instantly to nearby devices.",
                icon = Icons.Default.FileUpload,
                accent = NeonPrimary,
                secondaryAccent = NeonHighlight,
                onClick = { viewModel.navigateTo(Screen.SEND, context) }
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            NeonActionPanel(
                title = "RECEIVE",
                description = "Make this device discoverable and accept incoming files.",
                icon = Icons.Default.FileDownload,
                accent = AccentCyan,
                secondaryAccent = Color(0xFF7DF9FF),
                onClick = { viewModel.navigateTo(Screen.RECEIVE, context) }
            )
        }
    }
}

@Composable
private fun NeonActionPanel(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    secondaryAccent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(Radii.OverlaySheet))
            .neonGlow(
                color = accent,
                borderRadius = Radii.OverlaySheet,
                glowRadius = 22.dp,
                opacity = 0.34f
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.26f),
                        BgPanel,
                        secondaryAccent.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        secondaryAccent.copy(alpha = 0.90f),
                        accent.copy(alpha = 0.35f),
                        BorderSubtle
                    )
                ),
                shape = RoundedCornerShape(Radii.OverlaySheet)
            )
            .clickable(onClick = onClick)
            .padding(Spacing.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .neonGlow(
                    color = accent,
                    borderRadius = 22.dp,
                    glowRadius = 14.dp,
                    opacity = 0.42f
                )
                .background(accent.copy(alpha = 0.20f))
                .border(1.dp, secondaryAccent.copy(alpha = 0.72f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = secondaryAccent,
                modifier = Modifier.size(IconSize.Huge)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Large))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CustomTypography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = 1.2.sp
                ),
                color = TextStrong
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = description,
                style = CustomTypography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
