package activity.amigosecreto.util

import android.content.Context
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.regex.Pattern

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ValidationUtilsTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    // --- isNotEmpty ---

    @Test
    fun isNotEmpty_nulo_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty(null))
    }

    @Test
    fun isNotEmpty_string_vazia_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty(""))
    }

    @Test
    fun isNotEmpty_so_espacos_retorna_false() {
        assertFalse(ValidationUtils.isNotEmpty("   "))
    }

    @Test
    fun isNotEmpty_string_valida_retorna_true() {
        assertTrue(ValidationUtils.isNotEmpty("Ana"))
    }

    @Test
    fun isNotEmpty_string_com_espacos_laterais_retorna_true() {
        assertTrue(ValidationUtils.isNotEmpty("  Ana  "))
    }

    // --- validateMinParticipants ---

    @Test
    fun validateMinParticipants_exatamente_minimo_retorna_null() {
        assertNull(ValidationUtils.validateMinParticipants(ctx, 3, 3))
    }

    @Test
    fun validateMinParticipants_acima_do_minimo_retorna_null() {
        assertNull(ValidationUtils.validateMinParticipants(ctx, 5, 3))
    }

    @Test
    fun validateMinParticipants_abaixo_do_minimo_retorna_mensagem() {
        val msg = ValidationUtils.validateMinParticipants(ctx, 2, 3)
        assertNotNull(msg)
        assertTrue(msg!!.contains("3"))
    }

    @Test
    fun validateMinParticipants_zero_participantes_retorna_mensagem() {
        assertNotNull(ValidationUtils.validateMinParticipants(ctx, 0, 3))
    }

    // --- getDatabaseErrorMessage ---

    @Test
    fun getDatabaseErrorMessage_UNIQUE_retorna_msg_amigavel() {
        val msg = ValidationUtils.getDatabaseErrorMessage(ctx, Exception("UNIQUE constraint failed"))
        assertEquals(ctx.getString(activity.amigosecreto.R.string.validation_db_unique), msg)
    }

    @Test
    fun getDatabaseErrorMessage_NOT_NULL_retorna_msg_amigavel() {
        val msg = ValidationUtils.getDatabaseErrorMessage(ctx, Exception("NOT NULL constraint failed"))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getDatabaseErrorMessage_FOREIGN_KEY_retorna_msg_amigavel() {
        val msg = ValidationUtils.getDatabaseErrorMessage(ctx, Exception("FOREIGN KEY constraint failed"))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getDatabaseErrorMessage_erro_generico_retorna_msg_padrao() {
        val msg = ValidationUtils.getDatabaseErrorMessage(ctx, Exception("algum erro desconhecido"))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getDatabaseErrorMessage_message_null_retorna_msg_padrao() {
        val msg = ValidationUtils.getDatabaseErrorMessage(ctx, Exception(null as String?))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    // --- EMAIL_PATTERN via reflexão ---

    @Test
    fun email_valido_corresponde_ao_padrao() {
        val p = getPattern("EMAIL_PATTERN")
        assertTrue(p.matcher("teste@email.com").matches())
        assertTrue(p.matcher("user.name+tag@sub.domain.org").matches())
    }

    @Test
    fun email_invalido_nao_corresponde_ao_padrao() {
        val p = getPattern("EMAIL_PATTERN")
        assertFalse(p.matcher("nao-e-email").matches())
        assertFalse(p.matcher("@semlocal.com").matches())
        assertFalse(p.matcher("sem_arroba.com").matches())
    }

    // --- PHONE_PATTERN via reflexão ---

    @Test
    fun telefone_BR_formato_padrao_corresponde() {
        val p = getPattern("PHONE_PATTERN")
        assertTrue(p.matcher("(11) 91234-5678").matches())
        assertTrue(p.matcher("(11) 1234-5678").matches())
    }

    @Test
    fun telefone_BR_com_codigo_pais_corresponde() {
        assertTrue(getPattern("PHONE_PATTERN").matcher("+55 11 91234-5678").matches())
    }

    @Test
    fun telefone_invalido_nao_corresponde() {
        val p = getPattern("PHONE_PATTERN")
        assertFalse(p.matcher("123").matches())
        assertFalse(p.matcher("abc").matches())
    }

    // --- getNetworkErrorMessage ---

    @Test
    fun getNetworkErrorMessage_timeout_retorna_msg_amigavel() {
        val msg = ValidationUtils.getNetworkErrorMessage(ctx, Exception("connection timeout exceeded"))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getNetworkErrorMessage_unable_to_resolve_host_retorna_msg_amigavel() {
        val msg = ValidationUtils.getNetworkErrorMessage(ctx, Exception("Unable to resolve host \"api.example.com\""))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getNetworkErrorMessage_erro_generico_retorna_msg_padrao() {
        val msg = ValidationUtils.getNetworkErrorMessage(ctx, Exception("unknown network error"))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    @Test
    fun getNetworkErrorMessage_message_null_retorna_msg_padrao() {
        val msg = ValidationUtils.getNetworkErrorMessage(ctx, Exception(null as String?))
        assertNotNull(msg); assertFalse(msg.isEmpty())
    }

    // --- validateNotEmpty (Robolectric) ---

    @Test
    fun validateNotEmpty_campoVazio_retorna_false_e_seta_erro() {
        val et = EditText(ctx).also { it.setText("") }
        assertFalse(ValidationUtils.validateNotEmpty(et, "Campo obrigatório"))
        assertEquals("Campo obrigatório", et.error.toString())
    }

    @Test
    fun validateNotEmpty_campoSoEspacos_retorna_false() {
        assertFalse(ValidationUtils.validateNotEmpty(EditText(ctx).also { it.setText("   ") }, "Campo obrigatório"))
    }

    @Test
    fun validateNotEmpty_campoPreenchido_retorna_true_e_limpa_erro() {
        val et = EditText(ctx).also { it.setText("Ana") }
        assertTrue(ValidationUtils.validateNotEmpty(et, "Campo obrigatório"))
        assertNull(et.error)
    }

    // --- validateName (Robolectric) ---

    @Test
    fun validateName_nomeVazio_retorna_false() {
        val et = EditText(ctx).also { it.setText("") }
        assertFalse(ValidationUtils.validateName(ctx, et))
        assertNotNull(et.error)
    }

    @Test
    fun validateName_nomeUmCaractere_retorna_false() {
        val et = EditText(ctx).also { it.setText("A") }
        assertFalse(ValidationUtils.validateName(ctx, et))
        assertNotNull(et.error)
    }

    @Test
    fun validateName_nomeDoisCaracteres_retorna_true() {
        val et = EditText(ctx).also { it.setText("Al") }
        assertTrue(ValidationUtils.validateName(ctx, et))
        assertNull(et.error)
    }

    @Test
    fun validateName_nomeValido_retorna_true() {
        val et = EditText(ctx).also { it.setText("Carlos") }
        assertTrue(ValidationUtils.validateName(ctx, et))
        assertNull(et.error)
    }

    // --- validateEmail (Robolectric) ---

    @Test
    fun validateEmail_vazio_retorna_true_email_opcional() {
        assertTrue(ValidationUtils.validateEmail(ctx, EditText(ctx).also { it.setText("") }))
    }

    @Test
    fun validateEmail_emailValido_retorna_true() {
        val et = EditText(ctx).also { it.setText("user@example.com") }
        assertTrue(ValidationUtils.validateEmail(ctx, et))
        assertNull(et.error)
    }

    @Test
    fun validateEmail_emailInvalido_retorna_false() {
        val et = EditText(ctx).also { it.setText("nao-e-email") }
        assertFalse(ValidationUtils.validateEmail(ctx, et))
        assertNotNull(et.error)
    }

    // --- validatePhone (Robolectric) ---

    @Test
    fun validatePhone_vazio_retorna_true_telefone_opcional() {
        assertTrue(ValidationUtils.validatePhone(ctx, EditText(ctx).also { it.setText("") }))
    }

    @Test
    fun validatePhone_telefoneValido_retorna_true() {
        val et = EditText(ctx).also { it.setText("(11) 91234-5678") }
        assertTrue(ValidationUtils.validatePhone(ctx, et))
        assertNull(et.error)
    }

    @Test
    fun validatePhone_telefoneInvalido_retorna_false() {
        val et = EditText(ctx).also { it.setText("123") }
        assertFalse(ValidationUtils.validatePhone(ctx, et))
        assertNotNull(et.error)
    }

    // Utilitário para acessar constantes privadas via reflexão
    private fun getPattern(fieldName: String): Pattern {
        val field = ValidationUtils::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(null) as Pattern
    }
}
