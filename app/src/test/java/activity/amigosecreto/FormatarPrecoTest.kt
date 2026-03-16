package activity.amigosecreto

import activity.amigosecreto.util.MensagemSecretaBuilder
import org.junit.Assert.*
import org.junit.Test

class FormatarPrecoTest {

    // --- valores inteiros --- (agora sempre 2 casas decimais com NumberFormat pt-BR)

    @Test
    fun inteiro_zero_retorna_formatado() {
        assertEquals("0,00", MensagemSecretaBuilder.formatarPreco(0.0))
    }

    @Test
    fun inteiro_positivo_com_duas_casas() {
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(100.0))
    }

    @Test
    fun inteiro_com_separador_de_milhar() {
        assertEquals("1.500,00", MensagemSecretaBuilder.formatarPreco(1500.0))
    }

    @Test
    fun valor_grande_com_separador_de_milhar() {
        assertEquals("1.000.000,00", MensagemSecretaBuilder.formatarPreco(1000000.0))
    }

    // --- valores com centavos ---

    @Test
    fun centavos_usa_virgula_como_separador() {
        assertEquals("19,99", MensagemSecretaBuilder.formatarPreco(19.99))
    }

    @Test
    fun centavos_com_zero_significativo() {
        assertEquals("10,50", MensagemSecretaBuilder.formatarPreco(10.5))
    }

    @Test
    fun centavos_pequenos() {
        assertEquals("0,99", MensagemSecretaBuilder.formatarPreco(0.99))
    }

    @Test
    fun milhar_com_centavos() {
        assertEquals("2.500,50", MensagemSecretaBuilder.formatarPreco(2500.5))
    }

    // --- edge cases IEEE 754 ---

    @Test
    fun ieee754_arredonda_para_duas_casas() {
        // NumberFormat arredonda automaticamente para 2 casas — imprecisões IEEE 754 irrelevantes.
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(100.0000000001))
    }

    @Test
    fun ieee754_logo_abaixo_de_inteiro_arredonda() {
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(99.9999999999))
    }

    @Test
    fun valor_com_centavos_reais_nao_e_arredondado_para_inteiro() {
        assertNotEquals("20,00", MensagemSecretaBuilder.formatarPreco(19.99))
        assertEquals("19,99", MensagemSecretaBuilder.formatarPreco(19.99))
    }

    @Test
    fun centavo_minimo() {
        assertEquals("0,01", MensagemSecretaBuilder.formatarPreco(0.01))
    }
}
