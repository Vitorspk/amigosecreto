package activity.amigosecreto.util

import activity.amigosecreto.R
import activity.amigosecreto.db.Desejo
import android.content.Context

/**
 * Constrói a mensagem de amigo secreto enviada via SMS/WhatsApp.
 *
 * Classe pura — sem dependência de Android/Context — para facilitar testes unitários.
 *
 * As strings da mensagem são passadas como [Strings] para suportar múltiplos idiomas (i18n).
 * O call site (Activity/ViewModel) obtém as strings via Context.getString() antes de chamar [gerar].
 */
object MensagemSecretaBuilder {

    /**
     * Strings localizadas necessárias para montar a mensagem.
     * Em produção, instanciar via [from] passando um Context.
     * Para testes unitários, usar [ptBr] como padrão.
     */
    data class Strings(
        val greeting: String,
        val intro: String,
        val amigoLabel: String,
        val wishlistHeader: String,
        val farewell: String,
        val priceRange: String,
        val priceUpTo: String,
        val priceFrom: String
    ) {
        companion object {
            /** Valores padrão em pt-BR — usado em testes unitários (sem Context). */
            fun ptBr() = Strings(
                greeting = "🎁 Ola, *%1\$s*!\n\n",
                intro = "Voce foi sorteado(a) no *Amigo Secreto* e vai presentear alguem especial!\n\n",
                amigoLabel = "Seu Amigo Secreto e:\n",
                wishlistHeader = "🛍️ *Lista de desejos de %1\$s:*\n",
                farewell = "Lembre-se: o segredo é seu! Não conte para ninguém. 🤫",
                priceRange = " - R$ %1\$s a R$ %2\$s",
                priceUpTo = " - ate R$ %1\$s",
                priceFrom = " - a partir de R$ %1\$s"
            )

            /** Cria a partir de strings localizadas do Context (produção). */
            fun from(ctx: Context): Strings {
                return Strings(
                    greeting = ctx.getString(R.string.share_msg_greeting),
                    intro = ctx.getString(R.string.share_msg_intro),
                    amigoLabel = ctx.getString(R.string.share_msg_amigo_label),
                    wishlistHeader = ctx.getString(R.string.share_msg_wishlist_header),
                    farewell = ctx.getString(R.string.share_msg_farewell),
                    priceRange = ctx.getString(R.string.share_msg_price_range),
                    priceUpTo = ctx.getString(R.string.share_msg_price_up_to),
                    priceFrom = ctx.getString(R.string.share_msg_price_from)
                )
            }
        }
    }

    @JvmStatic
    fun gerar(
        nomeParticipante: String?,
        nomeAmigo: String?,
        desejos: List<Desejo>?,
        strings: Strings = Strings.ptBr()
    ): String {
        val nome = nomeParticipante ?: "???"
        val amigo = nomeAmigo ?: "???"
        val sb = StringBuilder()
        sb.append(String.format(strings.greeting, nome))
        sb.append(strings.intro)
        sb.append(strings.amigoLabel)
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
                // Se min > max (faixa invalida), prioriza o maximo cadastrado ("ate max"),
                // ignorando o min inconsistente — comportamento intencional para nao omitir
                // o maximo que o usuario cadastrou mesmo com dados incoerentes.
                val minFormatado = formatarPreco(d.precoMinimo)
                val maxFormatado = formatarPreco(d.precoMaximo)
                when {
                    d.precoMinimo > 0 && d.precoMaximo >= d.precoMinimo ->
                        desejosBuilder.append(String.format(strings.priceRange, minFormatado, maxFormatado))
                    d.precoMaximo > 0 ->
                        desejosBuilder.append(String.format(strings.priceUpTo, maxFormatado))
                    d.precoMinimo > 0 ->
                        desejosBuilder.append(String.format(strings.priceFrom, minFormatado))
                }
                val lojas = d.lojas?.trim()
                if (!lojas.isNullOrEmpty()) {
                    desejosBuilder.append(" 🏪 ").append(lojas)
                }
                desejosBuilder.append("\n")
            }
            if (desejosBuilder.isNotEmpty()) {
                sb.append(String.format(strings.wishlistHeader, amigo))
                sb.append(desejosBuilder)
                sb.append("\n")
            }
        }
        sb.append(strings.farewell)
        return sb.toString()
    }

    // Visivel para testes unitarios (FormatarPrecoTest). numberFormatPtBr() usa apenas
    // java.text.NumberFormat + java.util.Locale — sem dependencia de Android/Context.
    // TODO i18n: mover LOCALE_PT_BR e numberFormatPtBr() de WindowInsetsUtils para FormatUtils;
    //  após a mudança, formatarPreco() deve receber o Locale do dispositivo para suportar
    //  moedas diferentes do R$ nas Fases 2/3 (en-US → USD, es-ES → EUR, etc.).
    @JvmStatic
    fun formatarPreco(valor: Double): String = WindowInsetsUtils.numberFormatPtBr().format(valor)
}
