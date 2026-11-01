package activity.amigosecreto.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticipanteAcoesLayoutTest {

    @Test
    fun largura_de_360dp_usa_seis_colunas() {
        assertEquals(6, ParticipanteAcoesLayout.colunas(360))
    }

    @Test
    fun larguras_acima_de_360dp_usam_seis_colunas() {
        assertEquals(6, ParticipanteAcoesLayout.colunas(411))
        assertEquals(6, ParticipanteAcoesLayout.colunas(820))
    }

    @Test
    fun largura_logo_abaixo_de_360dp_usa_tres_colunas() {
        assertEquals(3, ParticipanteAcoesLayout.colunas(359))
        assertEquals(3, ParticipanteAcoesLayout.colunas(320))
    }

    @Test
    fun largura_zero_ou_invalida_usa_tres_colunas() {
        assertEquals(3, ParticipanteAcoesLayout.colunas(0))
    }
}
