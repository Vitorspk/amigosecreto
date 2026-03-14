package activity.amigosecreto.db

import java.io.Serializable

// Serializable (not Parcelable): Participante is passed via Intent extras as Serializable.
// Plain class (not data class): reference equality is required by ParticipanteKotlinMigrationTest
// contracts — two Participante objects with the same fields must NOT be equal.
// idsExcluidos uses mutableListOf() — each instance gets its own independent list (no sharing).
// The init block defensively copies the list so Java callers using Arrays.asList can safely mutate it.
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
    var idsExcluidos: MutableList<Int> = mutableListOf(),
) : Serializable {
    // Defensive copy: Java callers may pass fixed-size lists (e.g. Arrays.asList) whose
    // add/remove operations throw UnsupportedOperationException. Copying on set guarantees
    // the field is always a mutable ArrayList regardless of what Java passes in.
    init {
        idsExcluidos = ArrayList(idsExcluidos)
    }

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String = nome ?: ""
}
