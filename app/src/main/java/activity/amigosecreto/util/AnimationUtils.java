package activity.amigosecreto.util;

import android.app.Activity;
import activity.amigosecreto.R;

/**
 * Utility class for applying consistent animations across the app
 */
public class AnimationUtils {

    /**
     * Apply slide transition when starting a new activity
     * @param activity The current activity
     */
    public static void applySlideTransition(Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Apply slide transition when finishing/going back
     * @param activity The current activity
     */
    public static void applySlideBackTransition(Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Apply fade transition
     * @param activity The current activity
     */
    public static void applyFadeTransition(Activity activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}