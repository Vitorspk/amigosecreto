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

    companion object {
        // ThreadLocal para garantir thread-safety — SimpleDateFormat não é thread-safe.
        // Inicialização lazy compatível com API 21+ (ThreadLocal.withInitial requer API 26).
        private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        }
    }

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

    @androidx.room.RawQuery
    protected abstract suspend fun listarParesBatchRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<SorteioPar>

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
        require(participantes.size == sorteados.size) {
            "participantes.size (${participantes.size}) != sorteados.size (${sorteados.size})"
        }
        val dataHora = DATE_FORMAT.get()!!.format(Date())
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
     * Dois passes para evitar N+1:
     * (1) busca sorteios do grupo;
     * (2) busca todos os pares em uma única query com IN e agrupa no Kotlin.
     */
    @Transaction
    open suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> {
        val sorteios = listarEventosPorGrupo(grupoId)
        if (sorteios.isEmpty()) return sorteios
        val ids = sorteios.map { it.id }
        val placeholders = ids.joinToString(",") { "?" }
        val query = androidx.sqlite.db.SimpleSQLiteQuery(
            "SELECT * FROM sorteio_par WHERE sorteio_id IN ($placeholders)",
            ids.toTypedArray<Any?>()
        )
        val paresPorSorteio = listarParesBatchRaw(query).groupBy { it.sorteioId }
        sorteios.forEach { s -> s.pares = paresPorSorteio[s.id] ?: emptyList() }
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
