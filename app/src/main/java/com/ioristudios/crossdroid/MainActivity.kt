package com.ioristudios.crossdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import com.ioristudios.crossdroid.ui.components.ConfirmationPopup
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import com.ioristudios.crossdroid.ui.theme.AccentCyan

class MainActivity : ComponentActivity() {
    private val viewModel: CrossDroidViewModel by viewModels()

    private var backendService: com.ioristudios.crossdroid.backend.CrossDroidBackendService? = null
    private var isBound = false

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as com.ioristudios.crossdroid.backend.CrossDroidBackendService.LocalBinder
            backendService = binder.getService()
            isBound = true
            viewModel.setBackendService(backendService!!)
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            isBound = false
            backendService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val serviceIntent = android.content.Intent(this, com.ioristudios.crossdroid.backend.CrossDroidBackendService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        
        // Turn on edge-to-edge system display values
        enableEdgeToEdge()
        
        setContent {
            CrossDroidTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isSidebarVisible by viewModel.isSidebarVisible.collectAsState()
                val showReceivePopup by viewModel.showReceivePopup.collectAsState()
                val pendingRequest by viewModel.pendingIncomingRequest.collectAsState()
                
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                
                // To readable size extension
                fun Long.toReadableSize(): String {
                    if (this <= 0) return "0 B"
                    val units = arrayOf("B", "KB", "MB", "GB", "TB")
                    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
                    return String.format(java.util.Locale.US, "%.1f %s", this / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
                }
                
                var hasRequestedPermissions by remember {
                    val sharedPrefs = context.getSharedPreferences("crossdroid_prefs", android.content.Context.MODE_PRIVATE)
                    mutableStateOf(sharedPrefs.getBoolean("has_requested_permissions", false))
                }

                val manageStorageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) {
                    // storage permission returned
                }

                val permissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (!android.os.Environment.isExternalStorageManager()) {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                manageStorageLauncher.launch(intent)
                            } catch (e: Exception) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                manageStorageLauncher.launch(intent)
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (!hasRequestedPermissions) {
                        context.getSharedPreferences("crossdroid_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("has_requested_permissions", true).apply()
                        hasRequestedPermissions = true
                        
                        val list = mutableListOf<String>()
                        list.add(android.Manifest.permission.CAMERA)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                            list.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                            list.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                            list.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            list.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            list.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            list.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                            list.add(android.Manifest.permission.BLUETOOTH_SCAN)
                        }
                        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                            list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        permissionsLauncher.launch(list.toTypedArray())
                    }
                }
                val snackbarHostState = remember { SnackbarHostState() }
                var lastBackPressTime by remember { mutableLongStateOf(0L) }

                val breathingTransition = rememberInfiniteTransition(label = "SnackbarBreathe")
                val breatheOpacity by breathingTransition.animateFloat(
                    initialValue = 0.14f,
                    targetValue = 0.32f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "SnackbarBreatheOpacity"
                )

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
                            val currentData = snackbarHostState.currentSnackbarData
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedVisibility(
                                    visible = currentData != null,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn(),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeOut()
                                ) {
                                    if (currentData != null) {
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            BgPanel.copy(alpha = 0.95f),
                                                            BgPanelMuted.copy(alpha = 0.92f)
                                                        )
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            NeonPrimary.copy(alpha = 0.45f),
                                                            AccentCyan.copy(alpha = 0.35f)
                                                        )
                                                    ),
                                                    shape = CircleShape
                                                )
                                                .neonGlow(
                                                    color = NeonPrimary,
                                                    borderRadius = 22.dp,
                                                    glowRadius = 14.dp,
                                                    opacity = breatheOpacity
                                                )
                                                .padding(horizontal = Spacing.Medium + 4.dp, vertical = Spacing.Small + 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = NeonHighlight,
                                                modifier = Modifier.size(IconSize.Standard)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.Small + 2.dp))
                                            Text(
                                                text = currentData.visuals.message,
                                                style = CustomTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = TextStrong
                                            )
                                        }
                                    }
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
                    
                    if (pendingRequest != null) {
                        ConfirmationPopup(
                            visible = showReceivePopup,
                            deviceName = pendingRequest!!.deviceName,
                            filesCount = 1, // Single file offer
                            totalSize = pendingRequest!!.offer.TotalBytes.toReadableSize(),
                            fileNames = listOf(pendingRequest!!.offer.FileName),
                            onAccept = { viewModel.acceptIncomingTransfer(context) },
                            onDecline = { viewModel.declineIncomingTransfer(context) }
                        )
                    }
                }
            }
        }
    }
}
