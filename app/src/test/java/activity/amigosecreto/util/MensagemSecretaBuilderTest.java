package activity.amigosecreto.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import activity.amigosecreto.db.Desejo;

import static org.junit.Assert.*;

public class MensagemSecretaBuilderTest {

    private Desejo buildDesejo(String produto, String categoria, double min, double max, String lojas) {
        Desejo d = new Desejo();
        d.setProduto(produto);
        d.setCategoria(categoria);
        d.setPrecoMinimo(min);
        d.setPrecoMaximo(max);
        d.setLojas(lojas);
        return d;
    }

    // --- saudação e estrutura básica ---

    @Test
    public void gerar_containsParticipanteName() {
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", null);
        assertTrue(msg.contains("Ana"));
    }

    @Test
    public void gerar_containsAmigoName() {
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", null);
        assertTrue(msg.contains("Bruno"));
    }

    @Test
    public void gerar_containsEncerramento() {
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", null);
        assertTrue(msg.contains("segredo"));
    }

    // --- nomes nulos ---

    @Test
    public void gerar_nullParticipante_usesPlaceholder() {
        String msg = MensagemSecretaBuilder.gerar(null, "Carlos", null);
        assertTrue(msg.contains("???"));
        assertFalse(msg.contains("null"));
    }

    @Test
    public void gerar_nullAmigo_usesPlaceholder() {
        String msg = MensagemSecretaBuilder.gerar("Diana", null, null);
        assertTrue(msg.contains("???"));
        assertFalse(msg.contains("null"));
    }

    @Test
    public void gerar_nullParticipanteEAmigo_bothUsePlaceholder() {
        String msg = MensagemSecretaBuilder.gerar(null, null, null);
        // ambos os placeholders devem aparecer (dois "???" na mensagem)
        int count = 0;
        int idx = 0;
        while ((idx = msg.indexOf("???", idx)) != -1) { count++; idx += 3; }
        assertTrue(count >= 2);
        assertFalse(msg.contains("null"));
    }

    // --- sem desejos ---

    @Test
    public void gerar_nullDesejos_noWishlistSection() {
        String msg = MensagemSecretaBuilder.gerar("Eva", "Felipe", null);
        assertFalse(msg.contains("Lista de desejos"));
    }

    @Test
    public void gerar_emptyDesejos_noWishlistSection() {
        String msg = MensagemSecretaBuilder.gerar("Eva", "Felipe", Collections.emptyList());
        assertFalse(msg.contains("Lista de desejos"));
    }

    // --- com desejos ---

    @Test
    public void gerar_withDesejo_containsWishlistSection() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", null, 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", desejos);
        assertTrue(msg.contains("Lista de desejos"));
        assertTrue(msg.contains("Fone"));
    }

    @Test
    public void gerar_withCategoria_containsCategoria() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", "Eletrônicos", 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", desejos);
        assertTrue(msg.contains("Eletrônicos"));
    }

    @Test
    public void gerar_categoriaApenasEspacos_naoExibida() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", "   ", 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", desejos);
        // categoria em branco deve ser ignorada — produto ainda aparece
        assertTrue(msg.contains("Fone"));
        assertFalse(msg.contains("(   )"));
    }

    @Test
    public void gerar_withLojas_containsLoja() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", null, 0, 0, "Amazon"));
        String msg = MensagemSecretaBuilder.gerar("Gabi", "Hugo", desejos);
        assertTrue(msg.contains("Amazon"));
    }

    // --- faixa de preço ---

    @Test
    public void gerar_minAndMax_showsRange() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Livro", null, 50.0, 100.0, null));
        String msg = MensagemSecretaBuilder.gerar("Iris", "João", desejos);
        assertTrue(msg.contains("R$"));
        assertTrue(msg.contains("50,00"));
        assertTrue(msg.contains("100,00"));
    }

    @Test
    public void gerar_onlyMin_showsAPartirDe() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Tablet", null, 300.0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Karen", "Lucas", desejos);
        assertTrue(msg.contains("a partir de"));
    }

    @Test
    public void gerar_onlyMax_showsAte() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Monitor", null, 0, 800.0, null));
        String msg = MensagemSecretaBuilder.gerar("Maria", "Nadia", desejos);
        assertTrue(msg.contains("ate R$"));
    }

    @Test
    public void gerar_invalidRange_minGreaterThanMax_showsOnlyAte() {
        // Faixa inválida: min > max — comportamento intencional: exibe "ate R$ max"
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Item", null, 500.0, 100.0, null));
        String msg = MensagemSecretaBuilder.gerar("Otto", "Paulo", desejos);
        assertTrue(msg.contains("ate R$"));
        assertFalse(msg.contains("a partir de R$"));
        assertFalse(msg.contains("500,00")); // valor formatado do min nao deve aparecer
    }

    @Test
    public void gerar_zeroPrices_noPriceSection() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Caderno", null, 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Quesia", "Rita", desejos);
        assertFalse(msg.contains("R$"));
    }

    // --- produto nulo ou vazio filtrado ---

    @Test
    public void gerar_desejo_nullProduto_isSkipped() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo(null, "Cat", 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Sara", "Tiago", desejos);
        // cabecalho nao deve aparecer pois o unico item foi filtrado pelo produto nulo
        assertFalse(msg.contains("Lista de desejos"));
    }

    @Test
    public void gerar_allDesejos_nullProduto_noWishlistSection() {
        List<Desejo> desejos = Arrays.asList(
                buildDesejo(null, null, 0, 0, null),
                buildDesejo("   ", null, 0, 0, null)
        );
        String msg = MensagemSecretaBuilder.gerar("Sara", "Tiago", desejos);
        assertFalse(msg.contains("Lista de desejos"));
    }

    @Test
    public void gerar_desejo_emptyProduto_isSkipped() {
        List<Desejo> desejos = Arrays.asList(
                buildDesejo("   ", null, 0, 0, null),
                buildDesejo("Mochila", null, 0, 0, null)
        );
        String msg = MensagemSecretaBuilder.gerar("Uma", "Vera", desejos);
        // apenas Mochila deve aparecer
        assertTrue(msg.contains("Mochila"));
        // numeracao deve comecar em 1
        assertTrue(msg.contains("1. Mochila"));
    }

    // --- trim de produto, categoria e lojas (comportamento Kotlin: trim antes de append) ---

    @Test
    public void gerar_produtoComEspacosExternos_trimadoNoOutput() {
        // Kotlin trim()s the value before appending — leading/trailing spaces must not appear.
        List<Desejo> desejos = Collections.singletonList(buildDesejo("  Fone  ", null, 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", desejos);
        assertTrue(msg.contains("Fone"));
        assertFalse(msg.contains("  Fone  "));
    }

    @Test
    public void gerar_categoriaComEspacosExternos_trimadaNoOutput() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", "  Eletronicos  ", 0, 0, null));
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", desejos);
        assertTrue(msg.contains("Eletronicos"));
        assertFalse(msg.contains("(  Eletronicos  )"));
    }

    @Test
    public void gerar_lojasComEspacosExternos_trimadoNoOutput() {
        List<Desejo> desejos = Collections.singletonList(buildDesejo("Fone", null, 0, 0, "  Amazon  "));
        String msg = MensagemSecretaBuilder.gerar("Ana", "Bruno", desejos);
        assertTrue(msg.contains("Amazon"));
        assertFalse(msg.contains("  Amazon  "));
    }

    // --- múltiplos desejos ---

    @Test
    public void gerar_multipleDesejos_allListed() {
        List<Desejo> desejos = Arrays.asList(
                buildDesejo("Livro", null, 0, 0, null),
                buildDesejo("Caneta", null, 0, 0, null),
                buildDesejo("Mochila", null, 0, 0, null)
        );
        String msg = MensagemSecretaBuilder.gerar("Wagner", "Xavier", desejos);
        assertTrue(msg.contains("Livro"));
        assertTrue(msg.contains("Caneta"));
        assertTrue(msg.contains("Mochila"));
    }

}
