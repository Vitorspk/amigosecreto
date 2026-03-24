package activity.amigosecreto.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

// Plain class (not data class): reference equality required by ParticipanteKotlinMigrationTest contracts.
// toString() returns "" for null nome; Java returned null.
// idsExcluidos is @Ignore — populated by a separate JOIN query in ParticipanteRoomDao,
// not stored in the participante table.
@Entity(
    tableName = "participante",
    foreignKeys = [ForeignKey(
        entity = Grupo::class,
        parentColumns = ["id"],
        childColumns = ["grupo_id"],
    )]
)
class Participante(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "nome")
    var nome: String? = null,
    @ColumnInfo(name = "email")
    var email: String? = null,
    @ColumnInfo(name = "telefone")
    var telefone: String? = null,
    @ColumnInfo(name = "amigo_sorteado_id")
    var amigoSorteadoId: Int? = null,
    @ColumnInfo(name = "enviado")
    var isEnviado: Boolean = false,
    @ColumnInfo(name = "grupo_id")
    var grupoId: Int = 0,
    @ColumnInfo(name = "confirmou_presente", defaultValue = "0")
    var confirmouPresente: Boolean = false,
    @ColumnInfo(name = "foi_notificado", defaultValue = "0")
    var foiNotificado: Boolean = false,
    @ColumnInfo(name = "observacoes")
    var observacoes: String? = null,
) : Serializable {

    // codigoAcesso: orphaned field — no DB column in any schema version. @Ignore so Room skips it.
    @Ignore
    var codigoAcesso: String? = null

    // idsExcluidos: transient field, populated by JOIN query. Not stored in participante table.
    // Defensive copy prevents UnsupportedOperationException from fixed-size java.util.Arrays.asList().
    @Ignore
    var idsExcluidos: MutableList<Int> = mutableListOf()
        set(value) { field = ArrayList(value) }

    // Secondary constructor for backward compat with existing callers that don't pass grupoId
    @Ignore
    constructor(
        id: Int = 0,
        nome: String? = null,
        email: String? = null,
        telefone: String? = null,
        amigoSorteadoId: Int? = null,
        isEnviado: Boolean = false,
        codigoAcesso: String? = null,
        idsExcluidos: MutableList<Int> = mutableListOf(),
    ) : this(id, nome, email, telefone, amigoSorteadoId, isEnviado, 0) {
        this.codigoAcesso = codigoAcesso
        this.idsExcluidos = ArrayList(idsExcluidos)
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
