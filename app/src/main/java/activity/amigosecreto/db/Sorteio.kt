package activity.amigosecreto.db

import java.io.Serializable

/**
 * Evento de sorteio: representa uma rodada de sorteio realizada em um grupo.
 * Cada sorteio tem sua data/hora e uma lista de pares (quem tirou quem) em [SorteioPar].
 */
class Sorteio : Serializable {
    var id: Int = 0
    var grupoId: Int = 0
    var dataHora: String = ""
    var pares: List<SorteioPar> = emptyList()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Par de um sorteio: registra que [nomeParticipante] tirou [nomeSorteado].
 * Os nomes são snapshotados no momento do sorteio para preservar integridade histórica
 * mesmo que os participantes sejam removidos posteriormente.
 */
class SorteioPar : Serializable {
    var sorteioId: Int = 0
    var participanteId: Int = 0
    var sorteadoId: Int = 0
    var nomeParticipante: String = ""
    var nomeSorteado: String = ""
    var enviado: Boolean = false

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
