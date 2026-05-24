package com.ioristudios.crossdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.AppSidebar
import com.ioristudios.crossdroid.ui.components.BottomNavbar
import com.ioristudios.crossdroid.ui.navigation.NavigationHost
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.CrossDroidTheme
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.neonGlow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.IconSize

class MainActivity : ComponentActivity() {
    private val viewModel: CrossDroidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Turn on edge-to-edge system display values
        enableEdgeToEdge()
        
        setContent {
            CrossDroidTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isSidebarVisible by viewModel.isSidebarVisible.collectAsState()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                var lastBackPressTime by remember { mutableLongStateOf(0L) }

                BackHandler(enabled = true) {
                    if (isSidebarVisible) {
                        viewModel.setSidebarVisible(false)
                    } else if (currentScreen != Screen.HOME) {
                        viewModel.navigateBack(context, triggerHaptic = false)
                    } else {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            (context as? android.app.Activity)?.finish()
                        } else {
                            lastBackPressTime = currentTime
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = "Double press back to exit",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            // Persistent navbar only on the three primary hubs
                            if (currentScreen in listOf(Screen.HOME, Screen.DEVICES, Screen.HISTORY)) {
                                BottomNavbar(viewModel = viewModel)
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                                        .clip(RoundedCornerShape(Radii.CardStandard))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    BgPanel.copy(alpha = 0.95f),
                                                    BgPanelMuted.copy(alpha = 0.90f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    NeonPrimary.copy(alpha = 0.45f),
                                                    BorderSubtle.copy(alpha = 0.40f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(Radii.CardStandard)
                                        )
                                        .neonGlow(
                                            color = NeonPrimary,
                                            borderRadius = Radii.CardStandard,
                                            glowRadius = 12.dp,
                                            opacity = 0.18f
                                        )
                                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small + 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left vertical accent strip
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(NeonPrimary, NeonHighlight)
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.Medium))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = NeonHighlight,
                                        modifier = Modifier.size(IconSize.Standard)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.Small + 2.dp))
                                    Text(
                                        text = data.visuals.message,
                                        style = CustomTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextStrong,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        },
                        containerColor = BgMain,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BgMain)
                                .padding(innerPadding)
                        ) {
                            NavigationHost(viewModel = viewModel)
                        }
                    }

                    AppSidebar(
                        isVisible = isSidebarVisible,
                        onDismiss = { viewModel.setSidebarVisible(false) },
                        onAboutClick = {
                            viewModel.setSidebarVisible(false)
                            viewModel.navigateTo(Screen.ABOUT)
                        }
                    )
                }
            }
        }
    }
}
