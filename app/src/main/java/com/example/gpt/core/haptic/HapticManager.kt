package com.example.gpt.core.haptic

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var isEnabled = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun performUIFeedback() {
        if (!isEnabled) return
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()
                    vibrator.vibrate(effect, attributes)
                } else {
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (e: Exception) {
            Log.e("HapticManager", "UI Feedback failed", e)
        }
    }

    private fun vibrateMetric(milliseconds: Long, amplitude: Int) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (!isEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(milliseconds, amplitude)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_MEDIA)
                        .build()
                    vibrator.vibrate(effect, attributes)
                } else {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    vibrator.vibrate(effect, attributes)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            Log.e("HapticManager", "Metric vibration failed", e)
        }
    }

    fun performTestVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) return
        vibrateMetric(100, 255)
    }

    fun syncBeat() {
        vibrateMetric(60, 255)
    }

    fun accentBeat() {
        vibrateMetric(50, 255)
    }

    fun normalBeat() {
        vibrateMetric(20, 150)
    }

    fun lightTap() {
        vibrateMetric(10, 80)
    }

    fun mediumTap() {
        vibrateMetric(25, 120)
    }

    fun successPattern() {
        if (!isEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timing = longArrayOf(0, 50, 50, 50, 50, 100)
            val amplitudes = intArrayOf(0, 100, 0, 150, 0, 255)
            try {
                val effect = VibrationEffect.createWaveform(timing, amplitudes, -1)
                vibrator?.vibrate(effect)
            } catch (e: Exception) {
                performUIFeedback()
            }
        } else {
            performUIFeedback()
        }
    }
}

@Composable
fun rememberHapticFeedback(): (HapticType) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { type: HapticType ->
            when (type) {
                HapticType.LIGHT -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                HapticType.MEDIUM -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                HapticType.HEAVY -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}

enum class HapticType {
    LIGHT, MEDIUM, HEAVY
}