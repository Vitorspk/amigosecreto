package activity.amigosecreto.db

import java.io.Serializable

data class Grupo(
    var id: Int = 0,
    var nome: String? = null,
    var data: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String = nome ?: ""
}