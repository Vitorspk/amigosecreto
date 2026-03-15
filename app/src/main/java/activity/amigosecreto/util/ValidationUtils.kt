package activity.amigosecreto.util

import android.widget.EditText
import java.util.regex.Pattern

/**
 * Utility class for input validation with friendly error messages
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
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) {
            editText.error = errorMessage
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validateName(editText: EditText): Boolean {
        val name = editText.text.toString().trim()
        return when {
            name.isEmpty() -> {
                editText.error = "Nome é obrigatório"
                editText.requestFocus()
                false
            }
            name.length < 2 -> {
                editText.error = "Nome deve ter pelo menos 2 caracteres"
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
    fun validateEmail(editText: EditText): Boolean {
        val email = editText.text.toString().trim()
        if (email.isEmpty()) return true // Email is optional
        return if (!EMAIL_PATTERN.matcher(email).matches()) {
            editText.error = "Email inválido"
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validatePhone(editText: EditText): Boolean {
        val phone = editText.text.toString().trim()
        if (phone.isEmpty()) return true // Phone is optional
        return if (!PHONE_PATTERN.matcher(phone).matches()) {
            editText.error = "Telefone inválido (ex: (11) 91234-5678)"
            editText.requestFocus()
            false
        } else {
            editText.error = null
            true
        }
    }

    @JvmStatic
    fun validateMinParticipants(count: Int, minimum: Int): String? {
        return if (count < minimum)
            "É necessário pelo menos $minimum participantes para realizar o sorteio"
        else null
    }

    @JvmStatic
    fun getDatabaseErrorMessage(e: Exception): String {
        return when {
            e.message?.contains("UNIQUE") == true -> "Este item já existe no banco de dados"
            e.message?.contains("NOT NULL") == true -> "Alguns campos obrigatórios estão vazios"
            e.message?.contains("FOREIGN KEY") == true -> "Não é possível excluir. Item está sendo usado"
            else -> "Erro ao acessar o banco de dados. Tente novamente"
        }
    }

    @JvmStatic
    fun getNetworkErrorMessage(e: Exception): String {
        return when {
            e.message?.contains("timeout") == true -> "Tempo de conexão esgotado. Verifique sua internet"
            e.message?.contains("Unable to resolve host") == true -> "Sem conexão com a internet"
            else -> "Erro de rede. Verifique sua conexão"
        }
    }
}
