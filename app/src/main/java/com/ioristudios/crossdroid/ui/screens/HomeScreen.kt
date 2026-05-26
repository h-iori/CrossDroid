package com.ioristudios.crossdroid.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
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
    
    var sendTrigger by remember { mutableStateOf(false) }
    var receiveTrigger by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        sendTrigger = true
        kotlinx.coroutines.delay(90L)
        receiveTrigger = true
    }

    val sendAlpha by animateFloatAsState(
        targetValue = if (sendTrigger) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "sendAlpha"
    )
    val sendOffsetY by animateFloatAsState(
        targetValue = if (sendTrigger) 0f else 30f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f,
            stiffness = 250f
        ),
        label = "sendOffsetY"
    )

    val receiveAlpha by animateFloatAsState(
        targetValue = if (receiveTrigger) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "receiveAlpha"
    )
    val receiveOffsetY by animateFloatAsState(
        targetValue = if (receiveTrigger) 0f else 30f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f,
            stiffness = 250f
        ),
        label = "receiveOffsetY"
    )

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
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = sendAlpha
                        translationY = sendOffsetY
                    }
            ) {
                EnterpriseActionPanel(
                    title = "Send",
                    description = "Select files and transfer them instantly to nearby devices.",
                    icon = Icons.Default.FileUpload,
                    palette = ActionPalette(
                        accent = NeonPrimary,
                        wash = Color(0xFFE46CFF),
                        edge = NeonHighlight,
                        iconTint = Color(0xFFF3D7FF)
                    ),
                    onClick = { viewModel.navigateTo(Screen.SEND, context) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = receiveAlpha
                        translationY = receiveOffsetY
                    }
            ) {
                EnterpriseActionPanel(
                    title = "Receive",
                    description = "Make this device discoverable and accept incoming files.",
                    icon = Icons.Default.FileDownload,
                    palette = ActionPalette(
                        accent = AccentCyan,
                        wash = Color(0xFF8B5CFF),
                        edge = Color(0xFF8AF7FF),
                        iconTint = Color(0xFFD9FDFF)
                    ),
                    onClick = { viewModel.navigateTo(Screen.RECEIVE, context) }
                )
            }
        }
    }
}

private data class ActionPalette(
    val accent: Color,
    val wash: Color,
    val edge: Color,
    val iconTint: Color
)

@Composable
private fun EnterpriseActionPanel(
    title: String,
    description: String,
    icon: ImageVector,
    palette: ActionPalette,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = 300f
        ),
        label = "EnterpriseActionPanelScale"
    )
    val washAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.24f else 0.16f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "EnterpriseActionPanelWash"
    )
    val cornerRadius = 20.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .neonGlow(
                color = palette.accent,
                borderRadius = cornerRadius,
                glowRadius = 18.dp,
                opacity = if (isPressed) 0.24f else 0.16f
            )
            .background(BgPanel)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.18f),
                        BgPanel,
                        palette.wash.copy(alpha = washAlpha)
                    )
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.edge.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    radius = 520f
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.edge.copy(alpha = 0.76f),
                        palette.accent.copy(alpha = 0.34f),
                        BorderSubtle
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.30f),
                                BgPanelMuted.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = palette.edge.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.iconTint,
                    modifier = Modifier.size(IconSize.Large)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CustomTypography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        letterSpacing = 0.sp
                    ),
                    color = TextStrong
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = description,
                    style = CustomTypography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        letterSpacing = 0.sp
                    ),
                    color = TextSecondary.copy(alpha = 0.94f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent.copy(alpha = 0.13f))
                    .border(
                        width = 1.dp,
                        color = palette.edge.copy(alpha = 0.34f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = palette.edge.copy(alpha = 0.92f),
                    modifier = Modifier.size(IconSize.Small)
                )
            }
        }
    }
}
