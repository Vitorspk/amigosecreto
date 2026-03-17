package activity.amigosecreto.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.ParticipanteDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que verifica quantos grupos têm participantes mas ainda não realizaram o sorteio,
 * e exibe uma notificação de lembrete para o organizador.
 *
 * Grupos "pendentes" = grupos com >= 3 participantes onde nenhum tem amigo_sorteado_id definido.
 *
 * Se não houver grupos pendentes, o Worker cancela o próprio agendamento para não
 * consumir recursos desnecessariamente. O agendamento é retomado via [LembreteScheduler.agendar]
 * quando novos grupos forem criados (chamado em [GruposActivity.onCreate]).
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
            Log.e(TAG, "LembreteWorker: erro ao verificar grupos pendentes", e)
            Result.failure()
        }
    }

    private fun contarGruposPendentes(context: Context): Int {
        val grupoDao = GrupoDAO(context)
        val participanteDao = ParticipanteDAO(context)
        grupoDao.open()
        participanteDao.open()
        return try {
            grupoDao.listar().count { grupo ->
                val participantes = participanteDao.listarPorGrupo(grupo.id)
                participantes.size >= MIN_PARTICIPANTES &&
                    participantes.none { it.amigoSorteadoId != null && it.amigoSorteadoId!! > 0 }
            }
        } finally {
            grupoDao.close()
            participanteDao.close()
        }
    }

    companion object {
        private const val TAG = "LembreteWorker"
        private const val MIN_PARTICIPANTES = 3
    }
}
