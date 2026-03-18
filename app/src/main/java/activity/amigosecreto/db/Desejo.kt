package activity.amigosecreto.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// equals/hashCode use id only — identity = DB primary key, not field values.
// Secondary constructor preserved for test call sites (Desejo(id, produto)).
@Parcelize
@Entity(
    tableName = "desejo",
    foreignKeys = [ForeignKey(
        entity = Participante::class,
        parentColumns = ["id"],
        childColumns = ["participante_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
class Desejo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "produto")
    var produto: String? = null,
    @ColumnInfo(name = "categoria")
    var categoria: String? = null,
    @ColumnInfo(name = "lojas")
    var lojas: String? = null,
    @ColumnInfo(name = "preco_minimo")
    var precoMinimo: Double = 0.0,
    @ColumnInfo(name = "preco_maximo")
    var precoMaximo: Double = 0.0,
    @ColumnInfo(name = "participante_id")
    var participanteId: Int = 0,
) : Parcelable, Comparable<Desejo> {

    // Secondary constructor preserving the Desejo(id, produto) call site in tests.
    @Ignore
    constructor(id: Int, produto: String?) : this(
        id = id, produto = produto, categoria = null, lojas = null,
        precoMinimo = 0.0, precoMaximo = 0.0, participanteId = 0,
    )

    override fun equals(other: Any?): Boolean = other is Desejo && id == other.id

    override fun hashCode(): Int = id

    override fun compareTo(other: Desejo): Int = id.compareTo(other.id)

    override fun toString(): String = "[id=$id;produto=$produto;]"
}
