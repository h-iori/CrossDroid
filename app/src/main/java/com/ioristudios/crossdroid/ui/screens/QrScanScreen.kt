package com.ioristudios.crossdroid.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.AccentGreen
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
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
fun QrScanScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanLocked by remember { mutableStateOf(false) }
    var scannerStatus by remember { mutableStateOf("Awaiting camera access") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        scannerStatus = if (granted) "Scanning receiver identity" else "Camera access denied"
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            scannerStatus = "Scanning receiver identity"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Secure Pairing",
            viewModel = viewModel,
            showBackButton = true,
            subtitle = "QR receiver handshake"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScannerHeader(
                status = scannerStatus,
                hasCameraPermission = hasCameraPermission
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            ScannerSurface(
                hasCameraPermission = hasCameraPermission,
                torchEnabled = torchEnabled,
                onTorchChange = { torchEnabled = it },
                onGrantCamera = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onQrDetected = {
                    if (!scanLocked) {
                        scanLocked = true
                        scannerStatus = "Receiver verified"
                        val defaultDevice = MockData.deviceNodes.first { it.osType == "windows" }
                        val filesToSend = viewModel.selectedFiles.value.toList()
                        HapticHelper.triggerSuccess(context)
                        viewModel.startTransferFlow(
                            defaultDevice,
                            filesToSend,
                            isIncoming = false,
                            context = context
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        PairingActionBar(
            hasCameraPermission = hasCameraPermission,
            torchEnabled = torchEnabled,
            onTorchClick = { torchEnabled = !torchEnabled },
            onEnterCode = { viewModel.navigateTo(Screen.ENTER_CODE, context) },
            onRadar = { viewModel.navigateTo(Screen.RADAR, context) }
        )
    }
}

@Composable
private fun ScannerHeader(
    status: String,
    hasCameraPermission: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(BgPanelMuted)
                .border(1.dp, BorderSubtle, CircleShape)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasCameraPermission) AccentGreen else ColorError)
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = status.uppercase(),
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = if (hasCameraPermission) AccentCyan else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Text(
            text = "Align the receiver QR inside the secure frame",
            style = CustomTypography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.sp
            ),
            color = TextStrong,
            textAlign = TextAlign.Center
        )
        Text(
            text = "CrossDroid scans locally and starts the transfer after one verified detection.",
            style = CustomTypography.labelMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.Tiny)
        )
    }
}

@Composable
private fun ScannerSurface(
    hasCameraPermission: Boolean,
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onGrantCamera: () -> Unit,
    onQrDetected: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scannerHeight = maxHeight.coerceAtMost(430.dp)
        val cornerRadius = 22.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scannerHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .neonGlow(AccentCyan, borderRadius = cornerRadius, glowRadius = 18.dp, opacity = 0.12f)
                .background(BgPanel)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            AccentCyan.copy(alpha = 0.65f),
                            NeonPrimary.copy(alpha = 0.45f),
                            BorderSubtle
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    torchEnabled = torchEnabled,
                    onTorchChange = onTorchChange,
                    onQrDetected = onQrDetected,
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay(modifier = Modifier.fillMaxSize())
            } else {
                CameraPermissionPanel(
                    onGrantCamera = onGrantCamera,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.Large)
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onQrDetected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = rememberBarcodeScanner()
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(cameraController, scanner, lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        cameraController.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(
                listOf(scanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                executor
            ) { result ->
                val barcodes = result?.getValue(scanner).orEmpty()
                if (barcodes.any { it.rawValue?.isNotBlank() == true }) {
                    onQrDetected()
                }
            }
        )
        cameraController.bindToLifecycle(lifecycleOwner)

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            scanner.close()
        }
    }

    LaunchedEffect(torchEnabled) {
        runCatching {
            cameraController.enableTorch(torchEnabled)
        }.onFailure {
            onTorchChange(false)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController
            }
        }
    )
}

@Composable
private fun rememberBarcodeScanner(): BarcodeScanner {
    return remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScannerOverlayTransition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScannerLaserPosition"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScannerGlowPulse"
    )

    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.72f
        val frameHeight = frameWidth.coerceAtMost(size.height * 0.62f)
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val right = left + frameWidth
        val bottom = top + frameHeight
        val corner = 34.dp.toPx()
        val stroke = 3.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.42f))
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, BgMain.copy(alpha = 0.42f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.62f
            )
        )

        for (i in 1..4) {
            val x = left + frameWidth * i / 5f
            drawLine(
                color = AccentCyan.copy(alpha = 0.10f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 1.dp.toPx()
            )
            val y = top + frameHeight * i / 5f
            drawLine(
                color = AccentCyan.copy(alpha = 0.10f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawRoundRect(
            color = AccentCyan.copy(alpha = 0.18f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
            style = Stroke(width = 1.dp.toPx())
        )

        val bracketColor = AccentCyan.copy(alpha = glowPulse)
        listOf(
            Offset(left, top) to Offset(left + corner, top),
            Offset(left, top) to Offset(left, top + corner),
            Offset(right, top) to Offset(right - corner, top),
            Offset(right, top) to Offset(right, top + corner),
            Offset(left, bottom) to Offset(left + corner, bottom),
            Offset(left, bottom) to Offset(left, bottom - corner),
            Offset(right, bottom) to Offset(right - corner, bottom),
            Offset(right, bottom) to Offset(right, bottom - corner)
        ).forEach { (start, end) ->
            drawLine(bracketColor, start, end, stroke)
        }

        val laserY = top + frameHeight * laserPosition
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, NeonHighlight, AccentCyan, Color.Transparent)
            ),
            start = Offset(left, laserY),
            end = Offset(right, laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun CameraPermissionPanel(
    onGrantCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AccentCyan.copy(alpha = 0.12f))
                .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.Large))
        Text(
            text = "Camera access required",
            style = CustomTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextStrong,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Allow camera access to scan the receiver QR code. Manual PIN and radar discovery remain available.",
            style = CustomTypography.bodyMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.Small)
        )
        Spacer(modifier = Modifier.height(Spacing.Large))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(NeonPrimary, AccentCyan)))
                .clickable(onClick = onGrantCamera)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(IconSize.Small)
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = "ENABLE CAMERA",
                style = CustomTypography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun PairingActionBar(
    hasCameraPermission: Boolean,
    torchEnabled: Boolean,
    onTorchClick: () -> Unit,
    onEnterCode: () -> Unit,
    onRadar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet))
            .background(BgElevated)
            .border(
                1.dp,
                BorderSubtle,
                RoundedCornerShape(topStart = Radii.OverlaySheet, topEnd = Radii.OverlaySheet)
            )
            .padding(Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScannerPillAction(
            label = "Code",
            icon = Icons.Default.Dialpad,
            accent = NeonHighlight,
            onClick = onEnterCode,
            modifier = Modifier.weight(1f)
        )
        ScannerPillAction(
            label = "Radar",
            icon = Icons.Default.Radar,
            accent = AccentCyan,
            onClick = onRadar,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            enabled = hasCameraPermission,
            onClick = onTorchClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (torchEnabled) AccentCyan.copy(alpha = 0.18f) else BgPanelMuted)
                .border(
                    1.dp,
                    if (torchEnabled) AccentCyan.copy(alpha = 0.65f) else BorderSubtle,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle flash",
                tint = if (hasCameraPermission) AccentCyan else TextMuted,
                modifier = Modifier.size(IconSize.Small)
            )
        }
    }
}

@Composable
private fun ScannerPillAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(BgPanelMuted)
            .border(1.dp, accent.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(IconSize.Small)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = label.uppercase(),
            style = CustomTypography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = TextStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
