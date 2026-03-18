package activity.amigosecreto.util

import android.content.Context
import timber.log.Timber
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utilitário stateless para agendar e cancelar o [LembreteWorker] via WorkManager.
 *
 * O trabalho periódico usa [ExistingPeriodicWorkPolicy.KEEP] — se já existe um agendamento
 * ativo, não substitui, evitando resetar o intervalo.
 */
object LembreteScheduler {

    private const val TAG = "LembreteScheduler"
    const val NOME_TRABALHO = "lembrete_sorteio_pendente"
    const val INTERVALO_HORAS = 24L

    /**
     * Agenda o lembrete diário. Idempotente — chamadas adicionais não resetam o timer.
     */
    fun agendar(context: Context) {
        val request = PeriodicWorkRequestBuilder<LembreteWorker>(INTERVALO_HORAS, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_TRABALHO,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("Lembrete diário agendado")
    }

    /**
     * Cancela o lembrete agendado. Chame quando não há mais grupos pendentes ou o app
     * não precisa mais enviar lembretes.
     */
    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOME_TRABALHO)
        Timber.d("Lembrete diário cancelado")
    }
}
