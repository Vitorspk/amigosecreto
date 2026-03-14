package activity.amigosecreto.db

import java.io.Serializable

// var fields preserved intentionally: GrupoDAO populates instances via setters
// after construction. Switching to val would break all DAO call sites.
// WARNING: Do NOT use Grupo instances as Map/Set keys. data class equals/hashCode
// are based on field values; mutating fields after insertion corrupts collections.
// Serializable (not Parcelable): Grupo is passed between Activities via Intent extras
// as Serializable — no Parcel dependency needed for a simple data holder.
data class Grupo(
    var id: Int = 0,
    var nome: String? = null,
    var data: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    // Returns "" for null nome — Kotlin String cannot be null.
    // Java toString() returned null; production code uses getNome() directly,
    // so this change has no impact on UI call sites.
    override fun toString(): String = nome ?: ""
}
