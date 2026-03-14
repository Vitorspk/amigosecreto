package activity.amigosecreto.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// var fields preserved intentionally: Java DAOs populate instances via setters
// (e.g. GrupoDAO.cursorToDesejo sets each field after construction).
// Switching to val would break all DAO call sites.
@Parcelize
data class Desejo(
    var id: Int = 0,
    var produto: String? = null,
    var categoria: String? = null,
    var lojas: String? = null,
    var precoMinimo: Double = 0.0,
    var precoMaximo: Double = 0.0,
    var participanteId: Int = 0,
) : Parcelable, Comparable<Desejo> {

    // Secondary constructor preserving the Java Desejo(int id, String produto) call site
    constructor(id: Int, produto: String?) : this(
        id = id, produto = produto,
        categoria = null, lojas = null,
        precoMinimo = 0.0, precoMaximo = 0.0,
        participanteId = 0,
    )

    override fun compareTo(other: Desejo): Int = id.compareTo(other.id)

    override fun toString(): String = "[id=$id;produto=$produto;]"
}
