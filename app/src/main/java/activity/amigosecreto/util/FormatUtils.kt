package activity.amigosecreto.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitários de formatação numérica e de localidade pt-BR.
 *
 * Extraído de WindowInsetsUtils (que misturava insets de janela com formatação).
 * WindowInsetsUtils mantém delegates para retrocompatibilidade, mas novos call sites
 * devem usar FormatUtils diretamente.
 */
object FormatUtils {

    @JvmField
    val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** Formato monetário brasileiro: R$ 1.234,56 */
    @JvmStatic
    fun currencyFormatPtBr(): NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_PT_BR)

    /** Formato numérico brasileiro com 2 casas decimais: 1.234,56 */
    @JvmStatic
    fun numberFormatPtBr(): NumberFormat = NumberFormat.getNumberInstance(LOCALE_PT_BR).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}
