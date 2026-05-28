package com.ioristudios.crossdroid.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalContext
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.screens.AboutScreen
import com.ioristudios.crossdroid.ui.screens.HomeScreen
import com.ioristudios.crossdroid.ui.screens.DevicesScreen
import com.ioristudios.crossdroid.ui.screens.HistoryScreen
import com.ioristudios.crossdroid.ui.screens.HistoryDetailScreen
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
    val context = LocalContext.current

    AnimatedContent(
        targetState = currentScreen,
        modifier = modifier,
        transitionSpec = {
            // Check slide transition criteria
            val isEnteringSubflow = targetState in listOf(
                Screen.SEND, Screen.QR_SCAN, Screen.ENTER_CODE, Screen.RADAR, Screen.TRANSFER, Screen.RECEIVE, Screen.HISTORY_DETAIL, Screen.ABOUT
            )
            val isExitingSubflow = initialState in listOf(
                Screen.SEND, Screen.QR_SCAN, Screen.ENTER_CODE, Screen.RADAR, Screen.TRANSFER, Screen.RECEIVE, Screen.HISTORY_DETAIL, Screen.ABOUT
            )

            val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = 0.82f, // smooth, natural damping
                stiffness = 350f      // fast and responsive
            )
            val fadeSpec = spring<Float>(
                stiffness = 350f
            )
            val scaleSpec = spring<Float>(
                dampingRatio = 0.8f,
                stiffness = 300f
            )

            if (isEnteringSubflow && !isExitingSubflow) {
                // Entering from Main Tabs to subflow: Slide up/right & Fade
                (slideInHorizontally(animationSpec = slideSpec) { it / 2 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { -it / 2 } + fadeOut(fadeSpec))
            } else if (!isEnteringSubflow && isExitingSubflow) {
                // Back to main tabs: Slide back out
                (slideInHorizontally(animationSpec = slideSpec) { -it / 2 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { it / 2 } + fadeOut(fadeSpec))
            } else if (isEnteringSubflow && isExitingSubflow) {
                // Moving between subflow screens (e.g. Scan -> Code -> Radar -> Transfer)
                if (targetState == Screen.TRANSFER) {
                    // Critical entry: scale zoom & fade
                    (scaleIn(initialScale = 0.88f, animationSpec = scaleSpec) + fadeIn(fadeSpec))
                        .togetherWith(scaleOut(targetScale = 1.12f, animationSpec = scaleSpec) + fadeOut(fadeSpec))
                } else {
                    (slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(fadeSpec))
                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { -it } + fadeOut(fadeSpec))
                }
            } else {
                // Crossfade between main screens (Home, Devices, History)
                fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
            }
        },
        label = "MainScreenNavigation"
    ) { targetStateVal ->
        when (targetStateVal) {
            Screen.HOME -> HomeScreen(viewModel = viewModel)
            Screen.DEVICES -> DevicesScreen(viewModel = viewModel)
            Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
            Screen.HISTORY_DETAIL -> HistoryDetailScreen(viewModel = viewModel)
            Screen.SEND -> SendScreen(viewModel = viewModel)
            Screen.QR_SCAN -> QrScanScreen(viewModel = viewModel)
            Screen.ENTER_CODE -> EnterCodeScreen(viewModel = viewModel)
            Screen.RADAR -> RadarScreen(viewModel = viewModel)
            Screen.RECEIVE -> ReceiveScreen(viewModel = viewModel)
            Screen.TRANSFER -> TransferScreen(viewModel = viewModel)
            Screen.ABOUT -> AboutScreen(onBack = { viewModel.navigateBack(context) })
        }
    }
}
