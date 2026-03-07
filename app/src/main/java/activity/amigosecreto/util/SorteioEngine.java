package activity.amigosecreto.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import activity.amigosecreto.db.Participante;

/**
 * Motor de sorteio do Amigo Secreto.
 * Classe isolada para facilitar testes unitarios sem dependencia de Android.
 */
public class SorteioEngine {

    /**
     * Tenta realizar um sorteio valido respeitando as regras:
     * - Ninguem tira a si mesmo
     * - Ninguem tira quem esta na sua lista de excluidos
     *
     * @param participantes lista de participantes (nao modificada)
     * @return lista de sorteados na mesma ordem dos participantes, ou null se impossivel
     */
    public static List<Participante> tentarSorteio(List<Participante> participantes) {
        List<Participante> disponiveis = new ArrayList<>(participantes);
        List<Participante> resultado = new ArrayList<>();
        Random random = new Random();

        for (Participante atual : participantes) {
            List<Participante> possiveis = new ArrayList<>();
            for (Participante p : disponiveis) {
                if (p.getId() != atual.getId() && !atual.getIdsExcluidos().contains(p.getId())) {
                    possiveis.add(p);
                }
            }
            if (possiveis.isEmpty()) return null;
            Participante sorteado = possiveis.get(random.nextInt(possiveis.size()));
            resultado.add(sorteado);
            disponiveis.remove(sorteado);
        }
        return resultado;
    }
}
