package activity.amigosecreto.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for providing haptic feedback on user interactions
 */
object HapticFeedbackUtils {

    /** Light haptic feedback for normal button presses */
    @JvmStatic
    fun performLightFeedback(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            0 /* respect user accessibility settings */
        )
    }

    /** Medium haptic feedback for important actions */
    @JvmStatic
    fun performMediumFeedback(view: View?) {
        if (view == null) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            HapticFeedbackConstants.CONFIRM
        else
            HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(constant, 0 /* respect user accessibility settings */)
    }

    /** Strong haptic feedback for critical actions or errors */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun performStrongFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    /** Success feedback with pattern (short-short-long) */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun performSuccessFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 50, 50, 50, 50, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    /** Error feedback with pattern (long vibration) */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun performErrorFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 100, 50, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    /** Celebration feedback for successful draw/sortear */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun performCelebrationFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 50, 50, 50, 50, 50, 50, 100, 50, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }
}
