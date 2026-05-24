package com.ioristudios.crossdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.theme.BgElevated
import com.ioristudios.crossdroid.ui.theme.BgSurface
import com.ioristudios.crossdroid.ui.theme.ColorError
import com.ioristudios.crossdroid.ui.theme.ColorSuccess
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import com.ioristudios.crossdroid.ui.theme.NeonHighlight
import com.ioristudios.crossdroid.ui.theme.NeonPrimary
import com.ioristudios.crossdroid.ui.theme.Radii
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextBody
import com.ioristudios.crossdroid.ui.theme.TextMuted
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

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val keySize = if (maxHeight < 400.dp) 48.dp else if (maxHeight < 500.dp) 54.dp else 64.dp
        val pinBoxSize = if (maxHeight < 400.dp) 42.dp else if (maxHeight < 500.dp) 48.dp else 54.dp
        val spacerHeight = if (maxHeight < 450.dp) Spacing.Small else Spacing.Medium

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PIN Display slots (4 boxes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until maxLen) {
                    val hasChar = i < pinCode.length
                    val char = if (hasChar) pinCode[i].toString() else ""
                    val isFocused = i == pinCode.length && errorMessage == null

                    val boxBorderColor = when {
                        errorMessage != null -> ColorError
                        isFocused -> NeonHighlight
                        hasChar -> NeonPrimary
                        else -> BgSurface
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.Small)
                            .size(pinBoxSize)
                            .then(
                                if (isFocused) {
                                    Modifier.neonGlow(
                                        color = NeonPrimary,
                                        borderRadius = Radii.ButtonSmall,
                                        glowRadius = 8.dp,
                                        opacity = 0.25f
                                    )
                                } else Modifier
                            )
                            .background(BgElevated)
                            .border(
                                width = 1.5.dp,
                                color = boxBorderColor,
                                shape = RoundedCornerShape(Radii.ButtonSmall)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = TextStrong,
                            style = CustomTypography.headlineMedium.copy(
                                fontSize = if (pinBoxSize < 48.dp) 18.sp else 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            // Error message text block
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ColorError,
                    style = CustomTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large)
                )
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            // Keyboard grid (10 buttons + backspace + confirm)
            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('B', '0', 'C') // B = Backspace, C = Confirm
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
                            val isSpecial = key == 'B' || key == 'C'
                            val keyBg = if (isSpecial) {
                                if (key == 'C') NeonPrimary.copy(alpha = 0.15f) else ColorError.copy(alpha = 0.1f)
                            } else {
                                BgElevated
                            }

                            val keyBorder = if (isSpecial) {
                                if (key == 'C') NeonPrimary else ColorError
                            } else {
                                BgSurface
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.Small)
                                    .size(keySize)
                                    .clip(CircleShape)
                                    .background(keyBg)
                                    .border(width = 1.dp, color = keyBorder, shape = CircleShape)
                                    .clickable {
                                        when (key) {
                                            'B' -> onBackspace()
                                            'C' -> onConfirm()
                                            else -> onKeyTap(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    'B' -> {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = ColorError,
                                            modifier = Modifier.size(if (keySize < 54.dp) 16.dp else 20.dp)
                                        )
                                    }
                                    'C' -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = ColorSuccess,
                                            modifier = Modifier.size(if (keySize < 54.dp) 18.dp else 22.dp)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = key.toString(),
                                            color = TextStrong,
                                            style = CustomTypography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (keySize < 54.dp) 16.sp else 20.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
