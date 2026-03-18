package activity.amigosecreto.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import activity.amigosecreto.db.Grupo

@Dao
interface GrupoRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(grupo: Grupo): Long

    @Update
    suspend fun atualizar(grupo: Grupo): Int

    @Delete
    suspend fun remover(grupo: Grupo)

    @Query("SELECT * FROM grupo ORDER BY id DESC")
    suspend fun listar(): List<Grupo>

    @Query("SELECT * FROM grupo WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Grupo?

    @Query("DELETE FROM participante")
    suspend fun deletarTodosParticipantes()

    @Query("DELETE FROM grupo")
    suspend fun deletarTodosGrupos()

    @Query("DELETE FROM desejo")
    suspend fun deletarTodosDesejos()

    @Query("DELETE FROM sorteio")
    suspend fun deletarTodosSorteios()
}
