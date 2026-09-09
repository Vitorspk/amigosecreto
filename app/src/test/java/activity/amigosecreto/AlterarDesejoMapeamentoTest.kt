package activity.amigosecreto

import activity.amigosecreto.db.Desejo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de [AlterarDesejoActivity.montarDesejoAtualizado] — o mapeamento dos campos de texto
 * para o [Desejo] persistido, incluindo o parse de preço em formato pt-BR.
 *
 * Função pura, então não precisa de Robolectric nem de infraestrutura de Hilt.
 */
class AlterarDesejoMapeamentoTest {

    private fun base(id: Int = 7, participanteId: Int = 42) =
        Desejo().apply { this.id = id; this.participanteId = participanteId }

    private fun montar(
        precoMinimo: String = "",
        precoMaximo: String = "",
        produto: String = "Fone",
        categoria: String = "Eletrônicos",
        lojas: String = "Loja X",
        base: Desejo = base(),
    ) = AlterarDesejoActivity.montarDesejoAtualizado(
        base = base, produto = produto, categoria = categoria,
        precoMinimo = precoMinimo, precoMaximo = precoMaximo, lojas = lojas,
    )

    @Test
    fun preserva_id_e_participanteId_do_desejo_original() {
        // Perder o participanteId desvincularia o desejo do participante.
        val d = montar(base = base(id = 99, participanteId = 3))
        assertEquals(99, d.id)
        assertEquals(3, d.participanteId)
    }

    @Test
    fun preco_com_virgula_decimal_e_aceito() {
        val d = montar(precoMinimo = "10,50", precoMaximo = "1200,99")
        assertEquals(10.50, d.precoMinimo, 0.001)
        assertEquals(1200.99, d.precoMaximo, 0.001)
    }

    @Test
    fun preco_com_ponto_decimal_tambem_e_aceito() {
        val d = montar(precoMinimo = "10.50")
        assertEquals(10.50, d.precoMinimo, 0.001)
    }

    @Test
    fun preco_vazio_vira_zero() {
        val d = montar(precoMinimo = "", precoMaximo = "   ")
        assertEquals(0.0, d.precoMinimo, 0.001)
        assertEquals(0.0, d.precoMaximo, 0.001)
    }

    @Test
    fun espacos_ao_redor_do_preco_sao_ignorados() {
        val d = montar(precoMinimo = "  25,00  ")
        assertEquals(25.0, d.precoMinimo, 0.001)
    }

    @Test(expected = NumberFormatException::class)
    fun preco_malformado_lanca_NumberFormatException() {
        // O chamador converte isso em Toast e mantém a tela aberta.
        montar(precoMinimo = "abc")
    }

    @Test
    fun campos_de_texto_sao_trimados() {
        val d = montar(produto = "  Fone  ", categoria = "  Áudio ", lojas = "  Loja  ")
        assertEquals("Fone", d.produto)
        assertEquals("Áudio", d.categoria)
        assertEquals("Loja", d.lojas)
    }
}
