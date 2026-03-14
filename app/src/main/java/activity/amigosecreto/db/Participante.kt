package activity.amigosecreto.db

import java.io.Serializable

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
