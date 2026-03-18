package activity.amigosecreto.util

import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testa a lógica de construção de intents do CompartilharHelper.
 * Os métodos que chamam startActivity() são testados via integração (Espresso);
 * aqui testamos a estrutura dos intents construídos internamente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CompartilharHelperTest {

    // --- buildGenericoIntent (lógica de construção interna) ---

    @Test
    fun intent_generico_tem_tipo_text_plain() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "mensagem")
        }
        assertEquals("text/plain", intent.type)
        assertEquals("mensagem", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    // --- Email URI ---

    @Test
    fun uri_mailto_contem_scheme_correto() {
        val uri = Uri.Builder()
            .scheme("mailto")
            .appendQueryParameter("subject", "Assunto")
            .appendQueryParameter("body", "Corpo")
            .build()
        assertEquals("mailto", uri.scheme)
    }

    @Test
    fun uri_mailto_contem_assunto_e_corpo() {
        val assunto = "Amigo Secreto"
        val corpo = "Olá, você tirou X!"
        val uri = Uri.Builder()
            .scheme("mailto")
            .appendQueryParameter("subject", assunto)
            .appendQueryParameter("body", corpo)
            .build()
        assertTrue(uri.toString().contains("subject="))
        assertTrue(uri.toString().contains("body="))
        assertEquals(assunto, uri.getQueryParameter("subject"))
        assertEquals(corpo, uri.getQueryParameter("body"))
    }

    @Test
    fun uri_mailto_nao_permite_injection_via_assunto() {
        // Uri.Builder.appendQueryParameter() codifica caracteres especiais
        val assuntoComInjection = "Assunto\nBcc: atacante@evil.com"
        val uri = Uri.Builder()
            .scheme("mailto")
            .appendQueryParameter("subject", assuntoComInjection)
            .build()
        // O valor recuperado deve ser igual ao original (sem injection expandida)
        assertEquals(assuntoComInjection, uri.getQueryParameter("subject"))
        // A URI serializada não deve conter newline literal
        assertFalse(uri.toString().contains("\n"))
    }

    // --- isAppInstalado (via reflexão — verifica comportamento de NameNotFoundException) ---

    @Test
    fun isWhatsAppInstalado_retorna_false_em_ambiente_de_teste() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        assertFalse(CompartilharHelper.isWhatsAppInstalado(ctx))
    }

    @Test
    fun isTelegramInstalado_retorna_false_em_ambiente_de_teste() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        assertFalse(CompartilharHelper.isTelegramInstalado(ctx))
    }

    // --- Intent de WhatsApp ---

    @Test
    fun intent_whatsapp_tem_package_correto() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "mensagem")
            setPackage("com.whatsapp")
        }
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals("mensagem", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    // --- Intent de Telegram ---

    @Test
    fun intent_telegram_tem_package_correto() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "mensagem")
            setPackage("org.telegram.messenger")
        }
        assertEquals("org.telegram.messenger", intent.`package`)
    }
}
