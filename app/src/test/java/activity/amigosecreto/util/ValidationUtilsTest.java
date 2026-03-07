package activity.amigosecreto.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ValidationUtilsTest {

    // --- isNotEmpty ---

    @Test
    public void isNotEmpty_nulo_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty(null));
    }

    @Test
    public void isNotEmpty_string_vazia_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty(""));
    }

    @Test
    public void isNotEmpty_so_espacos_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty("   "));
    }

    @Test
    public void isNotEmpty_string_valida_retorna_true() {
        assertTrue(ValidationUtils.isNotEmpty("Ana"));
    }

    @Test
    public void isNotEmpty_string_com_espacos_laterais_retorna_true() {
        assertTrue(ValidationUtils.isNotEmpty("  Ana  "));
    }

    // --- validateMinParticipants ---

    @Test
    public void validateMinParticipants_exatamente_minimo_retorna_null() {
        assertNull(ValidationUtils.validateMinParticipants(3, 3));
    }

    @Test
    public void validateMinParticipants_acima_do_minimo_retorna_null() {
        assertNull(ValidationUtils.validateMinParticipants(5, 3));
    }

    @Test
    public void validateMinParticipants_abaixo_do_minimo_retorna_mensagem() {
        String msg = ValidationUtils.validateMinParticipants(2, 3);
        assertNotNull(msg);
        assertTrue(msg.contains("3"));
    }

    @Test
    public void validateMinParticipants_zero_participantes_retorna_mensagem() {
        assertNotNull(ValidationUtils.validateMinParticipants(0, 3));
    }

    // --- getDatabaseErrorMessage ---

    @Test
    public void getDatabaseErrorMessage_UNIQUE_retorna_msg_amigavel() {
        Exception e = new Exception("UNIQUE constraint failed");
        String msg = ValidationUtils.getDatabaseErrorMessage(e);
        assertTrue(msg.toLowerCase().contains("existe") || msg.toLowerCase().contains("unique") || msg.contains("banco"));
    }

    @Test
    public void getDatabaseErrorMessage_NOT_NULL_retorna_msg_amigavel() {
        Exception e = new Exception("NOT NULL constraint failed");
        String msg = ValidationUtils.getDatabaseErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getDatabaseErrorMessage_FOREIGN_KEY_retorna_msg_amigavel() {
        Exception e = new Exception("FOREIGN KEY constraint failed");
        String msg = ValidationUtils.getDatabaseErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getDatabaseErrorMessage_erro_generico_retorna_msg_padrao() {
        Exception e = new Exception("algum erro desconhecido");
        String msg = ValidationUtils.getDatabaseErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getDatabaseErrorMessage_message_null_retorna_msg_padrao() {
        Exception e = new Exception((String) null);
        String msg = ValidationUtils.getDatabaseErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    // --- EMAIL_PATTERN via reflexao ---

    @Test
    public void email_valido_corresponde_ao_padrao() throws Exception {
        Pattern p = getPattern("EMAIL_PATTERN");
        assertTrue(p.matcher("teste@email.com").matches());
        assertTrue(p.matcher("user.name+tag@sub.domain.org").matches());
    }

    @Test
    public void email_invalido_nao_corresponde_ao_padrao() throws Exception {
        Pattern p = getPattern("EMAIL_PATTERN");
        assertFalse(p.matcher("nao-e-email").matches());
        assertFalse(p.matcher("@semlocal.com").matches());
        assertFalse(p.matcher("sem_arroba.com").matches());
    }

    // --- PHONE_PATTERN via reflexao ---

    @Test
    public void telefone_BR_formato_padrao_corresponde() throws Exception {
        Pattern p = getPattern("PHONE_PATTERN");
        assertTrue(p.matcher("(11) 91234-5678").matches());
        assertTrue(p.matcher("(11) 1234-5678").matches());
    }

    @Test
    public void telefone_BR_com_codigo_pais_corresponde() throws Exception {
        Pattern p = getPattern("PHONE_PATTERN");
        assertTrue(p.matcher("+55 11 91234-5678").matches());
    }

    @Test
    public void telefone_invalido_nao_corresponde() throws Exception {
        Pattern p = getPattern("PHONE_PATTERN");
        assertFalse(p.matcher("123").matches());
        assertFalse(p.matcher("abc").matches());
    }

    // Utilitario para acessar constantes privadas via reflexao
    private Pattern getPattern(String fieldName) throws Exception {
        Field field = ValidationUtils.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Pattern) field.get(null);
    }
}
