package activity.amigosecreto.util;

import android.view.View;
import com.google.android.material.snackbar.Snackbar;
import activity.amigosecreto.R;

/**
 * Helper class for creating consistent Snackbar messages
 */
public class SnackbarHelper {

    /**
     * Show success message
     */
    public static void showSuccess(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(view.getContext().getColor(R.color.colorSecondary));
        snackbar.setTextColor(view.getContext().getColor(R.color.white));
        snackbar.show();
    }

    /**
     * Show error message
     */
    public static void showError(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(view.getContext().getColor(R.color.colorError));
        snackbar.setTextColor(view.getContext().getColor(R.color.white));
        snackbar.show();
    }

    /**
     * Show info message
     */
    public static void showInfo(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(view.getContext().getColor(R.color.colorPrimary));
        snackbar.setTextColor(view.getContext().getColor(R.color.white));
        snackbar.show();
    }

    /**
     * Show message with action
     */
    public static void showWithAction(View view, String message, String actionText, View.OnClickListener action) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionText, action);
        snackbar.setActionTextColor(view.getContext().getColor(R.color.colorAccent));
        snackbar.show();
    }
}