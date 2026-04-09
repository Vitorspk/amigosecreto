package activity.amigosecreto.repository

import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.room.ParticipanteRoomDao

/**
 * Repository que encapsula todo o acesso a [ParticipanteRoomDao].
 *
 * Isola Activities e ViewModels do Room, facilitando testes via injeção do DAO.
 * Todos os métodos são suspend — devem ser chamados a partir de uma coroutine.
 *
 * A classe é `open` para que testes Kotlin possam criar subclasses anônimas com override.
 */
open class ParticipanteRepository @VisibleForTesting internal constructor(
    private val dao: ParticipanteRoomDao,
) {

    open suspend fun listarPorGrupo(grupoId: Int): List<Participante> =
        dao.listarPorGrupo(grupoId)

    open suspend fun inserir(participante: Participante, grupoId: Int) {
        participante.grupoId = grupoId
        val id = dao.inserir(participante)
        participante.id = id.toInt()
    }

    open suspend fun atualizar(participante: Participante): Boolean =
        dao.atualizar(participante) > 0

    open suspend fun remover(id: Int) = dao.remover(id)

    open suspend fun deletarTodosDoGrupo(grupoId: Int) = dao.deletarTodosDoGrupo(grupoId)

    open suspend fun limparSorteioDoGrupo(grupoId: Int) = dao.limparSorteioDoGrupo(grupoId)

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    open suspend fun adicionarExclusao(idParticipante: Int, idExcluido: Int) {
        dao.salvarExclusoes(idParticipante, listOf(idExcluido), emptyList())
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    open suspend fun removerExclusao(idParticipante: Int, idExcluido: Int) {
        dao.salvarExclusoes(idParticipante, emptyList(), listOf(idExcluido))
    }

    open suspend fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) =
        dao.salvarExclusoes(participanteId, adicionar, remover)

    open suspend fun salvarSorteio(participantes: List<Participante>, sorteados: List<Participante>): Boolean =
        dao.salvarSorteio(participantes, sorteados)

    open suspend fun marcarComoEnviado(id: Int) = dao.marcarComoEnviado(id)

    open suspend fun confirmarCompra(id: Int) = dao.marcarConfirmacaoCompra(id)

    open suspend fun getNomeAmigoSorteado(amigoId: Int): String =
        dao.getNome(amigoId) ?: NOME_AMIGO_DESCONHECIDO

    open suspend fun contarPorGrupo(): Map<Int, Int> =
        dao.contarPorTodosGrupos().associate { it.grupoId to it.count }

    open suspend fun contarEnviadosPorGrupo(): Map<Int, Int> =
        dao.contarEnviadosPorTodosGrupos().associate { it.grupoId to it.count }

    open suspend fun buscarPorId(id: Int): Participante? = dao.buscarPorId(id)

    companion object {
        const val NOME_AMIGO_DESCONHECIDO = "Ninguém"
    }
}
