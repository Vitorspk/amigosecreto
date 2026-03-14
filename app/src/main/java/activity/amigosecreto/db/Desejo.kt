package activity.amigosecreto.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// var fields preserved intentionally: Java DAOs populate instances via setters
// (e.g. GrupoDAO.cursorToDesejo sets each field after construction).
// Switching to val would break all DAO call sites.
// TODO(fase10-dao): switch to val fields once DesejoDAO is migrated to Kotlin/Room —
// at that point, constructor injection replaces setter-based population.
// equals/hashCode use id only (not all fields) — safe to use in HashMap/HashSet/TreeSet
// because identity is determined by DB primary key, not by mutable field values.
// WARNING: two Desejo objects with the same id but different fields compare as equal —
// do not rely on equals for value comparison; use field accessors directly.
// @JvmOverloads generates 8 constructor overloads (one per defaulted parameter). The only
// call sites in production are Desejo() (no-arg, used by DAOs) and Desejo(int, String)
// (used in legacy Java tests); the remaining 6 overloads are unused but harmless.
@Parcelize
class Desejo @JvmOverloads constructor(
    var id: Int = 0,
    var produto: String? = null,
    var categoria: String? = null,
    var lojas: String? = null,
    var precoMinimo: Double = 0.0,
    var precoMaximo: Double = 0.0,
    var participanteId: Int = 0,
) : Parcelable, Comparable<Desejo> {

    override fun equals(other: Any?): Boolean = other is Desejo && id == other.id

    override fun hashCode(): Int = id

    override fun compareTo(other: Desejo): Int = id.compareTo(other.id)

    override fun toString(): String = "[id=$id;produto=$produto;]"
}
