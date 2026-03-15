package activity.amigosecreto.util

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitários para manipulação de Window Insets em modo Edge-to-Edge.
 *
 * Centraliza o locale pt-BR e o handler de IME para evitar duplicação
 * e garantir consistência entre as Activities de desejo.
 */
object WindowInsetsUtils {

    /**
     * Locale pt-BR compartilhado para formatação monetária.
     * Uso de Locale.forLanguageTag em vez do construtor deprecated new Locale(String, String).
     */
    @JvmField
    val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /**
     * Aplica padding dinâmico no bottom de uma View quando o teclado (IME) abre.
     * Garante que botões abaixo de ScrollView permaneçam visíveis sem fechar o teclado.
     *
     * Captura o padBottom original em onCreate (antes de qualquer inset) e soma
     * o inset do IME ou da system bar, prevalecendo o maior.
     *
     * @param scrollView View que receberá o padding ajustado (geralmente NestedScrollView)
     */
    @JvmStatic
    fun applyImeBottomPadding(scrollView: View?) {
        if (scrollView == null) return
        val padBottomBase = scrollView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, insets ->
            val ime: Insets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = maxOf(ime.bottom, systemBars.bottom)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, padBottomBase + bottomInset)
            insets
        }
    }

    /**
     * Retorna um NumberFormat de moeda pt-BR (ex: "R$ 1.000,00").
     * Cria uma nova instância a cada chamada — NumberFormat não é thread-safe.
     */
    @JvmStatic
    fun currencyFormatPtBr(): NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_PT_BR)

    /**
     * Retorna um NumberFormat numérico pt-BR com 2 casas decimais fixas (ex: "1.000,00").
     * Usado em contextos onde o símbolo "R$" já está presente como prefixo no layout.
     * Cria uma nova instância a cada chamada — NumberFormat não é thread-safe.
     */
    @JvmStatic
    fun numberFormatPtBr(): NumberFormat = NumberFormat.getNumberInstance(LOCALE_PT_BR).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}
