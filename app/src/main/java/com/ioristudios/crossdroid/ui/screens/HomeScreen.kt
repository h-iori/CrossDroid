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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgSurface
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
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BgElevated,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgElevated)
                        .padding(Spacing.Large)
                ) {
                    Text(
                        text = "CrossDroid Menu",
                        style = CustomTypography.headlineMedium.copy(fontSize = 18.sp),
                        color = TextStrong
                    )
                    Spacer(modifier = Modifier.height(Spacing.Large))
                    
                    Text(
                        text = "IORI STUDIOS v1.0.0",
                        style = CustomTypography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BgSurface)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgMain)
        ) {
            TopAppBar(
                title = "CrossDroid",
                viewModel = viewModel,
                showBackButton = false,
                onMenuClick = {
                    scope.launch {
                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                    }
                }
            )

            // Brand Header Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(horizontal = Spacing.Large),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // White text glowing neon purple
                Text(
                    text = "CrossDroid",
                    color = TextStrong,
                    style = CustomTypography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier
                        .neonGlow(
                            color = NeonPrimary,
                            borderRadius = 8.dp,
                            glowRadius = 24.dp,
                            opacity = 0.45f
                        )
                )
                
                Spacer(modifier = Modifier.height(Spacing.Small))
                
                Text(
                    text = "by IORI STUDIOS",
                    color = TextSecondary,
                    style = CustomTypography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // Central Actions Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .padding(horizontal = Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Send Button Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(Radii.CardStandard))
                        .neonGlow(NeonPrimary, borderRadius = Radii.CardStandard, glowRadius = 12.dp, opacity = 0.15f)
                        .background(BgElevated)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(NeonPrimary.copy(alpha = 0.4f), Color.Transparent)),
                            shape = RoundedCornerShape(Radii.CardStandard)
                        )
                        .clickable {
                            viewModel.navigateTo(Screen.SEND, context)
                        }
                        .padding(Spacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(Radii.ButtonSmall))
                                .background(NeonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Send",
                                tint = NeonHighlight,
                                modifier = Modifier.size(IconSize.Large)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                        Column {
                            Text(
                                text = "SEND",
                                style = CustomTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = TextStrong
                            )
                            Text(
                                text = "Share files with nearby devices",
                                style = CustomTypography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Large Receive Button Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(Radii.CardStandard))
                        .neonGlow(Color(0xFF00E5FF), borderRadius = Radii.CardStandard, glowRadius = 12.dp, opacity = 0.15f)
                        .background(BgElevated)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent)),
                            shape = RoundedCornerShape(Radii.CardStandard)
                        )
                        .clickable {
                            viewModel.navigateTo(Screen.RECEIVE, context)
                        }
                        .padding(Spacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(Radii.ButtonSmall))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Receive",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(IconSize.Large)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                        Column {
                            Text(
                                text = "RECEIVE",
                                style = CustomTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = TextStrong
                            )
                            Text(
                                text = "Wait for others to send files",
                                style = CustomTypography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp)) // Offset BottomNav overlapping area
        }
    }
}
