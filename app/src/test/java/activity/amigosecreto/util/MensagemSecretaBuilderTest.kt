package activity.amigosecreto.util

import activity.amigosecreto.db.Desejo
import org.junit.Assert.*
import org.junit.Test

class MensagemSecretaBuilderTest {

    private fun buildDesejo(produto: String?, categoria: String?, min: Double, max: Double, lojas: String?) =
        Desejo().apply {
            this.produto = produto
            this.categoria = categoria
            this.precoMinimo = min
            this.precoMaximo = max
            this.lojas = lojas
        }

    // --- saudação e estrutura básica ---

    @Test
    fun gerar_containsParticipanteName() {
        assertTrue(MensagemSecretaBuilder.gerar("Ana", "Bruno", null).contains("Ana"))
    }

    @Test
    fun gerar_containsAmigoName() {
        assertTrue(MensagemSecretaBuilder.gerar("Ana", "Bruno", null).contains("Bruno"))
    }

    @Test
    fun gerar_containsEncerramento() {
        assertTrue(MensagemSecretaBuilder.gerar("Ana", "Bruno", null).contains("segredo"))
    }

    // --- nomes nulos ---

    @Test
    fun gerar_nullParticipante_usesPlaceholder() {
        val msg = MensagemSecretaBuilder.gerar(null, "Carlos", null)
        assertTrue(msg.contains("???"))
        assertFalse(msg.contains("null"))
    }

    @Test
    fun gerar_nullAmigo_usesPlaceholder() {
        val msg = MensagemSecretaBuilder.gerar("Diana", null, null)
        assertTrue(msg.contains("???"))
        assertFalse(msg.contains("null"))
    }

    @Test
    fun gerar_nullParticipanteEAmigo_bothUsePlaceholder() {
        val msg = MensagemSecretaBuilder.gerar(null, null, null)
        var count = 0; var idx = 0
        while (msg.indexOf("???", idx).also { idx = it } != -1) { count++; idx += 3 }
        assertTrue(count >= 2)
        assertFalse(msg.contains("null"))
    }

    // --- sem desejos ---

    @Test
    fun gerar_nullDesejos_noWishlistSection() {
        assertFalse(MensagemSecretaBuilder.gerar("Eva", "Felipe", null).contains("Lista de desejos"))
    }

    @Test
    fun gerar_emptyDesejos_noWishlistSection() {
        assertFalse(MensagemSecretaBuilder.gerar("Eva", "Felipe", emptyList()).contains("Lista de desejos"))
    }

    // --- com desejos ---

    @Test
    fun gerar_withDesejo_containsWishlistSection() {
        val msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", listOf(buildDesejo("Fone", null, 0.0, 0.0, null)))
        assertTrue(msg.contains("Lista de desejos"))
        assertTrue(msg.contains("Fone"))
    }

    @Test
    fun gerar_withCategoria_containsCategoria() {
        val msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", listOf(buildDesejo("Fone", "Eletrônicos", 0.0, 0.0, null)))
        assertTrue(msg.contains("Eletrônicos"))
    }

    @Test
    fun gerar_categoriaApenasEspacos_naoExibida() {
        val msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", listOf(buildDesejo("Fone", "   ", 0.0, 0.0, null)))
        assertTrue(msg.contains("Fone"))
        assertFalse(msg.contains("(   )"))
    }

    @Test
    fun gerar_withLojas_containsLoja() {
        val msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", listOf(buildDesejo("Fone", null, 0.0, 0.0, "Amazon")))
        assertTrue(msg.contains("Amazon"))
    }

    // --- faixa de preço ---

    @Test
    fun gerar_minAndMax_showsRange() {
        val msg = MensagemSecretaBuilder.gerar("Iris", "João", listOf(buildDesejo("Livro", null, 50.0, 100.0, null)))
        assertTrue(msg.contains("R\$"))
        assertTrue(msg.contains("50,00"))
        assertTrue(msg.contains("100,00"))
    }

    @Test
    fun gerar_onlyMin_showsAPartirDe() {
        val msg = MensagemSecretaBuilder.gerar("Karen", "Lucas", listOf(buildDesejo("Tablet", null, 300.0, 0.0, null)))
        assertTrue(msg.contains("a partir de"))
    }

    @Test
    fun gerar_onlyMax_showsAte() {
        val msg = MensagemSecretaBuilder.gerar("Maria", "Nadia", listOf(buildDesejo("Monitor", null, 0.0, 800.0, null)))
        assertTrue(msg.contains("ate R\$"))
    }

    @Test
    fun gerar_invalidRange_minGreaterThanMax_showsOnlyAte() {
        val msg = MensagemSecretaBuilder.gerar("Otto", "Paulo", listOf(buildDesejo("Item", null, 500.0, 100.0, null)))
        assertTrue(msg.contains("ate R\$"))
        assertFalse(msg.contains("a partir de R\$"))
        assertFalse(msg.contains("500,00"))
    }

    @Test
    fun gerar_zeroPrices_noPriceSection() {
        assertFalse(MensagemSecretaBuilder.gerar("Quesia", "Rita", listOf(buildDesejo("Caderno", null, 0.0, 0.0, null))).contains("R\$"))
    }

    // --- produto nulo ou vazio filtrado ---

    @Test
    fun gerar_desejo_nullProduto_isSkipped() {
        val msg = MensagemSecretaBuilder.gerar("Sara", "Tiago", listOf(buildDesejo(null, "Cat", 0.0, 0.0, null)))
        assertFalse(msg.contains("Lista de desejos"))
    }

    @Test
    fun gerar_allDesejos_nullProduto_noWishlistSection() {
        val msg = MensagemSecretaBuilder.gerar("Sara", "Tiago", listOf(
            buildDesejo(null, null, 0.0, 0.0, null),
            buildDesejo("   ", null, 0.0, 0.0, null)
        ))
        assertFalse(msg.contains("Lista de desejos"))
    }

    @Test
    fun gerar_desejo_emptyProduto_isSkipped() {
        val msg = MensagemSecretaBuilder.gerar("Uma", "Vera", listOf(
            buildDesejo("   ", null, 0.0, 0.0, null),
            buildDesejo("Mochila", null, 0.0, 0.0, null)
        ))
        assertTrue(msg.contains("Mochila"))
        assertTrue(msg.contains("1. Mochila"))
    }

    // --- trim de produto, categoria e lojas ---

    @Test
    fun gerar_produtoComEspacosExternos_trimadoNoOutput() {
        val msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", listOf(buildDesejo("  Fone  ", null, 0.0, 0.0, null)))
        assertTrue(msg.contains("Fone"))
        assertFalse(msg.contains("  Fone  "))
    }

    @Test
    fun gerar_categoriaComEspacosExternos_trimadaNoOutput() {
        val msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", listOf(buildDesejo("Fone", "  Eletronicos  ", 0.0, 0.0, null)))
        assertTrue(msg.contains("Eletronicos"))
        assertFalse(msg.contains("(  Eletronicos  )"))
    }

    @Test
    fun gerar_lojasComEspacosExternos_trimadoNoOutput() {
        val msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", listOf(buildDesejo("Fone", null, 0.0, 0.0, "  Amazon  ")))
        assertTrue(msg.contains("Amazon"))
        assertFalse(msg.contains("  Amazon  "))
    }

    // --- múltiplos desejos ---

    @Test
    fun gerar_multipleDesejos_allListed() {
        val msg = MensagemSecretaBuilder.gerar("Wagner", "Xavier", listOf(
            buildDesejo("Livro", null, 0.0, 0.0, null),
            buildDesejo("Caneta", null, 0.0, 0.0, null),
            buildDesejo("Mochila", null, 0.0, 0.0, null)
        ))
        assertTrue(msg.contains("Livro"))
        assertTrue(msg.contains("Caneta"))
        assertTrue(msg.contains("Mochila"))
    }
}