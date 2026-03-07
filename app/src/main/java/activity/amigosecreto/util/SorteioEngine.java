package activity.amigosecreto.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import activity.amigosecreto.db.Participante;

/**
 * Motor de sorteio do Amigo Secreto.
 * Classe utilitaria isolada para facilitar testes unitarios sem dependencia de Android.
 *
 * O algoritmo e guloso: processa participantes em ordem e escolhe aleatoriamente
 * dentre os disponiveis validos. Pode retornar null mesmo quando uma solucao existe
 * (ex: escolha inicial bloqueou os ultimos participantes). O chamador deve tentar
 * multiplas vezes (ex: ate 100) para obter um resultado valido.
 */
public class SorteioEngine {

    private SorteioEngine() {}

    /**
     * Tenta realizar um sorteio valido usando aleatoriedade propria.
     *
     * @param participantes lista de participantes (nao modificada)
     * @return lista de sorteados na mesma ordem dos participantes, ou null se impossivel
     */
    public static List<Participante> tentarSorteio(List<Participante> participantes) {
        return tentarSorteio(participantes, new Random());
    }

    /**
     * Tenta realizar um sorteio valido usando o Random fornecido.
     * Visibilidade de pacote para permitir testes deterministicos com seed fixa.
     *
     * @param participantes lista de participantes (nao modificada)
     * @param random        fonte de aleatoriedade
     * @return lista de sorteados na mesma ordem dos participantes, ou null se impossivel
     */
    static List<Participante> tentarSorteio(List<Participante> participantes, Random random) {
        List<Participante> disponiveis = new ArrayList<>(participantes);
        List<Participante> resultado = new ArrayList<>();

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
