package activity.amigosecreto.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Evento de sorteio: representa uma rodada de sorteio realizada em um grupo.
 * Cada sorteio tem sua data/hora e uma lista de pares (quem tirou quem) em [SorteioPar].
 * [pares] é transient (@Ignore) — populado por JOIN query em SorteioRoomDao.
 */
@Entity(
    tableName = "sorteio",
    foreignKeys = [ForeignKey(
        entity = Grupo::class,
        parentColumns = ["id"],
        childColumns = ["grupo_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
class Sorteio(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "grupo_id")
    var grupoId: Int = 0,
    @ColumnInfo(name = "data_hora")
    var dataHora: String = "",
) : Serializable {

    @Ignore
    var pares: List<SorteioPar> = emptyList()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Par de um sorteio: registra que [nomeParticipante] tirou [nomeSorteado].
 * Os nomes são snapshotados no momento do sorteio para preservar integridade histórica
 * mesmo que os participantes sejam removidos posteriormente.
 */
@Entity(
    tableName = "sorteio_par",
    primaryKeys = ["sorteio_id", "participante_id"],
    foreignKeys = [ForeignKey(
        entity = Sorteio::class,
        parentColumns = ["id"],
        childColumns = ["sorteio_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
class SorteioPar(
    @ColumnInfo(name = "sorteio_id")
    var sorteioId: Int = 0,
    @ColumnInfo(name = "participante_id")
    var participanteId: Int = 0,
    @ColumnInfo(name = "sorteado_id")
    var sorteadoId: Int = 0,
    @ColumnInfo(name = "nome_participante")
    var nomeParticipante: String = "",
    @ColumnInfo(name = "nome_sorteado")
    var nomeSorteado: String = "",
    @ColumnInfo(name = "enviado")
    var enviado: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
