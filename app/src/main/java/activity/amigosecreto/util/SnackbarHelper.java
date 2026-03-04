package activity.amigosecreto.util;

import android.view.View;
import androidx.core.content.ContextCompat;
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
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.colorSecondary));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    /**
     * Show error message
     */
    public static void showError(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.colorError));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    /**
     * Show info message
     */
    public static void showInfo(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.colorPrimary));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    /**
     * Show message with action
     */
    public static void showWithAction(View view, String message, String actionText, View.OnClickListener action) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionText, action);
        snackbar.setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
        snackbar.show();
    }
}
