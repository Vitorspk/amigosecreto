package activity.amigosecreto.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.SorteioPar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Dao
abstract class SorteioRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirSorteio(sorteio: Sorteio): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirPar(par: SorteioPar)

    @Query("UPDATE participante SET amigo_sorteado_id = :sorteadoId WHERE id = :participanteId")
    protected abstract suspend fun atualizarAmigoSorteado(participanteId: Int, sorteadoId: Int)

    @Query("SELECT * FROM sorteio WHERE grupo_id = :grupoId ORDER BY id DESC")
    abstract suspend fun listarEventosPorGrupo(grupoId: Int): List<Sorteio>

    @Query("SELECT * FROM sorteio_par WHERE sorteio_id = :sorteioId")
    abstract suspend fun listarParesPorSorteio(sorteioId: Int): List<SorteioPar>

    @Query("SELECT * FROM sorteio WHERE grupo_id = :grupoId ORDER BY id DESC LIMIT 1")
    abstract suspend fun buscarUltimoEventoPorGrupo(grupoId: Int): Sorteio?

    @Query("SELECT COUNT(*) FROM sorteio WHERE grupo_id = :grupoId")
    abstract suspend fun contarPorGrupo(grupoId: Int): Int

    /**
     * Salva sorteio completo atomicamente:
     * 1. Insere evento de sorteio com timestamp
     * 2. Insere todos os pares (sorteio_par) com snapshots de nomes
     * 3. Atualiza amigo_sorteado_id em participante para cada par
     */
    @Transaction
    open suspend fun salvarSorteioCompleto(
        grupoId: Int,
        participantes: List<Participante>,
        sorteados: List<Participante>,
    ): Long {
        val dataHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sorteio = Sorteio(grupoId = grupoId, dataHora = dataHora)
        val sorteioId = inserirSorteio(sorteio).toInt()

        participantes.forEachIndexed { i, participante ->
            val sorteado = sorteados[i]
            inserirPar(SorteioPar(
                sorteioId = sorteioId,
                participanteId = participante.id,
                sorteadoId = sorteado.id,
                nomeParticipante = participante.nome ?: "",
                nomeSorteado = sorteado.nome ?: "",
                enviado = false,
            ))
            atualizarAmigoSorteado(participante.id, sorteado.id)
        }

        return sorteioId.toLong()
    }

    /**
     * Carrega sorteios de um grupo com seus pares populados.
     * Dois passes para evitar N+1: (1) sorteios, (2) todos os pares em batch por sorteio_id.
     */
    @Transaction
    open suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> {
        val sorteios = listarEventosPorGrupo(grupoId)
        sorteios.forEach { s ->
            s.pares = listarParesPorSorteio(s.id)
        }
        return sorteios
    }

    /**
     * Busca o sorteio mais recente de um grupo com pares populados.
     */
    @Transaction
    open suspend fun buscarUltimoPorGrupo(grupoId: Int): Sorteio? {
        val sorteio = buscarUltimoEventoPorGrupo(grupoId) ?: return null
        sorteio.pares = listarParesPorSorteio(sorteio.id)
        return sorteio
    }
}
