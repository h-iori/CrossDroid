package com.ioristudios.crossdroid.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.screens.AboutScreen
import com.ioristudios.crossdroid.ui.screens.HomeScreen
import com.ioristudios.crossdroid.ui.screens.DevicesScreen
import com.ioristudios.crossdroid.ui.screens.HistoryScreen
import com.ioristudios.crossdroid.ui.screens.SendScreen
import com.ioristudios.crossdroid.ui.screens.QrScanScreen
import com.ioristudios.crossdroid.ui.screens.EnterCodeScreen
import com.ioristudios.crossdroid.ui.screens.RadarScreen
import com.ioristudios.crossdroid.ui.screens.ReceiveScreen
import com.ioristudios.crossdroid.ui.screens.TransferScreen

@Composable
fun NavigationHost(
    viewModel: CrossDroidViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen = viewModel.currentScreen.collectAsState().value

    AnimatedContent(
        targetState = currentScreen,
        modifier = modifier,
        transitionSpec = {
            // Check slide transition criteria
            val isEnteringSubflow = targetState in listOf(
                Screen.SEND, Screen.QR_SCAN, Screen.ENTER_CODE, Screen.RADAR, Screen.TRANSFER, Screen.RECEIVE
            )
            val isExitingSubflow = initialState in listOf(
                Screen.SEND, Screen.QR_SCAN, Screen.ENTER_CODE, Screen.RADAR, Screen.TRANSFER, Screen.RECEIVE
            )

            if (isEnteringSubflow && !isExitingSubflow) {
                // Entering from Main Tabs to subflow: Slide up/right & Fade
                (slideInHorizontally(animationSpec = tween(350)) { it / 2 } + fadeIn(tween(350)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it / 2 } + fadeOut(tween(350)))
            } else if (!isEnteringSubflow && isExitingSubflow) {
                // Back to main tabs: Slide back out
                (slideInHorizontally(animationSpec = tween(350)) { -it / 2 } + fadeIn(tween(350)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it / 2 } + fadeOut(tween(350)))
            } else if (isEnteringSubflow && isExitingSubflow) {
                // Moving between subflow screens (e.g. Scan -> Code -> Radar -> Transfer)
                if (targetState == Screen.TRANSFER) {
                    // Critical entry: scale zoom & fade
                    (scaleIn(initialScale = 0.85f, animationSpec = tween(380)) + fadeIn(tween(380)))
                        .togetherWith(scaleOut(targetScale = 1.15f, animationSpec = tween(380)) + fadeOut(tween(380)))
                } else {
                    (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(300)))
                }
            } else {
                // Crossfade between main screens (Home, Devices, History)
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
            }
        },
        label = "MainScreenNavigation"
    ) { targetStateVal ->
        when (targetStateVal) {
            Screen.HOME -> HomeScreen(viewModel = viewModel)
            Screen.DEVICES -> DevicesScreen(viewModel = viewModel)
            Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
            Screen.SEND -> SendScreen(viewModel = viewModel)
            Screen.QR_SCAN -> QrScanScreen(viewModel = viewModel)
            Screen.ENTER_CODE -> EnterCodeScreen(viewModel = viewModel)
            Screen.RADAR -> RadarScreen(viewModel = viewModel)
            Screen.RECEIVE -> ReceiveScreen(viewModel = viewModel)
            Screen.TRANSFER -> TransferScreen(viewModel = viewModel)
            Screen.ABOUT -> AboutScreen(onBack = { viewModel.navigateTo(Screen.HOME) })
        }
    }
}
