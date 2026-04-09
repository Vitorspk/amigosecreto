package activity.amigosecreto.util

import android.app.Activity
import android.os.Build
import activity.amigosecreto.R

/**
 * Utility class for applying consistent animations across the app.
 * Uses [Activity.overrideActivityTransition] on API 34+ and falls back to the
 * deprecated [Activity.overridePendingTransition] on older versions.
 */
object AnimationUtils {

    /** Apply slide transition when starting a new activity */
    @JvmStatic
    fun applySlideTransition(activity: Activity) {
        overrideTransition(activity, R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Apply slide transition when finishing/going back */
    @JvmStatic
    fun applySlideBackTransition(activity: Activity) {
        overrideTransition(activity, R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Apply fade transition */
    @JvmStatic
    fun applyFadeTransition(activity: Activity) {
        overrideTransition(activity, R.anim.fade_in, R.anim.fade_out)
    }

    @Suppress("DEPRECATION")
    private fun overrideTransition(activity: Activity, enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
        } else {
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }
}
