package com.starborn.mantracounter.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * A second and a half when a mala closes: a firm strike, then a steady hum underneath it — the
 * physical mala equivalent of feeling the guru bead under your thumb, long enough to notice
 * without looking up, short enough not to intrude. Devices without amplitude control simply
 * vibrate at full strength for the same segments.
 */
fun Context.vibrateMalaComplete() {
    val vibrator = vibrator() ?: return
    if (!vibrator.hasVibrator()) return
    val effect = VibrationEffect.createWaveform(
        //  wait  strike  gap  sustain          = 1,500ms of vibration
        longArrayOf(0, 260, 70, 1_170),
        intArrayOf(0, 255, 0, 165),
        -1,
    )
    runCatching { vibrator.vibrate(effect) }
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
