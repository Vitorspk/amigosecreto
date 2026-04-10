package activity.amigosecreto.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitários de formatação numérica, monetária e de datas.
 *
 * Extraído de WindowInsetsUtils (que misturava insets de janela com formatação).
 * WindowInsetsUtils mantém delegates para retrocompatibilidade, mas novos call sites
 * devem usar FormatUtils diretamente.
 */
object FormatUtils {

    @JvmField
    val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** Formato monetário brasileiro: R$ 1.234,56. Cria nova instância a cada chamada — NumberFormat não é thread-safe. */
    @JvmStatic
    fun currencyFormatPtBr(): NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_PT_BR)

    /** Formato numérico brasileiro com 2 casas decimais: 1.234,56. Cria nova instância a cada chamada — NumberFormat não é thread-safe. */
    @JvmStatic
    fun numberFormatPtBr(): NumberFormat = NumberFormat.getNumberInstance(LOCALE_PT_BR).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Formata uma data como string usando o padrão curto (e.g. dd/MM/yyyy ou MM/dd/yyyy)
     * de acordo com o [pattern] fornecido (obtido via R.string.date_format_short do Context).
     * Usa [Locale.getDefault] para respeitar o idioma do dispositivo.
     */
    @JvmStatic
    fun formatDate(date: Date, pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)

    /**
     * Formata uma string de data ISO (yyyy-MM-dd'T'HH:mm:ss) para o padrão de exibição
     * fornecido em [displayPattern] (e.g. "dd/MM/yyyy 'às' HH:mm" ou localizado).
     * Retorna [input] original se o parse falhar.
     */
    @JvmStatic
    fun formatIsoDateTime(input: String, displayPattern: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdfIn.parse(input) ?: return input
            SimpleDateFormat(displayPattern, Locale.getDefault()).format(date)
        } catch (e: Exception) {
            input
        }
    }
}
