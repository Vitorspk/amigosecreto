package activity.amigosecreto.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import activity.amigosecreto.db.Desejo

@Dao
interface DesejoRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(desejo: Desejo): Long

    @Update
    suspend fun atualizar(desejo: Desejo): Int

    @Query("DELETE FROM desejo WHERE id = :id")
    suspend fun remover(id: Int)

    @Query("SELECT * FROM desejo WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Desejo?

    @Query("SELECT * FROM desejo")
    suspend fun listar(): List<Desejo>

    @Query("SELECT * FROM desejo WHERE participante_id = :participanteId")
    suspend fun listarPorParticipante(participanteId: Int): List<Desejo>

    @Query("SELECT COUNT(*) FROM desejo WHERE participante_id = :participanteId")
    suspend fun contarPorParticipante(participanteId: Int): Int

    /**
     * Conta desejos agrupados por participante para um grupo inteiro.
     * Evita N+1: uma única query via JOIN retorna Map<participanteId, count>.
     */
    @Query("""
        SELECT d.participante_id, COUNT(*) as count
        FROM desejo d
        INNER JOIN participante p ON d.participante_id = p.id
        WHERE p.grupo_id = :grupoId
        GROUP BY d.participante_id
    """)
    suspend fun contarDesejosPorGrupo(grupoId: Int): List<DesejoContagem>

    /**
     * Retorna todos os desejos de todos os participantes de um grupo em uma query.
     * Evita N+1: uma única query via JOIN retorna todos os desejos agrupáveis por participanteId.
     */
    @Query("""
        SELECT d.*
        FROM desejo d
        INNER JOIN participante p ON d.participante_id = p.id
        WHERE p.grupo_id = :grupoId
    """)
    suspend fun listarDesejosPorGrupo(grupoId: Int): List<Desejo>

    // ── Helper projection ─────────────────────────────────────────────────────

    data class DesejoContagem(
        @androidx.room.ColumnInfo(name = "participante_id") val participanteId: Int,
        @androidx.room.ColumnInfo(name = "count") val count: Int,
    )
}
