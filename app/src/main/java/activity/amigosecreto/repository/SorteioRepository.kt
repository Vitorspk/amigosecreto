package activity.amigosecreto.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.SorteioDAO

/**
 * Repository que encapsula todo o acesso a SorteioDAO.
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
open class SorteioRepository private constructor(private val dao: SorteioDAO) {

    constructor(context: Context) : this(SorteioDAO(context))

    @VisibleForTesting
    internal constructor(dao: SorteioDAO, @Suppress("UNUSED_PARAMETER") forTesting: Boolean) : this(dao)

    /**
     * Persiste um sorteio completo de forma atômica.
     * @return ID do sorteio criado, ou -1 em caso de falha
     */
    open fun salvarSorteioCompleto(
        grupoId: Int,
        participantes: List<Participante>,
        sorteados: List<Participante>
    ): Long {
        dao.open()
        return try { dao.salvarSorteioCompleto(grupoId, participantes, sorteados) } finally { dao.close() }
    }

    /**
     * Lista todos os sorteios de um grupo em ordem decrescente (mais recente primeiro).
     */
    open fun listarPorGrupo(grupoId: Int): List<Sorteio> {
        dao.open()
        return try { dao.listarPorGrupo(grupoId) } finally { dao.close() }
    }

    /**
     * Retorna o sorteio mais recente do grupo, ou null se não houver.
     */
    open fun buscarUltimoPorGrupo(grupoId: Int): Sorteio? {
        dao.open()
        return try { dao.buscarUltimoPorGrupo(grupoId) } finally { dao.close() }
    }
}
