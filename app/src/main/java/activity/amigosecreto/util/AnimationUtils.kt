package activity.amigosecreto.util

import android.app.Activity
import activity.amigosecreto.R

/**
 * Utility class for applying consistent animations across the app
 */
object AnimationUtils {

    /** Apply slide transition when starting a new activity */
    @JvmStatic
    fun applySlideTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Apply slide transition when finishing/going back */
    @JvmStatic
    fun applySlideBackTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Apply fade transition */
    @JvmStatic
    fun applyFadeTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
