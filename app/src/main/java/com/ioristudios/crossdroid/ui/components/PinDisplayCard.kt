package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.theme.AccentCyan
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgPanelMuted
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.BorderSubtle
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
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
fun PinDisplayCard(
    pinCode: String,
    errorMessage: String?,
    onKeyTap: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxLen = 4
    val canConfirm = pinCode.length == maxLen
    val panelScale by animateFloatAsState(
        targetValue = if (errorMessage != null) 0.985f else 1f,
        label = "PinPanelErrorPulse"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val keySize = ((maxWidth - 112.dp) / 3).coerceIn(52.dp, 68.dp)
        val pinBoxSize = ((maxWidth - 104.dp) / 4).coerceIn(48.dp, 62.dp)

        Column(
            modifier = Modifier
                .scale(panelScale)
                .clip(RoundedCornerShape(22.dp))
                .neonGlow(
                    color = if (errorMessage == null) NeonPrimary else ColorError,
                    borderRadius = 22.dp,
                    glowRadius = 18.dp,
                    opacity = if (errorMessage == null) 0.12f else 0.18f
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            BgElevated.copy(alpha = 0.98f),
                            BgSurface.copy(alpha = 0.86f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            if (errorMessage == null) NeonPrimary.copy(alpha = 0.45f) else ColorError.copy(alpha = 0.7f),
                            BorderSubtle
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SecurityStatusRow()

            Spacer(modifier = Modifier.height(Spacing.Medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (index in 0 until maxLen) {
                    val hasChar = index < pinCode.length
                    val isFocused = index == pinCode.length && errorMessage == null
                    val borderColor = when {
                        errorMessage != null -> ColorError
                        isFocused -> AccentCyan
                        hasChar -> NeonHighlight
                        else -> BorderSubtle
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.Tiny)
                            .size(pinBoxSize)
                            .clip(RoundedCornerShape(15.dp))
                            .background(BgPanelMuted)
                            .border(1.5.dp, borderColor, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasChar) pinCode[index].toString() else "",
                            color = TextStrong,
                            style = CustomTypography.headlineMedium.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = ColorError,
                    style = CustomTypography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.Medium)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('B', '0', 'C')
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        rowKeys.forEach { key ->
                            PinKey(
                                key = key,
                                keySize = keySize,
                                confirmEnabled = canConfirm,
                                onKeyTap = onKeyTap,
                                onBackspace = onBackspace,
                                onConfirm = onConfirm
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                .background(ColorSuccess)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = "ENCRYPTED LOCAL PAIRING",
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            ),
            color = AccentCyan
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "RECEIVER PIN",
            style = CustomTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            ),
            color = TextMuted
        )
    }
}

@Composable
private fun PinKey(
    key: Char,
    keySize: androidx.compose.ui.unit.Dp,
    confirmEnabled: Boolean,
    onKeyTap: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val isConfirm = key == 'C'
    val isBackspace = key == 'B'
    val enabled = !isConfirm || confirmEnabled
    val accent = when {
        isConfirm -> ColorSuccess
        isBackspace -> ColorError
        else -> AccentCyan
    }

    val background = when {
        isConfirm && enabled -> Brush.linearGradient(listOf(NeonPrimary.copy(alpha = 0.88f), AccentCyan.copy(alpha = 0.82f)))
        isConfirm -> Brush.linearGradient(listOf(BgPanelMuted, BgPanelMuted))
        isBackspace -> Brush.linearGradient(listOf(ColorError.copy(alpha = 0.12f), BgPanelMuted))
        else -> Brush.linearGradient(listOf(BgPanelMuted, BgSurface))
    }

    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.Small)
            .size(keySize)
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (enabled) accent.copy(alpha = 0.45f) else BorderSubtle,
                shape = CircleShape
            )
            .clickable(enabled = enabled) {
                when (key) {
                    'B' -> onBackspace()
                    'C' -> {
                        HapticHelper.triggerMedium(context)
                        onConfirm()
                    }
                    else -> onKeyTap(key)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            'B' -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = ColorError,
                modifier = Modifier.size(IconSize.Small)
            )
            'C' -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Confirm PIN",
                tint = if (enabled) Color.White else TextSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(IconSize.Standard)
            )
            else -> Text(
                text = key.toString(),
                color = TextStrong,
                style = CustomTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}
