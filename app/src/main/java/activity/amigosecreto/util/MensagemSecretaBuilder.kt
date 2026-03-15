package activity.amigosecreto.util

import activity.amigosecreto.db.Desejo

/**
 * Constrói a mensagem de amigo secreto enviada via SMS/WhatsApp.
 *
 * Classe pura — sem dependência de Android/Context — para facilitar testes unitários.
 */
object MensagemSecretaBuilder {

    @JvmStatic
    fun gerar(nomeParticipante: String?, nomeAmigo: String?, desejos: List<Desejo>?): String {
        val nome = nomeParticipante ?: "???"
        val amigo = nomeAmigo ?: "???"
        val sb = StringBuilder()
        sb.append("🎁 Ola, *").append(nome).append("*!\n\n")
        sb.append("Voce foi sorteado(a) no *Amigo Secreto* e vai presentear alguem especial!\n\n")
        sb.append("Seu Amigo Secreto e:\n")
        sb.append("*").append(amigo).append("* 🎉\n\n")

        if (!desejos.isNullOrEmpty()) {
            val desejosBuilder = StringBuilder()
            var num = 1
            for (d in desejos) {
                val produto = d.produto?.trim()
                if (produto.isNullOrEmpty()) continue
                desejosBuilder.append(num++).append(". ").append(produto)
                val categoria = d.categoria?.trim()
                if (!categoria.isNullOrEmpty()) {
                    desejosBuilder.append(" (").append(categoria).append(")")
                }
                // Logica de faixa de preco: exibe apenas quando os valores sao validos.
                // Se min > max (faixa invalida), prioriza o maximo cadastrado ("ate R$ max"),
                // ignorando o min inconsistente — comportamento intencional para nao omitir
                // o maximo que o usuario cadastrou mesmo com dados incoerentes.
                when {
                    d.precoMinimo > 0 && d.precoMaximo >= d.precoMinimo ->
                        desejosBuilder.append(" - R$ ").append(formatarPreco(d.precoMinimo))
                            .append(" a R$ ").append(formatarPreco(d.precoMaximo))
                    d.precoMaximo > 0 ->
                        desejosBuilder.append(" - ate R$ ").append(formatarPreco(d.precoMaximo))
                    d.precoMinimo > 0 ->
                        desejosBuilder.append(" - a partir de R$ ").append(formatarPreco(d.precoMinimo))
                }
                val lojas = d.lojas?.trim()
                if (!lojas.isNullOrEmpty()) {
                    desejosBuilder.append(" 🏪 ").append(lojas)
                }
                desejosBuilder.append("\n")
            }
            if (desejosBuilder.isNotEmpty()) {
                sb.append("🛍️ *Lista de desejos de ").append(amigo).append(":*\n")
                sb.append(desejosBuilder)
                sb.append("\n")
            }
        }
        sb.append("Lembre-se: o segredo é seu! Não conte para ninguém. 🤫")
        return sb.toString()
    }

    // Visivel para testes unitarios (FormatarPrecoTest). numberFormatPtBr() usa apenas
    // java.text.NumberFormat + java.util.Locale — sem dependencia de Android/Context.
    // TODO: mover LOCALE_PT_BR e numberFormatPtBr() para FormatUtils em refactor futuro.
    @JvmStatic
    fun formatarPreco(valor: Double): String = WindowInsetsUtils.numberFormatPtBr().format(valor)
}
