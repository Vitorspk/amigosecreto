package activity.amigosecreto;

import org.junit.Test;

import activity.amigosecreto.util.MensagemSecretaBuilder;

import static org.junit.Assert.*;

public class FormatarPrecoTest {

    // --- valores inteiros --- (agora sempre 2 casas decimais com NumberFormat pt-BR)

    @Test
    public void inteiro_zero_retorna_formatado() {
        assertEquals("0,00", MensagemSecretaBuilder.formatarPreco(0.0));
    }

    @Test
    public void inteiro_positivo_com_duas_casas() {
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(100.0));
    }

    @Test
    public void inteiro_com_separador_de_milhar() {
        assertEquals("1.500,00", MensagemSecretaBuilder.formatarPreco(1500.0));
    }

    @Test
    public void valor_grande_com_separador_de_milhar() {
        assertEquals("1.000.000,00", MensagemSecretaBuilder.formatarPreco(1000000.0));
    }

    // --- valores com centavos ---

    @Test
    public void centavos_usa_virgula_como_separador() {
        assertEquals("19,99", MensagemSecretaBuilder.formatarPreco(19.99));
    }

    @Test
    public void centavos_com_zero_significativo() {
        assertEquals("10,50", MensagemSecretaBuilder.formatarPreco(10.5));
    }

    @Test
    public void centavos_pequenos() {
        assertEquals("0,99", MensagemSecretaBuilder.formatarPreco(0.99));
    }

    @Test
    public void milhar_com_centavos() {
        assertEquals("2.500,50", MensagemSecretaBuilder.formatarPreco(2500.5));
    }

    // --- edge cases IEEE 754 ---

    @Test
    public void ieee754_arredonda_para_duas_casas() {
        // NumberFormat arredonda automaticamente para 2 casas
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(100.0000000001));
    }

    @Test
    public void ieee754_logo_abaixo_de_inteiro_arredonda() {
        assertEquals("100,00", MensagemSecretaBuilder.formatarPreco(99.9999999999));
    }

    @Test
    public void valor_com_centavos_reais_nao_e_arredondado_para_inteiro() {
        assertNotEquals("20,00", MensagemSecretaBuilder.formatarPreco(19.99));
        assertEquals("19,99", MensagemSecretaBuilder.formatarPreco(19.99));
    }

    @Test
    public void centavo_minimo() {
        assertEquals("0,01", MensagemSecretaBuilder.formatarPreco(0.01));
    }
}
