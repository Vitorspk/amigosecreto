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
 * Formatação pt-BR foi movida para [FormatUtils]. Os campos/métodos abaixo
 * são delegates mantidos para retrocompatibilidade com call sites existentes.
 */
object WindowInsetsUtils {

    /** @see FormatUtils.LOCALE_PT_BR */
    @JvmStatic
    val LOCALE_PT_BR: Locale get() = FormatUtils.LOCALE_PT_BR

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

    /** @see FormatUtils.currencyFormatPtBr */
    @JvmStatic
    fun currencyFormatPtBr(): NumberFormat = FormatUtils.currencyFormatPtBr()

    /** @see FormatUtils.numberFormatPtBr */
    @JvmStatic
    fun numberFormatPtBr(): NumberFormat = FormatUtils.numberFormatPtBr()
}
