package activity.amigosecreto.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import activity.amigosecreto.db.ParticipanteDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que verifica quantos grupos têm participantes mas ainda não realizaram o sorteio,
 * e exibe uma notificação de lembrete para o organizador.
 *
 * Grupos "pendentes" = grupos com >= [MIN_PARTICIPANTES] participantes onde nenhum tem
 * amigo_sorteado_id definido, calculado via query única com GROUP BY + HAVING (sem N+1).
 *
 * Se não houver grupos pendentes, o Worker cancela o próprio agendamento para não
 * consumir recursos desnecessariamente. O agendamento é retomado via [LembreteScheduler.agendar]
 * em [activity.amigosecreto.GruposActivity.onCreate].
 *
 * Tratamento de erros:
 * - Erros transitorios (ex: banco temporariamente ocupado) → [Result.retry] com backoff exponencial.
 * - O WorkManager aplica backoff de no mínimo 10 segundos, crescendo até 5 horas por padrão.
 *   Chamadas a [LembreteScheduler.agendar] com policy KEEP não resetam esse backoff — em caso
 *   de falhas repetidas o lembrete pode atrasar mais de 24h. Esse comportamento é aceitável dado
 *   que lembretes são best-effort, não críticos.
 *
 * TODO: Migrar para @HiltWorker + @AssistedInject (WorkManager 2.9.1 já suportado neste projeto)
 *   para injetar ParticipanteRepository via Hilt e seguir o Repository pattern do projeto.
 */
class LembreteWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val gruposPendentes = contarGruposPendentes(appContext)
            if (gruposPendentes == 0) {
                // Sem grupos pendentes — cancela o agendamento para poupar recursos
                LembreteScheduler.cancelar(appContext)
            } else {
                NotificationHelper.exibirLembreteSorteio(appContext, gruposPendentes)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "LembreteWorker: erro ao verificar grupos pendentes — retry agendado", e)
            Result.retry()
        }
    }

    private fun contarGruposPendentes(context: Context): Int {
        val participanteDao = ParticipanteDAO(context)
        participanteDao.open()
        return try {
            participanteDao.contarGruposPendentes(MIN_PARTICIPANTES)
        } finally {
            participanteDao.close()
        }
    }

    companion object {
        private const val TAG = "LembreteWorker"
        const val MIN_PARTICIPANTES = 3
    }
}
