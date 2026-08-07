package com.starborn.mantracounter.util

import android.view.HapticFeedbackConstants
import android.view.View

/** A light tick for every bead. */
fun View.hapticTick() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

/** A heavier pulse for landmarks — mala completed, target reached. */
fun View.hapticThud() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
