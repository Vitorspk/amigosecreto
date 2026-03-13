package activity.amigosecreto.util;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitários para manipulação de Window Insets em modo Edge-to-Edge.
 *
 * Centraliza o locale pt-BR e o handler de IME para evitar duplicação
 * e garantir consistência entre as Activities de desejo.
 */
public final class WindowInsetsUtils {

    /**
     * Locale pt-BR compartilhado para formatação monetária.
     * Uso de Locale.forLanguageTag em vez do construtor deprecated new Locale(String, String).
     */
    public static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");

    private WindowInsetsUtils() {}

    /**
     * Aplica padding dinâmico no bottom de uma View quando o teclado (IME) abre.
     * Garante que botões abaixo de ScrollView permaneçam visíveis sem fechar o teclado.
     *
     * Captura o padBottom original em onCreate (antes de qualquer inset) e soma
     * o inset do IME ou da system bar, prevalecendo o maior.
     *
     * @param scrollView View que receberá o padding ajustado (geralmente NestedScrollView)
     */
    public static void applyImeBottomPadding(View scrollView) {
        if (scrollView == null) return;
        final int padBottomBase = scrollView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomInset = Math.max(ime.bottom, systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    padBottomBase + bottomInset);
            return insets;
        });
    }

    /**
     * Retorna um NumberFormat de moeda pt-BR (ex: "R$ 1.000,00").
     * Cria uma nova instância a cada chamada — NumberFormat não é thread-safe.
     */
    public static NumberFormat currencyFormatPtBr() {
        return NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
    }

    /**
     * Retorna um NumberFormat numérico pt-BR com 2 casas decimais fixas (ex: "1.000,00").
     * Usado em contextos onde o símbolo "R$" já está presente como prefixo no layout.
     * Cria uma nova instância a cada chamada — NumberFormat não é thread-safe.
     */
    public static NumberFormat numberFormatPtBr() {
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf;
    }
}
