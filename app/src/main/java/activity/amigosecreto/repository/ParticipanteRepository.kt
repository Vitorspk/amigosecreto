package activity.amigosecreto.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO

/**
 * Repository que encapsula todo o acesso a ParticipanteDAO.
 *
 * Isola Activities e ViewModels do SQLite, facilitando testes (mock/substituto)
 * e evolução futura (ex: Room, sync com servidor).
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 *
 * TODO: fase10-dao — quando migrarem para Room, trocar var por val nos models e
 *  construir via construtor primário, eliminando setters.
 */
open class ParticipanteRepository private constructor(private val dao: ParticipanteDAO) {

    constructor(context: Context) : this(ParticipanteDAO(context))

    @VisibleForTesting
    internal constructor(dao: ParticipanteDAO, @Suppress("UNUSED_PARAMETER") forTesting: Boolean) : this(dao)

    open fun listarPorGrupo(grupoId: Int): List<Participante> {
        dao.open()
        return try { dao.listarPorGrupo(grupoId) } finally { dao.close() }
    }

    open fun inserir(participante: Participante, grupoId: Int) {
        dao.open()
        try { dao.inserir(participante, grupoId) } finally { dao.close() }
    }

    open fun atualizar(participante: Participante): Boolean {
        dao.open()
        return try { dao.atualizar(participante) } finally { dao.close() }
    }

    open fun remover(id: Int) {
        dao.open()
        try { dao.remover(id) } finally { dao.close() }
    }

    open fun deletarTodosDoGrupo(grupoId: Int) {
        dao.open()
        try { dao.deletarTodosDoGrupo(grupoId) } finally { dao.close() }
    }

    open fun limparSorteioDoGrupo(grupoId: Int) {
        dao.open()
        try { dao.limparSorteioDoGrupo(grupoId) } finally { dao.close() }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    open fun adicionarExclusao(idParticipante: Int, idExcluido: Int) {
        dao.open()
        try { dao.adicionarExclusao(idParticipante, idExcluido) } finally { dao.close() }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    open fun removerExclusao(idParticipante: Int, idExcluido: Int) {
        dao.open()
        try { dao.removerExclusao(idParticipante, idExcluido) } finally { dao.close() }
    }

    /**
     * Aplica todas as alterações de exclusão em uma única transação atômica (evita falha parcial
     * e múltiplos open/close que ocorreriam num loop de chamadas individuais).
     */
    open fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
        dao.open()
        try { dao.salvarExclusoes(participanteId, adicionar, remover) } finally { dao.close() }
    }

    open fun salvarSorteio(participantes: List<Participante>, sorteados: List<Participante>): Boolean {
        dao.open()
        return try { dao.salvarSorteio(participantes, sorteados) } finally { dao.close() }
    }

    open fun marcarComoEnviado(id: Int) {
        dao.open()
        try { dao.marcarComoEnviado(id) } finally { dao.close() }
    }

    open fun getNomeAmigoSorteado(amigoId: Int): String {
        dao.open()
        return try { dao.getNomeAmigoSorteado(amigoId) } finally { dao.close() }
    }

    open fun contarPorGrupo(): Map<Int, Int> {
        dao.open()
        return try { dao.contarPorGrupo() } finally { dao.close() }
    }

    open fun buscarPorId(id: Int): Participante? {
        dao.open()
        return try { dao.buscarPorId(id) } finally { dao.close() }
    }
}
