package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.PinDisplayCard
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
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong

@Composable
fun EnterCodeScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pinCode by viewModel.pinCode.collectAsState()
    val pinError by viewModel.pinError.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Manual Pairing",
            viewModel = viewModel,
            showBackButton = true,
            subtitle = "Receiver PIN challenge"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PairingCodeHeader()

            Spacer(modifier = Modifier.height(Spacing.Large))

            PinDisplayCard(
                pinCode = pinCode,
                errorMessage = pinError,
                onKeyTap = { viewModel.appendPinChar(it, context) },
                onBackspace = { viewModel.deletePinLast(context) },
                onConfirm = { viewModel.verifyPinCode(context) }
            )
        }
    }
}

@Composable
private fun PairingCodeHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            NeonPrimary.copy(alpha = 0.28f),
                            AccentCyan.copy(alpha = 0.16f),
                            BgPanel
                        )
                    )
                )
                .border(1.dp, NeonHighlight.copy(alpha = 0.42f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = NeonHighlight,
                modifier = Modifier.size(IconSize.Large)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Text(
            text = "Enter receiver PIN",
            style = CustomTypography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.sp
            ),
            color = TextStrong,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Use the 4-digit code shown on the receiving device to authorize this transfer.",
            style = CustomTypography.bodyMedium.copy(letterSpacing = 0.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.Small)
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(BgPanelMuted)
                .border(1.dp, BorderSubtle, CircleShape)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(IconSize.Small)
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = "4 DIGITS REQUIRED",
                style = CustomTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                ),
                color = TextSecondary
            )
        }
    }
}
