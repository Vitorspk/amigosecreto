package activity.amigosecreto;

import org.junit.Test;
import static org.junit.Assert.*;

public class FormatarPrecoTest {

    // --- valores inteiros exatos ---

    @Test
    public void inteiro_zero_retorna_zero() {
        assertEquals("0", ParticipantesActivity.formatarPreco(0.0));
    }

    @Test
    public void inteiro_positivo_sem_centavos() {
        assertEquals("100", ParticipantesActivity.formatarPreco(100.0));
    }

    @Test
    public void inteiro_grande_sem_centavos() {
        assertEquals("1500", ParticipantesActivity.formatarPreco(1500.0));
    }

    // --- valores com centavos ---

    @Test
    public void centavos_usa_virgula_como_separador() {
        assertEquals("19,99", ParticipantesActivity.formatarPreco(19.99));
    }

    @Test
    public void centavos_com_zero_significativo() {
        assertEquals("10,50", ParticipantesActivity.formatarPreco(10.5));
    }

    @Test
    public void centavos_pequenos() {
        assertEquals("0,99", ParticipantesActivity.formatarPreco(0.99));
    }

    // --- edge cases IEEE 754 ---

    @Test
    public void ieee754_muito_proximo_de_inteiro_exibe_sem_centavos() {
        // 100.0000000001 pode surgir de arredondamento ao ler REAL do SQLite
        assertEquals("100", ParticipantesActivity.formatarPreco(100.0000000001));
    }

    @Test
    public void ieee754_logo_abaixo_de_inteiro_exibe_sem_centavos() {
        assertEquals("100", ParticipantesActivity.formatarPreco(99.9999999999));
    }

    @Test
    public void valor_com_centavos_reais_nao_e_arredondado_para_inteiro() {
        // 19.99 NAO deve virar "20"
        assertNotEquals("20", ParticipantesActivity.formatarPreco(19.99));
        assertEquals("19,99", ParticipantesActivity.formatarPreco(19.99));
    }

    @Test
    public void limiar_inferior_da_tolerancia_exibe_centavos() {
        // 0.01 esta acima da tolerancia de 0.005 — deve mostrar centavos
        assertEquals("0,01", ParticipantesActivity.formatarPreco(0.01));
    }
}
