package com.ioristudios.crossdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.components.PinDisplayCard
import com.ioristudios.crossdroid.ui.components.TopAppBar
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.CustomTypography
import com.ioristudios.crossdroid.ui.theme.Spacing
import com.ioristudios.crossdroid.ui.theme.TextSecondary
import com.ioristudios.crossdroid.ui.theme.TextStrong

@Composable
fun EnterCodeScreen(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinCode by viewModel.pinCode.collectAsState()
    val pinError by viewModel.pinError.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopAppBar(
            title = "Enter Pairing PIN",
            viewModel = viewModel,
            showBackButton = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Enter Receiver PIN",
                style = CustomTypography.headlineMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = TextStrong,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Get the 4-digit security code from the receiving device.",
                style = CustomTypography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = Spacing.Small)
            )

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
