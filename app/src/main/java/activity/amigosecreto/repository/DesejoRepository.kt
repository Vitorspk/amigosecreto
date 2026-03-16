package activity.amigosecreto.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO

/**
 * Repository que encapsula todo o acesso a DesejoDAO.
 *
 * Isola Activities e ViewModels do SQLite, facilitando testes (mock/substituto)
 * e evolução futura (ex: Room, sync com servidor).
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
class DesejoRepository private constructor(private val dao: DesejoDAO) {

    constructor(context: Context) : this(DesejoDAO(context))

    @VisibleForTesting
    internal constructor(dao: DesejoDAO, @Suppress("UNUSED_PARAMETER") forTesting: Boolean) : this(dao)

    /**
     * Lista todos os desejos do banco, sem filtro por grupo ou participante.
     * Usado por ListarDesejosActivity (visão global). Não usar em loops por participante —
     * prefira listarDesejosPorGrupo() para evitar carregar dados de outros grupos.
     */
    fun listar(): List<Desejo> {
        dao.open()
        return try { dao.listar() } finally { dao.close() }
    }

    fun listarPorParticipante(participanteId: Int): List<Desejo> {
        dao.open()
        return try { dao.listarPorParticipante(participanteId) } finally { dao.close() }
    }

    fun contarDesejosPorParticipante(participanteId: Int): Int {
        dao.open()
        return try { dao.contarDesejosPorParticipante(participanteId) } finally { dao.close() }
    }

    /**
     * Retorna contagens de desejos por participante para um grupo inteiro em uma única query.
     * Use no lugar de chamar contarDesejosPorParticipante() em loop (evita problema N+1).
     */
    fun contarDesejosPorGrupo(grupoId: Int): Map<Int, Int> {
        dao.open()
        return try { dao.contarDesejosPorGrupo(grupoId) } finally { dao.close() }
    }

    /**
     * Retorna um mapa participante_id → lista de desejos para todos os participantes de um grupo,
     * usando uma única query (evita N open/close ao preparar mensagens para o grupo inteiro).
     */
    fun listarDesejosPorGrupo(grupoId: Int): Map<Int, List<Desejo>> {
        dao.open()
        return try { dao.listarDesejosPorGrupo(grupoId) } finally { dao.close() }
    }

    fun inserir(desejo: Desejo) {
        dao.open()
        try { dao.inserir(desejo) } finally { dao.close() }
    }

    fun alterar(oldDesejo: Desejo, newDesejo: Desejo) {
        dao.open()
        try { dao.alterar(oldDesejo, newDesejo) } finally { dao.close() }
    }

    fun remover(desejo: Desejo) {
        dao.open()
        try { dao.remover(desejo) } finally { dao.close() }
    }

    fun buscarPorId(id: Int): Desejo? {
        dao.open()
        return try { dao.buscarPorId(id) } finally { dao.close() }
    }

    /**
     * Retorna MAX(id)+1 para uso pré-inserção em Activities Java ainda não migradas.
     * Mantido para preservar isolamento do Repository pattern — Activities não devem
     * acessar o DAO diretamente.
     * TODO: remover quando InserirDesejoActivity migrar para Kotlin (test cleanup PR).
     * ParticipanteDesejosActivity já foi migrado e não usa mais este método.
     */
    fun proximoId(): Int {
        dao.open()
        return try { dao.proximoId() } finally { dao.close() }
    }
}
