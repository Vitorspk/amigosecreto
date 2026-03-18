package activity.amigosecreto.repository

import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.room.DesejoRoomDao

/**
 * Repository que encapsula todo o acesso a [DesejoRoomDao].
 *
 * Todos os métodos são suspend — devem ser chamados a partir de uma coroutine.
 */
class DesejoRepository @VisibleForTesting internal constructor(
    private val dao: DesejoRoomDao,
) {

    suspend fun listar(): List<Desejo> = dao.listar()

    suspend fun listarPorParticipante(participanteId: Int): List<Desejo> =
        dao.listarPorParticipante(participanteId)

    suspend fun contarDesejosPorParticipante(participanteId: Int): Int =
        dao.contarPorParticipante(participanteId)

    suspend fun contarDesejosPorGrupo(grupoId: Int): Map<Int, Int> =
        dao.contarDesejosPorGrupo(grupoId).associate { it.participanteId to it.count }

    suspend fun listarDesejosPorGrupo(grupoId: Int): Map<Int, List<Desejo>> =
        dao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }

    suspend fun inserir(desejo: Desejo) {
        val id = dao.inserir(desejo)
        desejo.id = id.toInt()
    }

    suspend fun alterar(oldDesejo: Desejo, newDesejo: Desejo) {
        newDesejo.id = oldDesejo.id
        dao.atualizar(newDesejo)
    }

    suspend fun remover(desejo: Desejo) = dao.remover(desejo.id)

    suspend fun buscarPorId(id: Int): Desejo? = dao.buscarPorId(id)
}
