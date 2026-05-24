package com.ioristudios.crossdroid.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPrimary,
    secondary = NeonGlow,
    tertiary = NeonHighlight,
    background = BgMain,
    surface = BgElevated,
    onBackground = TextBody,
    onSurface = TextBody,
    error = ColorError,
    onError = TextStrong
)

@Composable
fun CrossDroidTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgMain.toArgb()
            window.navigationBarColor = BgMain.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CustomTypography,
        content = content
    )
}

/**
 * Haptic Helper providing cyberpunk tactile feedback intensity presets.
 */
object HapticHelper {
    fun triggerLight(context: Context) {
        vibrate(context, 20)
    }

    fun triggerMedium(context: Context) {
        vibrate(context, 50)
    }

    fun triggerStrong(context: Context) {
        vibrate(context, 100)
    }

    fun triggerSuccess(context: Context) {
        // Pulsing success: buzz, pause, double buzz
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 60, 80),
                        intArrayOf(0, 180, 0, 255),
                        -1
                    )
                )
            } else {
                vibrator.vibrate(150)
            }
        } catch (e: Exception) {
            vibrate(context, 100)
        }
    }

    fun triggerError(context: Context) {
        // Rapid triple warning pulse
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 60, 40, 60, 40, 100),
                        intArrayOf(0, 255, 0, 255, 0, 255),
                        -1
                    )
                )
            } else {
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            vibrate(context, 200)
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
