package activity.amigosecreto.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.widget.Toast
import activity.amigosecreto.R

/**
 * Utilitário stateless para compartilhar mensagens via canais específicos.
 *
 * Cada método abre o app alvo diretamente (quando disponível) ou cai de volta para o
 * share sheet genérico do sistema. Todos os métodos são seguros para chamar mesmo
 * que o app alvo não esteja instalado.
 */
object CompartilharHelper {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    private const val TELEGRAM_FOSS_PACKAGE = "org.telegram.messenger.foss"

    /**
     * Compartilha via WhatsApp. Se WhatsApp não estiver instalado, abre o share sheet genérico.
     */
    fun compartilharWhatsApp(context: Context, mensagem: String, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensagem)
            setPackage(WHATSAPP_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            compartilharGenerico(context, mensagem, titulo)
        }
    }

    /**
     * Compartilha via WhatsApp para um número específico usando deep link wa.me.
     *
     * Remove todos os caracteres não dígitos do telefone antes de montar a URI —
     * wa.me espera apenas dígitos sem '+' (ex: 5511999999999).
     * Fallback para [compartilharWhatsApp] (sem destinatário) se o telefone for vazio;
     * fallback para [compartilharGenerico] se o intent não puder ser resolvido.
     *
     * @param telefone número do participante (qualquer formato local ou internacional)
     */
    fun compartilharWhatsAppComTelefone(context: Context, telefone: String, mensagem: String, titulo: String) {
        val phone = telefone.replace(Regex("[^\\d]"), "")
        if (phone.isBlank()) {
            compartilharWhatsApp(context, mensagem, titulo)
            return
        }
        val uri = Uri.Builder()
            .scheme("https")
            .authority("wa.me")
            .appendPath(phone)
            .appendQueryParameter("text", mensagem)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            compartilharGenerico(context, mensagem, titulo)
        }
    }

    /**
     * Compartilha via Telegram. Tenta o Telegram FOSS como fallback.
     * Se nenhum estiver instalado, abre o share sheet genérico.
     */
    fun compartilharTelegram(context: Context, mensagem: String, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensagem)
            setPackage(TELEGRAM_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            // Tenta Telegram FOSS
        }
        val foss = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensagem)
            setPackage(TELEGRAM_FOSS_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(foss)
        } catch (e: ActivityNotFoundException) {
            compartilharGenerico(context, mensagem, titulo)
        }
    }

    /**
     * Compartilha via Email. Usa [Intent.ACTION_SENDTO] com scheme mailto para abrir
     * o cliente de e-mail padrão. Se não houver app de e-mail, exibe toast de erro.
     *
     * Nota: o destinatário fica em branco (mailto:?subject=...) — o organizador preenche
     * o endereço no cliente de e-mail, pois o app não armazena e-mails dos participantes
     * de forma obrigatória.
     */
    fun compartilharEmail(context: Context, mensagem: String, assunto: String) {
        // Uri.Builder evita injection via assunto/corpo
        val uri = Uri.Builder()
            .scheme("mailto")
            .appendQueryParameter("subject", assunto)
            .appendQueryParameter("body", mensagem)
            .build()
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Abre o share sheet genérico do sistema com todos os apps disponíveis.
     */
    fun compartilharGenerico(context: Context, mensagem: String, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensagem)
        }
        context.startActivity(Intent.createChooser(intent, titulo).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Retorna true se o WhatsApp estiver instalado.
     */
    fun isWhatsAppInstalado(context: Context): Boolean =
        isAppInstalado(context, WHATSAPP_PACKAGE)

    /**
     * Retorna true se o Telegram (ou Telegram FOSS) estiver instalado.
     */
    fun isTelegramInstalado(context: Context): Boolean =
        isAppInstalado(context, TELEGRAM_PACKAGE) || isAppInstalado(context, TELEGRAM_FOSS_PACKAGE)

    private fun isAppInstalado(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: NameNotFoundException) {
            false
        }
    }
}
