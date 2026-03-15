package activity.amigosecreto.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// var fields: DAOs populate via setters after construction (see CLAUDE.md § Model layer decisions).
// TODO(fase10-dao): switch to val + constructor injection once DesejoDAO migrates to Room.
// equals/hashCode use id only — identity = DB primary key, not field values.
// Semantic change: Java compared all fields; audited — no production call site relied on that.
// Audited (all Desejo Activities): desejoDAO.remover(desejo) calls are DAO deletions, not
// List.remove() — equals is not involved. All collections are List<Desejo> iterated by index.
@Parcelize
class Desejo(
    var id: Int = 0,
    var produto: String? = null,
    var categoria: String? = null,
    var lojas: String? = null,
    var precoMinimo: Double = 0.0,
    var precoMaximo: Double = 0.0,
    var participanteId: Int = 0,
) : Parcelable, Comparable<Desejo> {

    // Secondary constructor preserving the Java Desejo(int, String) call site in tests.
    constructor(id: Int, produto: String?) : this(
        id = id, produto = produto, categoria = null, lojas = null,
        precoMinimo = 0.0, precoMaximo = 0.0, participanteId = 0,
    )

    override fun equals(other: Any?): Boolean = other is Desejo && id == other.id

    override fun hashCode(): Int = id

    override fun compareTo(other: Desejo): Int = id.compareTo(other.id)

    override fun toString(): String = "[id=$id;produto=$produto;]"
}
