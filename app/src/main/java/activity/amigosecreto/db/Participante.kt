package activity.amigosecreto.db

import java.io.Serializable

// var fields: ParticipanteDAO populates via setters after construction (see CLAUDE.md § Model layer decisions).
// TODO(fase10-dao): switch to val + constructor injection once ParticipanteDAO migrates to Room.
// Plain class (not data class): reference equality required by ParticipanteKotlinMigrationTest contracts.
// toString() returns "" for null nome; Java returned null.
class Participante(
    var id: Int = 0,
    var nome: String? = null,
    var email: String? = null,
    var telefone: String? = null,
    var amigoSorteadoId: Int? = null,
    var isEnviado: Boolean = false,
    var codigoAcesso: String? = null, // TODO: orphaned — no DB column in schema v8; add migration or remove in Fase 10
    idsExcluidos: MutableList<Int> = mutableListOf(),
) : Serializable {
    // Defensive copy on construction and on every setIdsExcluidos() call.
    // Prevents UnsupportedOperationException when Java passes Arrays.asList (fixed-size list).
    var idsExcluidos: MutableList<Int> = ArrayList(idsExcluidos)
        set(value) { field = ArrayList(value) }

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
