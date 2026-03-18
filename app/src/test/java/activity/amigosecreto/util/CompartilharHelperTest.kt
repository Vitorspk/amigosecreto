package activity.amigosecreto.util

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

/**
 * Testes que exercitam os métodos reais do CompartilharHelper via Robolectric.
 * Usa ShadowApplication.peekNextStartedActivity() para inspecionar os intents disparados.
 *
 * Os métodos compartilharWhatsApp/Telegram são chamados com um contexto de Application:
 * Robolectric captura a ActivityNotFoundException automaticamente (nenhum app instalado)
 * e o helper faz fallback para o share sheet genérico.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CompartilharHelperTest {

    private lateinit var context: android.content.Context
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = Shadows.shadowOf(context as Application)
        // Força FLAG_ACTIVITY_NEW_TASK — necessário para startActivity fora de Activity
        context.applicationContext
    }

    // --- compartilharGenerico ---

    @Test
    fun compartilharGenerico_dispara_intent_chooser() {
        CompartilharHelper.compartilharGenerico(context, "msg", "Título")
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        assertEquals(Intent.ACTION_CHOOSER, intent.action)
    }

    @Test
    fun compartilharGenerico_mensagem_esta_no_intent_aninhado() {
        CompartilharHelper.compartilharGenerico(context, "Olá Amigo!", "Título")
        val chooser = shadowApp.peekNextStartedActivity()
        assertNotNull(chooser)
        val wrapped = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(wrapped)
        assertEquals("Olá Amigo!", wrapped!!.getStringExtra(Intent.EXTRA_TEXT))
    }

    // --- compartilharWhatsApp ---

    @Test
    fun compartilharWhatsApp_dispara_intent_action_send_com_mensagem() {
        // Robolectric não lança ActivityNotFoundException para intents com package definido;
        // verifica que o intent ACTION_SEND com a mensagem correta foi disparado.
        CompartilharHelper.compartilharWhatsApp(context, "Olá WhatsApp!", "Título")
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("Olá WhatsApp!", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("com.whatsapp", intent.`package`)
    }

    // --- compartilharTelegram ---

    @Test
    fun compartilharTelegram_dispara_intent_action_send_com_mensagem() {
        CompartilharHelper.compartilharTelegram(context, "Olá Telegram!", "Título")
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("Olá Telegram!", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("org.telegram.messenger", intent.`package`)
    }

    // --- compartilharEmail ---

    @Test
    fun compartilharEmail_dispara_intent_sendto_com_scheme_mailto() {
        CompartilharHelper.compartilharEmail(context, "corpo", "Assunto")
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data?.scheme)
    }

    @Test
    fun compartilharEmail_uri_contem_assunto_e_corpo_codificados() {
        CompartilharHelper.compartilharEmail(context, "corpo do email", "Meu Assunto")
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        val uri = intent.data
        assertNotNull(uri)
        assertEquals("Meu Assunto", uri!!.getQueryParameter("subject"))
        assertEquals("corpo do email", uri.getQueryParameter("body"))
    }

    @Test
    fun compartilharEmail_assunto_com_caracteres_especiais_nao_gera_injection() {
        // Uri.Builder.appendQueryParameter() codifica newline — previne header injection
        val assuntoMalicioso = "Assunto\nBcc: atacante@evil.com"
        CompartilharHelper.compartilharEmail(context, "corpo", assuntoMalicioso)
        val intent = shadowApp.peekNextStartedActivity()
        assertNotNull(intent)
        val uri = intent.data.toString()
        assertFalse("URI não deve conter newline literal", uri.contains("\n"))
    }

    // --- isWhatsAppInstalado / isTelegramInstalado ---

    @Test
    fun isWhatsAppInstalado_retorna_false_sem_app_instalado() {
        assertFalse(CompartilharHelper.isWhatsAppInstalado(context))
    }

    @Test
    fun isTelegramInstalado_retorna_false_sem_app_instalado() {
        assertFalse(CompartilharHelper.isTelegramInstalado(context))
    }

    // --- URI mailto independente ---

    @Test
    fun uri_mailto_via_builder_codifica_parametros_corretamente() {
        val uri = Uri.Builder()
            .scheme("mailto")
            .appendQueryParameter("subject", "Teste <especial>")
            .appendQueryParameter("body", "Linha1\nLinha2")
            .build()
        assertEquals("Teste <especial>", uri.getQueryParameter("subject"))
        assertEquals("Linha1\nLinha2", uri.getQueryParameter("body"))
        // A URI serializada não deve conter < > ou \n literais
        val uriStr = uri.toString()
        assertFalse(uriStr.contains("<"))
        assertFalse(uriStr.contains(">"))
        assertFalse(uriStr.contains("\n"))
    }
}
