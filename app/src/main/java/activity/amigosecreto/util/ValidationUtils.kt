package activity.amigosecreto.util

import android.content.Context
import android.widget.EditText
import activity.amigosecreto.R
import java.util.regex.Pattern

/**
 * Utility class for input validation with friendly error messages.
 * Methods that set EditText errors require a [Context] to load strings from resources.
 */
object ValidationUtils {

    // Email pattern
    private val EMAIL_PATTERN: Pattern = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"
    )

    // Phone pattern (Brazilian format, with optional +55 country code from ContactPicker)
    private val PHONE_PATTERN: Pattern = Pattern.compile(
        "^(\\+55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$"
    )

    @JvmStatic
    fun isNotEmpty(text: String?): Boolean = !text.isNullOrBlank()

    @JvmStatic
    fun validateNotEmpty(editText: EditText, errorMessage: String): Boolean {
        val text = editText.text.toString()
        return if (text.isBlank()) {
            editText.error = errorMessage
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validateName(context: Context, editText: EditText): Boolean {
        val name = editText.text.toString().trim()
        return when {
            name.isEmpty() -> {
                editText.error = context.getString(R.string.validation_name_required)
                editText.requestFocus()
                false
            }
            name.length < 2 -> {
                editText.error = context.getString(R.string.validation_name_min_length)
                editText.requestFocus()
                false
            }
            else -> {
                editText.error = null
                true
            }
        }
    }

    @JvmStatic
    fun validateEmail(context: Context, editText: EditText): Boolean {
        val email = editText.text.toString().trim()
        if (email.isEmpty()) return true // Email is optional
        return if (!EMAIL_PATTERN.matcher(email).matches()) {
            editText.error = context.getString(R.string.validation_email_invalid)
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validatePhone(context: Context, editText: EditText): Boolean {
        val phone = editText.text.toString().trim()
        if (phone.isEmpty()) return true // Phone is optional
        return if (!PHONE_PATTERN.matcher(phone).matches()) {
            editText.error = context.getString(R.string.validation_phone_invalid)
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validateMinParticipants(context: Context, count: Int, minimum: Int): String? {
        return if (count < minimum)
            context.getString(R.string.validation_min_participants, minimum)
        else null
    }

    @JvmStatic
    fun getDatabaseErrorMessage(context: Context, e: Exception): String {
        return when {
            e.message?.contains("UNIQUE") == true -> context.getString(R.string.validation_db_unique)
            e.message?.contains("NOT NULL") == true -> context.getString(R.string.validation_db_not_null)
            e.message?.contains("FOREIGN KEY") == true -> context.getString(R.string.validation_db_foreign_key)
            else -> context.getString(R.string.validation_db_generic)
        }
    }

    @JvmStatic
    fun getNetworkErrorMessage(context: Context, e: Exception): String {
        return when {
            e.message?.contains("timeout") == true -> context.getString(R.string.validation_network_timeout)
            e.message?.contains("Unable to resolve host") == true -> context.getString(R.string.validation_network_no_connection)
            else -> context.getString(R.string.validation_network_generic)
        }
    }
}
