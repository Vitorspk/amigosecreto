package activity.amigosecreto.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import activity.amigosecreto.db.Grupo

@Dao
abstract class GrupoRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserir(grupo: Grupo): Long

    @Update
    abstract suspend fun atualizar(grupo: Grupo): Int

    @Delete
    abstract suspend fun remover(grupo: Grupo)

    @Query("SELECT * FROM grupo ORDER BY id DESC")
    abstract suspend fun listar(): List<Grupo>

    @Query("SELECT * FROM grupo WHERE id = :id LIMIT 1")
    abstract suspend fun buscarPorId(id: Int): Grupo?

    @Query("DELETE FROM participante")
    protected abstract suspend fun deletarTodosParticipantes()

    @Query("DELETE FROM grupo")
    protected abstract suspend fun deletarTodosGrupos()

    @Query("DELETE FROM desejo")
    protected abstract suspend fun deletarTodosDesejos()

    @Query("DELETE FROM sorteio_par")
    protected abstract suspend fun deletarTodosSorteioPares()

    @Query("DELETE FROM sorteio")
    protected abstract suspend fun deletarTodosSorteios()

    /**
     * Remove todos os dados de forma atômica respeitando ordem das FKs:
     * desejos e pares de sorteio antes dos participantes; sorteios antes dos grupos.
     */
    @Transaction
    open suspend fun deletarTudo() {
        deletarTodosDesejos()
        deletarTodosSorteioPares()
        deletarTodosSorteios()
        deletarTodosParticipantes()
        deletarTodosGrupos()
    }

    // ── Statistics queries ─────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM grupo")
    abstract suspend fun contarGrupos(): Int

    @Query("SELECT COUNT(*) FROM participante")
    abstract suspend fun contarParticipantes(): Int

    @Query("SELECT COUNT(*) FROM sorteio")
    abstract suspend fun contarSorteios(): Int

    @Query("SELECT COUNT(*) FROM desejo")
    abstract suspend fun contarDesejos(): Int

    @Query("""
        SELECT AVG(
            CASE
                WHEN preco_minimo > 0 AND preco_maximo > 0 THEN (preco_minimo + preco_maximo) / 2.0
                WHEN preco_minimo > 0 THEN preco_minimo
                ELSE preco_maximo
            END
        ) FROM desejo WHERE preco_minimo > 0 OR preco_maximo > 0
    """)
    abstract suspend fun mediaValorDesejos(): Double?
}
