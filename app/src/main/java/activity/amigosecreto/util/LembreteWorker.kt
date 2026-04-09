package activity.amigosecreto.util

import android.content.Context
import timber.log.Timber
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import activity.amigosecreto.repository.ParticipanteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
 */
@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val participanteRepository: ParticipanteRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val gruposPendentes = participanteRepository.contarGruposPendentes(MIN_PARTICIPANTES)
            if (gruposPendentes == 0) {
                // Sem grupos pendentes — cancela o agendamento para poupar recursos
                LembreteScheduler.cancelar(appContext)
            } else {
                NotificationHelper.exibirLembreteSorteio(appContext, gruposPendentes)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "LembreteWorker: erro ao verificar grupos pendentes — retry agendado")
            Result.retry()
        }
    }

    companion object {
        const val MIN_PARTICIPANTES = 3
    }
}
