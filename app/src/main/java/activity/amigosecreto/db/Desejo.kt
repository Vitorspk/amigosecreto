package activity.amigosecreto.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// var fields preserved intentionally: Java DAOs populate instances via setters
// (e.g. GrupoDAO.cursorToDesejo sets each field after construction).
// Switching to val would break all DAO call sites.
// WARNING: Do NOT use Desejo instances as Map/Set keys. data class equals/hashCode
// are based on field values; mutating fields after insertion corrupts collections.
@Parcelize
data class Desejo @JvmOverloads constructor(
    var id: Int = 0,
    var produto: String? = null,
    var categoria: String? = null,
    var lojas: String? = null,
    var precoMinimo: Double = 0.0,
    var precoMaximo: Double = 0.0,
    var participanteId: Int = 0,
) : Parcelable, Comparable<Desejo> {

    override fun compareTo(other: Desejo): Int = id.compareTo(other.id)

    override fun toString(): String = "[id=$id;produto=$produto;]"
}
