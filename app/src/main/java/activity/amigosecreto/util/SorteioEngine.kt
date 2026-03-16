package activity.amigosecreto.util

import activity.amigosecreto.db.Participante
import androidx.annotation.VisibleForTesting
import java.util.Random

/**
 * Motor de sorteio do Amigo Secreto.
 * Classe utilitaria isolada para facilitar testes unitarios sem dependencia de Android.
 *
 * O algoritmo e guloso: processa participantes em ordem e escolhe aleatoriamente
 * dentre os disponiveis validos. Pode retornar null mesmo quando uma solucao existe
 * (ex: escolha inicial bloqueou os ultimos participantes). O chamador deve tentar
 * multiplas vezes (ex: ate 100) para obter um resultado valido.
 */
object SorteioEngine {

    @JvmStatic
    fun tentarSorteio(participantes: List<Participante>): List<Participante>? =
        tentarSorteio(participantes, Random())

    @VisibleForTesting
    internal fun tentarSorteio(participantes: List<Participante>, random: Random): List<Participante>? {
        val disponiveis = participantes.toMutableList()
        val resultado = mutableListOf<Participante>()

        for (atual in participantes) {
            val possiveis = disponiveis.filter { p ->
                p.id != atual.id && !atual.idsExcluidos.contains(p.id)
            }
            if (possiveis.isEmpty()) return null
            val sorteado = possiveis[random.nextInt(possiveis.size)]
            resultado.add(sorteado)
            disponiveis.remove(sorteado)
        }
        return resultado
    }
}
