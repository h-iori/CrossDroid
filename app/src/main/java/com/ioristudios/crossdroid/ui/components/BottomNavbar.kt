package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ioristudios.crossdroid.R
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.BgPanel
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.IconSize
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextMuted
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong
import com.ioristudios.crossdroid.ui.theme.neonGlow

@Composable
fun BottomNavbar(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val pulseTransition = rememberInfiniteTransition(label = "BottomNavPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BottomNavGlowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.Medium),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(24.dp))
                .neonGlow(
                    color = NeonPrimary,
                    borderRadius = 24.dp,
                    glowRadius = 22.dp,
                    opacity = pulseAlpha,
                    offsetY = 4.dp
                )
                .background(BgPanel.copy(alpha = 0.98f))
                .border(1.dp, NeonPrimary.copy(alpha = 0.26f + (pulseAlpha * 0.34f)), RoundedCornerShape(24.dp))
                .padding(horizontal = Spacing.Small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab(
                label = "Devices",
                selected = currentScreen == Screen.DEVICES,
                pulseAlpha = pulseAlpha,
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                },
                onClick = { viewModel.navigateTo(Screen.DEVICES, context) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(0.92f))

            BottomNavTab(
                label = "History",
                selected = currentScreen == Screen.HISTORY,
                pulseAlpha = pulseAlpha,
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(IconSize.Standard)
                    )
                },
                onClick = { viewModel.navigateTo(Screen.HISTORY, context) },
                modifier = Modifier.weight(1f)
            )
        }

        val isHome = currentScreen == Screen.HOME
        val logoSize by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isHome) 72.dp else 68.dp,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "logoSizeAnim"
        )
        val logoGlowRadius by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isHome) 24.dp else 16.dp,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            ),
            label = "logoGlowRadius"
        )
        val logoInnerSize by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isHome) 70.dp else 66.dp,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "logoInnerSizeAnim"
        )

        Box(
            modifier = Modifier
                .offset(y = (-12).dp)
                .size(logoSize)
                .then(
                    Modifier.neonGlow(
                        color = NeonPrimary,
                        borderRadius = 100.dp,
                        glowRadius = logoGlowRadius,
                        opacity = if (isHome) pulseAlpha + 0.12f else pulseAlpha * 0.55f
                    )
                )
                .clip(CircleShape)
                .clickable {
                    HapticHelper.triggerMedium(context)
                    viewModel.navigateTo(Screen.HOME, context)
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.transfer_logo),
                contentDescription = "Home",
                modifier = Modifier
                    .size(logoInnerSize)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun BottomNavTab(
    label: String,
    selected: Boolean,
    pulseAlpha: Float,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "tabSelectedProgress"
    )

    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.18f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "iconScaleAnim"
    )

    val labelColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) TextStrong else TextSecondary,
        animationSpec = tween(200),
        label = "labelColorAnim"
    )

    val tint = if (selected) NeonHighlight else TextMuted

    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Tiny),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 30.dp)
                .neonGlow(
                    color = NeonPrimary,
                    borderRadius = 15.dp,
                    glowRadius = 8.dp,
                    opacity = pulseAlpha * 0.45f * selectedProgress
                )
                .background(
                    color = NeonPrimary.copy(alpha = (0.12f + (pulseAlpha * 0.08f)) * selectedProgress),
                    shape = RoundedCornerShape(15.dp)
                )
                .border(
                    width = 1.dp,
                    color = NeonPrimary.copy(alpha = (0.25f + (pulseAlpha * 0.15f)) * selectedProgress),
                    shape = RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.scale(iconScale)) {
                icon(tint)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            color = labelColor,
            style = CustomTypography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}
