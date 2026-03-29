package activity.amigosecreto.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WindowInsetsUtilsTest {

    // =========================================================
    // LOCALE_PT_BR
    // =========================================================

    @Test
    fun localePtBr_language_e_pt() {
        assertEquals("pt", WindowInsetsUtils.LOCALE_PT_BR.language)
    }

    @Test
    fun localePtBr_country_e_BR() {
        assertEquals("BR", WindowInsetsUtils.LOCALE_PT_BR.country)
    }

    @Test
    fun localePtBr_nao_e_null() {
        assertNotNull(WindowInsetsUtils.LOCALE_PT_BR)
    }

    // =========================================================
    // currencyFormatPtBr
    // =========================================================

    @Test
    fun currencyFormatPtBr_formata_valor_inteiro() {
        val fmt = WindowInsetsUtils.currencyFormatPtBr()
        val resultado = fmt.format(1000.0)
        // Deve conter o separador de milhar (ponto) e símbolo de moeda
        assertTrue("Esperava '1.000' em '$resultado'", resultado.contains("1.000"))
    }

    @Test
    fun currencyFormatPtBr_formata_valor_com_centavos() {
        val fmt = WindowInsetsUtils.currencyFormatPtBr()
        val resultado = fmt.format(9.99)
        assertTrue("Esperava '9,99' em '$resultado'", resultado.contains("9,99"))
    }

    @Test
    fun currencyFormatPtBr_usa_virgula_como_separador_decimal() {
        val fmt = WindowInsetsUtils.currencyFormatPtBr()
        val resultado = fmt.format(1.5)
        assertTrue("Separador decimal deve ser vírgula em '$resultado'", resultado.contains(","))
        assertFalse("Não deve usar ponto como decimal em '$resultado'", resultado.matches(Regex(".*\\d\\.\\d.*")))
    }

    @Test
    fun currencyFormatPtBr_retorna_nova_instancia_a_cada_chamada() {
        val fmt1 = WindowInsetsUtils.currencyFormatPtBr()
        val fmt2 = WindowInsetsUtils.currencyFormatPtBr()
        assertNotSame(fmt1, fmt2)
    }

    @Test
    fun currencyFormatPtBr_formata_zero() {
        val fmt = WindowInsetsUtils.currencyFormatPtBr()
        val resultado = fmt.format(0.0)
        assertTrue("Esperava '0,00' em '$resultado'", resultado.contains("0,00"))
    }

    @Test
    fun currencyFormatPtBr_formata_valor_grande() {
        val fmt = WindowInsetsUtils.currencyFormatPtBr()
        val resultado = fmt.format(1_000_000.0)
        assertTrue("Esperava '1.000.000' em '$resultado'", resultado.contains("1.000.000"))
    }

    // =========================================================
    // numberFormatPtBr
    // =========================================================

    @Test
    fun numberFormatPtBr_formata_valor_inteiro_com_duas_casas() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        val resultado = fmt.format(1000.0)
        assertTrue("Esperava '1.000,00' em '$resultado'", resultado.contains("1.000,00"))
    }

    @Test
    fun numberFormatPtBr_formata_valor_com_centavos() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        val resultado = fmt.format(9.99)
        assertEquals("9,99", resultado)
    }

    @Test
    fun numberFormatPtBr_arredonda_para_duas_casas_decimais() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        val resultado = fmt.format(1.005)
        // 1.005 arredondado para 2 casas → "1,01" ou "1,00" dependendo do modo de arredondamento
        assertTrue(resultado == "1,01" || resultado == "1,00")
    }

    @Test
    fun numberFormatPtBr_usa_virgula_como_separador_decimal() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        val resultado = fmt.format(1.5)
        assertEquals("1,50", resultado)
    }

    @Test
    fun numberFormatPtBr_formata_zero_com_duas_casas() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        assertEquals("0,00", fmt.format(0.0))
    }

    @Test
    fun numberFormatPtBr_nao_inclui_simbolo_moeda() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        val resultado = fmt.format(100.0)
        assertFalse("Não deve conter 'R$' em '$resultado'", resultado.contains("R$"))
        assertFalse("Não deve conter 'BRL' em '$resultado'", resultado.contains("BRL"))
    }

    @Test
    fun numberFormatPtBr_retorna_nova_instancia_a_cada_chamada() {
        val fmt1 = WindowInsetsUtils.numberFormatPtBr()
        val fmt2 = WindowInsetsUtils.numberFormatPtBr()
        assertNotSame(fmt1, fmt2)
    }

    @Test
    fun numberFormatPtBr_minimumFractionDigits_e_2() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        assertEquals(2, fmt.minimumFractionDigits)
    }

    @Test
    fun numberFormatPtBr_maximumFractionDigits_e_2() {
        val fmt = WindowInsetsUtils.numberFormatPtBr()
        assertEquals(2, fmt.maximumFractionDigits)
    }

    // =========================================================
    // Comparação entre currency e number format
    // =========================================================

    @Test
    fun currency_e_number_diferem_pela_presenca_do_simbolo_moeda() {
        val currency = WindowInsetsUtils.currencyFormatPtBr().format(100.0)
        val number = WindowInsetsUtils.numberFormatPtBr().format(100.0)
        assertNotEquals(currency, number)
        assertEquals("100,00", number)
        assertTrue("Currency deve conter símbolo 'R$' em '$currency'", currency.contains("R$"))
    }
}
