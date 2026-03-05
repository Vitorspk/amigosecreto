package activity.amigosecreto.util;

import android.widget.EditText;
import java.util.regex.Pattern;

/**
 * Utility class for input validation with friendly error messages
 */
public class ValidationUtils {

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"
    );

    // Phone pattern (Brazilian format, with optional +55 country code from ContactPicker)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$"
    );

    /**
     * Validate if text is not empty
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validate EditText is not empty
     */
    public static boolean validateNotEmpty(EditText editText, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return false;
        }
        editText.setError(null);
        return true;
    }

    /**
     * Validate name (minimum 2 characters)
     */
    public static boolean validateName(EditText editText) {
        String name = editText.getText().toString().trim();
        if (name.isEmpty()) {
            editText.setError("Nome é obrigatório");
            editText.requestFocus();
            return false;
        }
        if (name.length() < 2) {
            editText.setError("Nome deve ter pelo menos 2 caracteres");
            editText.requestFocus();
            return false;
        }
        editText.setError(null);
        return true;
    }

    /**
     * Validate email format
     */
    public static boolean validateEmail(EditText editText) {
        String email = editText.getText().toString().trim();
        if (email.isEmpty()) {
            return true; // Email is optional
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            editText.setError("Email inválido");
            editText.requestFocus();
            return false;
        }
        editText.setError(null);
        return true;
    }

    /**
     * Validate phone format
     */
    public static boolean validatePhone(EditText editText) {
        String phone = editText.getText().toString().trim();
        if (phone.isEmpty()) {
            return true; // Phone is optional
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            editText.setError("Telefone inválido (ex: (11) 91234-5678)");
            editText.requestFocus();
            return false;
        }
        editText.setError(null);
        return true;
    }

    /**
     * Validate minimum number of participants
     */
    public static String validateMinParticipants(int count, int minimum) {
        if (count < minimum) {
            return "É necessário pelo menos " + minimum + " participantes para realizar o sorteio";
        }
        return null;
    }

    /**
     * Get friendly error message for database operations
     */
    public static String getDatabaseErrorMessage(Exception e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("UNIQUE")) {
                return "Este item já existe no banco de dados";
            }
            if (e.getMessage().contains("NOT NULL")) {
                return "Alguns campos obrigatórios estão vazios";
            }
            if (e.getMessage().contains("FOREIGN KEY")) {
                return "Não é possível excluir. Item está sendo usado";
            }
        }
        return "Erro ao acessar o banco de dados. Tente novamente";
    }

    /**
     * Get friendly error message for network operations
     */
    public static String getNetworkErrorMessage(Exception e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("timeout")) {
                return "Tempo de conexão esgotado. Verifique sua internet";
            }
            if (e.getMessage().contains("Unable to resolve host")) {
                return "Sem conexão com a internet";
            }
        }
        return "Erro de rede. Verifique sua conexão";
    }
}