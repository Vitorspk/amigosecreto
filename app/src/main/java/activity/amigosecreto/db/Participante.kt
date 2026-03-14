package activity.amigosecreto.db

import java.io.Serializable

// Serializable (not Parcelable): Participante is passed via Intent extras as Serializable.
// Plain class (not data class): reference equality is required by ParticipanteKotlinMigrationTest
// contracts — two Participante objects with the same fields must NOT be equal.
// idsExcluidos uses mutableListOf() — each instance gets its own independent list (no sharing).
// The body property + custom setter defensively copy on both construction and setIdsExcluidos()
// calls, so Java callers using Arrays.asList can safely mutate the list afterward.
// No-arg constructor: Kotlin generates one automatically because all parameters have defaults.
// @JvmOverloads omitted — plain class with only the no-arg constructor needed by Java call sites.
class Participante(
    var id: Int = 0,
    var nome: String? = null,
    var email: String? = null,
    var telefone: String? = null,
    var amigoSorteadoId: Int? = null,
    var isEnviado: Boolean = false,
    var codigoAcesso: String? = null,
    idsExcluidos: MutableList<Int> = mutableListOf(),
) : Serializable {
    // Body property with custom setter: defensive ArrayList copy on construction and on every
    // setIdsExcluidos() call. Prevents UnsupportedOperationException when Java passes
    // Arrays.asList(...) (fixed-size) and later calls add() or remove() on the result.
    var idsExcluidos: MutableList<Int> = ArrayList(idsExcluidos)
        set(value) { field = ArrayList(value) }

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
