package activity.amigosecreto.util;

import android.content.Context;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)

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

    // --- getNetworkErrorMessage ---

    @Test
    public void getNetworkErrorMessage_timeout_retorna_msg_amigavel() {
        Exception e = new Exception("connection timeout exceeded");
        String msg = ValidationUtils.getNetworkErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getNetworkErrorMessage_unable_to_resolve_host_retorna_msg_amigavel() {
        Exception e = new Exception("Unable to resolve host \"api.example.com\"");
        String msg = ValidationUtils.getNetworkErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getNetworkErrorMessage_erro_generico_retorna_msg_padrao() {
        Exception e = new Exception("unknown network error");
        String msg = ValidationUtils.getNetworkErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    @Test
    public void getNetworkErrorMessage_message_null_retorna_msg_padrao() {
        Exception e = new Exception((String) null);
        String msg = ValidationUtils.getNetworkErrorMessage(e);
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }

    // --- validateNotEmpty (Robolectric) ---

    @Test
    public void validateNotEmpty_campoVazio_retorna_false_e_seta_erro() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("");
        assertFalse(ValidationUtils.validateNotEmpty(et, "Campo obrigatório"));
        assertEquals("Campo obrigatório", et.getError().toString());
    }

    @Test
    public void validateNotEmpty_campoSoEspacos_retorna_false() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("   ");
        assertFalse(ValidationUtils.validateNotEmpty(et, "Campo obrigatório"));
    }

    @Test
    public void validateNotEmpty_campoPreenchido_retorna_true_e_limpa_erro() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("Ana");
        assertTrue(ValidationUtils.validateNotEmpty(et, "Campo obrigatório"));
        assertNull(et.getError());
    }

    // --- validateName (Robolectric) ---

    @Test
    public void validateName_nomeVazio_retorna_false() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("");
        assertFalse(ValidationUtils.validateName(et));
        assertNotNull(et.getError());
    }

    @Test
    public void validateName_nomeUmCaractere_retorna_false() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("A");
        assertFalse(ValidationUtils.validateName(et));
        assertNotNull(et.getError());
    }

    @Test
    public void validateName_nomeDoisCaracteres_retorna_true() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("Al");
        assertTrue(ValidationUtils.validateName(et));
        assertNull(et.getError());
    }

    @Test
    public void validateName_nomeValido_retorna_true() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("Carlos");
        assertTrue(ValidationUtils.validateName(et));
        assertNull(et.getError());
    }

    // --- validateEmail (Robolectric) ---

    @Test
    public void validateEmail_vazio_retorna_true_email_opcional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("");
        assertTrue(ValidationUtils.validateEmail(et));
    }

    @Test
    public void validateEmail_emailValido_retorna_true() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("user@example.com");
        assertTrue(ValidationUtils.validateEmail(et));
        assertNull(et.getError());
    }

    @Test
    public void validateEmail_emailInvalido_retorna_false() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("nao-e-email");
        assertFalse(ValidationUtils.validateEmail(et));
        assertNotNull(et.getError());
    }

    // --- validatePhone (Robolectric) ---

    @Test
    public void validatePhone_vazio_retorna_true_telefone_opcional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("");
        assertTrue(ValidationUtils.validatePhone(et));
    }

    @Test
    public void validatePhone_telefoneValido_retorna_true() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("(11) 91234-5678");
        assertTrue(ValidationUtils.validatePhone(et));
        assertNull(et.getError());
    }

    @Test
    public void validatePhone_telefoneInvalido_retorna_false() {
        Context ctx = ApplicationProvider.getApplicationContext();
        EditText et = new EditText(ctx);
        et.setText("123");
        assertFalse(ValidationUtils.validatePhone(et));
        assertNotNull(et.getError());
    }

    // Utilitario para acessar constantes privadas via reflexao
    private Pattern getPattern(String fieldName) throws Exception {
        Field field = ValidationUtils.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Pattern) field.get(null);
    }
}
