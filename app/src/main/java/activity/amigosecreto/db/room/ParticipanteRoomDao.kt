package activity.amigosecreto.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import activity.amigosecreto.db.Exclusao
import activity.amigosecreto.db.Participante

@Dao
abstract class ParticipanteRoomDao {

    // ── Insert / Update / Delete ──────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserir(participante: Participante): Long

    @Update
    abstract suspend fun atualizar(participante: Participante): Int

    @Query("DELETE FROM participante WHERE id = :id")
    abstract suspend fun remover(id: Int)

    @Query("DELETE FROM participante WHERE grupo_id = :grupoId")
    abstract suspend fun deletarTodosDoGrupo(grupoId: Int)

    // ── Sorteio ───────────────────────────────────────────────────────────────

    @Query("UPDATE participante SET amigo_sorteado_id = :amigoId WHERE id = :participanteId")
    abstract suspend fun atualizarAmigoSorteado(participanteId: Int, amigoId: Int)

    @Query("UPDATE participante SET amigo_sorteado_id = NULL, enviado = 0 WHERE grupo_id = :grupoId")
    abstract suspend fun limparSorteioDoGrupo(grupoId: Int)

    @Query("UPDATE participante SET enviado = 1 WHERE id = :id")
    abstract suspend fun marcarComoEnviado(id: Int)

    @Query("UPDATE participante SET foi_notificado = 1 WHERE id = :id")
    abstract suspend fun marcarComoNotificado(id: Int)

    @Query("UPDATE participante SET confirmou_presente = 1 WHERE id = :id")
    abstract suspend fun marcarConfirmacaoCompra(id: Int)

    @Query("SELECT COUNT(*) FROM participante WHERE grupo_id = :grupoId AND confirmou_presente = 1")
    abstract suspend fun contarConfirmados(grupoId: Int): Int

    // ── Exclusions ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun inserirExclusao(exclusao: Exclusao)

    @Query("DELETE FROM exclusao WHERE participante_id = :participanteId AND excluido_id = :excluidoId")
    abstract suspend fun removerExclusao(participanteId: Int, excluidoId: Int)

    // ── Queries ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM participante WHERE grupo_id = :grupoId ORDER BY nome")
    abstract suspend fun listarPorGrupoSemExclusoes(grupoId: Int): List<Participante>

    /**
     * Busca todas as exclusões para participantes de um grupo em uma única query.
     * Evita N+1 ao carregar exclusões de todos os participantes de uma vez.
     *
     * Usa @RawQuery com [SimpleSQLiteQuery] porque o IN (?,?,?) precisa de tamanho dinâmico.
     * Room não suporta List<Int> em @Query com IN — somente via rawQuery ou JOIN.
     * Retorna lista de pares (participante_id, excluido_id).
     */
    @RawQuery
    protected abstract suspend fun listarExclusoesRaw(query: SupportSQLiteQuery): List<ExclusaoRow>

    suspend fun listarExclusoesPorIds(ids: List<Int>): List<ExclusaoRow> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val query = SimpleSQLiteQuery(
            "SELECT participante_id, excluido_id FROM exclusao WHERE participante_id IN ($placeholders)",
            ids.map { it }.toTypedArray()
        )
        return listarExclusoesRaw(query)
    }

    @Query("SELECT * FROM participante WHERE id = :id LIMIT 1")
    abstract suspend fun buscarPorId(id: Int): Participante?

    @Query("SELECT nome FROM participante WHERE id = :id LIMIT 1")
    abstract suspend fun getNome(id: Int): String?

    @Query("SELECT COUNT(*) FROM participante WHERE grupo_id = :grupoId")
    abstract suspend fun contarPorGrupo(grupoId: Int): Int

    @Query("""
        SELECT grupo_id, COUNT(*) as count 
        FROM participante 
        GROUP BY grupo_id
    """)
    abstract suspend fun contarPorTodosGrupos(): List<GrupoContagem>

    @Query("""
        SELECT COUNT(DISTINCT p.grupo_id) FROM participante p
        WHERE p.amigo_sorteado_id IS NULL
        GROUP BY p.grupo_id
        HAVING COUNT(*) >= :minParticipantes
    """)
    abstract suspend fun contarGruposPendentes(minParticipantes: Int = 3): Int

    // ── Composite operations ──────────────────────────────────────────────────

    /**
     * Carrega participantes do grupo com suas exclusões populadas.
     * Dois passes: (1) busca participantes, (2) busca exclusões em batch.
     */
    @Transaction
    open suspend fun listarPorGrupo(grupoId: Int): List<Participante> {
        val participantes = listarPorGrupoSemExclusoes(grupoId)
        if (participantes.isEmpty()) return participantes

        val ids = participantes.map { it.id }
        val exclusoes = listarExclusoesPorIds(ids)

        // Agrupa exclusões por participante_id em um Map para O(1) lookup
        val exclusoesPorParticipante = exclusoes.groupBy { it.participanteId }

        participantes.forEach { p ->
            p.idsExcluidos = exclusoesPorParticipante[p.id]
                ?.map { it.excluidoId }
                ?.toMutableList()
                ?: mutableListOf()
        }
        return participantes
    }

    /**
     * Salva pares de sorteio atomicamente (atualiza amigo_sorteado_id de cada participante).
     */
    @Transaction
    open suspend fun salvarSorteio(participantes: List<Participante>, sorteados: List<Participante>): Boolean {
        if (participantes.size != sorteados.size) return false
        participantes.forEachIndexed { index, p ->
            atualizarAmigoSorteado(p.id, sorteados[index].id)
        }
        return true
    }

    /**
     * Salva exclusões de um participante de forma atômica (adiciona e remove em uma transação).
     */
    @Transaction
    open suspend fun salvarExclusoes(
        participanteId: Int,
        adicionar: List<Int>,
        remover: List<Int>,
    ) {
        adicionar.forEach { inserirExclusao(Exclusao(participanteId, it)) }
        remover.forEach { removerExclusao(participanteId, it) }
    }

    // ── Helper data classes (projeções de query) ──────────────────────────────

    data class ExclusaoRow(
        @androidx.room.ColumnInfo(name = "participante_id") val participanteId: Int,
        @androidx.room.ColumnInfo(name = "excluido_id") val excluidoId: Int,
    )

    data class GrupoContagem(
        @androidx.room.ColumnInfo(name = "grupo_id") val grupoId: Int,
        @androidx.room.ColumnInfo(name = "count") val count: Int,
    )
}
