package activity.amigosecreto.db

import java.io.Serializable

// Serializable (not Parcelable): Participante is passed via Intent extras as Serializable.
// Plain class (not data class): reference equality is required by ParticipanteKotlinMigrationTest
// contracts — two Participante objects with the same fields must NOT be equal.
// idsExcluidos uses mutableListOf() — each instance gets its own independent list (no sharing).
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
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String = nome ?: ""
}
