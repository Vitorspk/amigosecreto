package activity.amigosecreto.util

import android.app.Activity
import activity.amigosecreto.R

// overridePendingTransition deprecated in API 34 (use overrideActivityTransition).
// TODO: migrate to overrideActivityTransition() in Fase 10e (Activities migration).

/**
 * Utility class for applying consistent animations across the app
 */
object AnimationUtils {

    /** Apply slide transition when starting a new activity */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun applySlideTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Apply slide transition when finishing/going back */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun applySlideBackTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Apply fade transition */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun applyFadeTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
