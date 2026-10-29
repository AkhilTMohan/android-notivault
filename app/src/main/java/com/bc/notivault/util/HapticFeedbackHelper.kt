package com.bc.notivault.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Utility helper providing tactile, responsive haptic feedbacks
 * across user interactions (clicks, tab selections, long-presses, toggles, actions).
 */
class HapticHelper(
    private val view: View,
    private val composeHaptic: HapticFeedback
) {
    fun longPress() {
        try {
            view.isHapticFeedbackEnabled = true
            val success = view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!success) {
                composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun click() {
        try {
            view.isHapticFeedbackEnabled = true
            val success = view.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!success) {
                composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun tick() {
        try {
            view.isHapticFeedbackEnabled = true
            view.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun confirm() {
        try {
            view.isHapticFeedbackEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(
                    HapticFeedbackConstants.CONFIRM,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            } else {
                view.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun reject() {
        try {
            view.isHapticFeedbackEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(
                    HapticFeedbackConstants.REJECT,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            } else {
                view.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberHaptic(): HapticHelper {
    val view = LocalView.current
    val composeHaptic = LocalHapticFeedback.current
    return remember(view, composeHaptic) { HapticHelper(view, composeHaptic) }
}
