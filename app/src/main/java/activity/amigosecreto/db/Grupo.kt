package activity.amigosecreto.db

import java.io.Serializable

// var fields: GrupoDAO populates via setters after construction (see CLAUDE.md § Model layer decisions).
// TODO(fase10-dao): switch to val + constructor injection once GrupoDAO migrates to Room.
// Plain class (not data class): reference equality — safe in all collection types.
// toString() returns "" for null nome; Java returned null. Production uses getNome() directly.
class Grupo(
    var id: Int = 0,
    var nome: String? = null,
    var data: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
