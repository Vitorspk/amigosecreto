package activity.amigosecreto.db

import java.io.Serializable

class Participante : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    var id: Int = 0
    var nome: String? = null
    var email: String? = null
    var telefone: String? = null
    var amigoSorteadoId: Int? = null
    var isEnviado: Boolean = false
    var codigoAcesso: String? = null
    var idsExcluidos: MutableList<Int> = mutableListOf()

    override fun toString(): String = nome ?: ""
}