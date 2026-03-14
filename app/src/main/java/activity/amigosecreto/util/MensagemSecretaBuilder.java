package activity.amigosecreto.util;

import java.util.List;

import activity.amigosecreto.db.Desejo;

/**
 * Constrói a mensagem de amigo secreto enviada via SMS/WhatsApp.
 *
 * Classe pura — sem dependência de Android/Context — para facilitar testes unitários.
 */
public class MensagemSecretaBuilder {

    private MensagemSecretaBuilder() {}

    /**
     * Gera a mensagem de amigo secreto formatada para compartilhamento.
     *
     * @param nomeParticipante nome de quem recebe a mensagem (quem tirou o amigo)
     * @param nomeAmigo        nome do amigo secreto sorteado
     * @param desejos          lista de desejos do amigo (pode ser null ou vazia)
     * @return mensagem formatada pronta para SMS/WhatsApp
     */
    public static String gerar(String nomeParticipante, String nomeAmigo, List<Desejo> desejos) {
        if (nomeParticipante == null) nomeParticipante = "???";
        if (nomeAmigo == null) nomeAmigo = "???";
        StringBuilder sb = new StringBuilder();
        sb.append("🎁 Ola, *").append(nomeParticipante).append("*!\n\n");
        sb.append("Voce foi sorteado(a) no *Amigo Secreto* e vai presentear alguem especial!\n\n");
        sb.append("Seu Amigo Secreto e:\n");
        sb.append("*").append(nomeAmigo).append("* 🎉\n\n");
        if (desejos != null && !desejos.isEmpty()) {
            StringBuilder desejosBuilder = new StringBuilder();
            int num = 1;
            for (Desejo d : desejos) {
                if (d.getProduto() == null || d.getProduto().trim().isEmpty()) continue;
                desejosBuilder.append(num++).append(". ").append(d.getProduto());
                if (d.getCategoria() != null && !d.getCategoria().trim().isEmpty()) {
                    desejosBuilder.append(" (").append(d.getCategoria()).append(")");
                }
                // Logica de faixa de preco: exibe apenas quando os valores sao validos.
                // Se min > max (faixa invalida), prioriza o maximo cadastrado ("ate R$ max"),
                // ignorando o min inconsistente — comportamento intencional para nao omitir
                // o maximo que o usuario cadastrou mesmo com dados incoerentes.
                if (d.getPrecoMinimo() > 0 && d.getPrecoMaximo() >= d.getPrecoMinimo()) {
                    desejosBuilder.append(" - R$ ").append(formatarPreco(d.getPrecoMinimo()))
                      .append(" a R$ ").append(formatarPreco(d.getPrecoMaximo()));
                } else if (d.getPrecoMaximo() > 0) {
                    desejosBuilder.append(" - ate R$ ").append(formatarPreco(d.getPrecoMaximo()));
                } else if (d.getPrecoMinimo() > 0) {
                    desejosBuilder.append(" - a partir de R$ ").append(formatarPreco(d.getPrecoMinimo()));
                }
                if (d.getLojas() != null && !d.getLojas().trim().isEmpty()) {
                    desejosBuilder.append(" 🏪 ").append(d.getLojas());
                }
                desejosBuilder.append("\n");
            }
            if (desejosBuilder.length() > 0) {
                sb.append("🛍️ *Lista de desejos de ").append(nomeAmigo).append(":*\n");
                sb.append(desejosBuilder);
                sb.append("\n");
            }
        }
        sb.append("Lembre-se: o segredo é seu! Não conte para ninguém. 🤫");
        return sb.toString();
    }

    public static String formatarPreco(double valor) {
        return WindowInsetsUtils.numberFormatPtBr().format(valor);
    }
}
