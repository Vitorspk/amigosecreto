package activity.amigosecreto.util

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import activity.amigosecreto.R

/**
 * Helper class for creating consistent Snackbar messages
 */
object SnackbarHelper {

    @JvmStatic
    fun showSuccess(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorSecondary))
            setTextColor(ContextCompat.getColor(view.context, R.color.white))
            show()
        }
    }

    @JvmStatic
    fun showError(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorError))
            setTextColor(ContextCompat.getColor(view.context, R.color.white))
            show()
        }
    }

    @JvmStatic
    fun showInfo(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorPrimary))
            setTextColor(ContextCompat.getColor(view.context, R.color.white))
            show()
        }
    }

    @JvmStatic
    fun showWithAction(view: View, message: String, actionText: String, action: View.OnClickListener) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).apply {
            setAction(actionText, action)
            setActionTextColor(ContextCompat.getColor(view.context, R.color.colorAccent))
            show()
        }
    }
}
