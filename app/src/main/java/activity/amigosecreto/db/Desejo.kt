package activity.amigosecreto.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// var fields preserved intentionally: Java DAOs populate instances via setters
// (e.g. GrupoDAO.cursorToDesejo sets each field after construction).
// Switching to val would break all DAO call sites.
// WARNING: Do NOT use Desejo instances as Map/Set keys or TreeSet/TreeMap keys.
// data class equals/hashCode are field-based — mutating after insertion corrupts HashMap/HashSet.
// compareTo uses only id — two objects with the same id but different fields compare as equal
// in a TreeSet, silently dropping one.
// TODO(fase10-dao): switch to val fields once DesejoDAO is migrated to Kotlin/Room —
// at that point, constructor injection replaces setter-based population.
// @JvmOverloads generates 8 constructor overloads (one per defaulted parameter). The only
// call sites in production are Desejo() (no-arg, used by DAOs) and Desejo(int, String)
// (used in legacy Java tests); the remaining 6 overloads are unused but harmless.
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
