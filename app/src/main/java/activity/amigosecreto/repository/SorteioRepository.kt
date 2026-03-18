package activity.amigosecreto.repository

import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.room.SorteioRoomDao

/**
 * Repository que encapsula todo o acesso a [SorteioRoomDao].
 *
 * Todos os métodos são suspend — devem ser chamados a partir de uma coroutine.
 */
open class SorteioRepository @VisibleForTesting internal constructor(
    private val dao: SorteioRoomDao,
) {

    open suspend fun salvarSorteioCompleto(
        grupoId: Int,
        participantes: List<Participante>,
        sorteados: List<Participante>,
    ): Long = dao.salvarSorteioCompleto(grupoId, participantes, sorteados)

    open suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> =
        dao.listarPorGrupo(grupoId)

    open suspend fun buscarUltimoPorGrupo(grupoId: Int): Sorteio? =
        dao.buscarUltimoPorGrupo(grupoId)
}
