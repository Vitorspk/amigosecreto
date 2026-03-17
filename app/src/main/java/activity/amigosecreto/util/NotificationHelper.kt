package activity.amigosecreto.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import activity.amigosecreto.GruposActivity
import activity.amigosecreto.R

/**
 * Utilitário stateless para criar o canal de notificação e exibir lembretes ao organizador.
 *
 * Uso:
 * 1. Chamar [criarCanal] uma vez na inicialização do app (via [AmigoSecretoApplication]).
 * 2. Chamar [exibirLembreteSorteio] a partir de um Worker em background.
 */
object NotificationHelper {

    const val CANAL_LEMBRETES = "canal_lembretes_sorteio"
    const val NOTIFICACAO_LEMBRETE_ID = 1001

    /**
     * Cria o canal de notificação (obrigatório no Android 8+).
     * Pode ser chamado múltiplas vezes — idempotente.
     */
    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nome = context.getString(R.string.notificacao_canal_nome)
            val descricao = context.getString(R.string.notificacao_canal_descricao)
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val canal = NotificationChannel(CANAL_LEMBRETES, nome, importancia).apply {
                description = descricao
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    /**
     * Exibe uma notificação de lembrete indicando quantos grupos ainda precisam de sorteio.
     *
     * @param gruposPendentes número de grupos com participantes mas sem sorteio realizado
     */
    fun exibirLembreteSorteio(context: Context, gruposPendentes: Int) {
        if (gruposPendentes <= 0) return

        val intent = Intent(context, GruposActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = context.getString(R.string.notificacao_lembrete_titulo)
        val mensagem = context.resources.getQuantityString(
            R.plurals.notificacao_lembrete_mensagem, gruposPendentes, gruposPendentes
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_LEMBRETES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val temPermissao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (temPermissao) {
            NotificationManagerCompat.from(context).notify(NOTIFICACAO_LEMBRETE_ID, notificacao)
        }
    }
}
