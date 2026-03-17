package activity.amigosecreto.util

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.ParticipanteDAO

/**
 * Worker que verifica quantos grupos têm participantes mas ainda não realizaram o sorteio,
 * e exibe uma notificação de lembrete para o organizador.
 *
 * Grupos "pendentes" = grupos com >= 3 participantes onde nenhum tem amigo_sorteado_id definido.
 */
class LembreteWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val gruposPendentes = contarGruposPendentes(appContext)
            NotificationHelper.exibirLembreteSorteio(appContext, gruposPendentes)
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
