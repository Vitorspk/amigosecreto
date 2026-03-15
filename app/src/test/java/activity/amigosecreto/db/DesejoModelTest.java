package activity.amigosecreto.db;

import org.junit.Test;
import static org.junit.Assert.*;

public class DesejoModelTest {

    private Desejo buildDesejo() {
        Desejo d = new Desejo();
        d.setId(1);
        d.setProduto("Livro Java");
        d.setCategoria("Livros");
        d.setLojas("Amazon");
        d.setPrecoMinimo(29.90);
        d.setPrecoMaximo(59.90);
        d.setParticipanteId(10);
        return d;
    }

    @Test
    public void defaultConstructor_allFieldsDefault() {
        Desejo d = new Desejo();
        assertEquals(0, d.getId());
        assertNull(d.getProduto());
        assertNull(d.getCategoria());
        assertNull(d.getLojas());
        assertEquals(0.0, d.getPrecoMinimo(), 0.001);
        assertEquals(0.0, d.getPrecoMaximo(), 0.001);
        assertEquals(0, d.getParticipanteId());
    }

    @Test
    public void twoArgConstructor_setsIdAndProduto() {
        Desejo d = new Desejo(5, "Fone de ouvido");
        assertEquals(5, d.getId());
        assertEquals("Fone de ouvido", d.getProduto());
    }

    @Test
    public void settersAndGetters_roundtrip() {
        Desejo d = buildDesejo();
        assertEquals(1, d.getId());
        assertEquals("Livro Java", d.getProduto());
        assertEquals("Livros", d.getCategoria());
        assertEquals("Amazon", d.getLojas());
        assertEquals(29.90, d.getPrecoMinimo(), 0.001);
        assertEquals(59.90, d.getPrecoMaximo(), 0.001);
        assertEquals(10, d.getParticipanteId());
    }

    // equals/hashCode are based on id only (plain class, not data class).
    // Identity is the DB primary key — mutable fields do not affect equality.

    @Test
    public void equals_mesmoId_isEqual() {
        Desejo a = buildDesejo();
        Desejo b = buildDesejo();
        assertEquals(a, b); // same id=1
    }

    @Test
    public void equals_differentId_notEqual() {
        Desejo a = buildDesejo();
        Desejo b = buildDesejo();
        b.setId(2);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_mesmoId_diferenteProduto_aindaEqual() {
        // equals uses id only — changing other fields must not affect equality
        Desejo a = buildDesejo();
        Desejo b = buildDesejo();
        b.setProduto("Outro produto");
        assertEquals(a, b); // same id → still equal
    }

    @Test
    public void equals_mesmoId_diferenteParticipanteId_aindaEqual() {
        Desejo a = buildDesejo();
        Desejo b = buildDesejo();
        b.setParticipanteId(b.getParticipanteId() + 1);
        assertEquals(a, b); // same id → still equal
    }

    @Test
    public void equals_null_notEqual() {
        assertNotEquals(buildDesejo(), null);
    }

    @Test
    public void equals_sameInstance_isEqual() {
        Desejo d = buildDesejo();
        assertEquals(d, d);
    }

    @Test
    public void hashCode_mesmoId_sameHash() {
        Desejo a = buildDesejo();
        Desejo b = buildDesejo();
        assertEquals(a.hashCode(), b.hashCode()); // same id → same hash
    }

    @Test
    public void compareTo_smallerIdComesFirst() {
        Desejo a = new Desejo(1, "A");
        Desejo b = new Desejo(2, "B");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void compareTo_sameId_returnsZero() {
        Desejo a = new Desejo(3, "A");
        Desejo b = new Desejo(3, "B");
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void toString_containsIdAndProduto() {
        Desejo d = new Desejo(7, "Tablet");
        String s = d.toString();
        assertTrue(s.contains("id=7"));
        assertTrue(s.contains("Tablet"));
    }

    @Test
    public void nullCategoria_naoAfetaEquals() {
        // equals uses id only — null fields must not affect equality
        Desejo a = buildDesejo();
        a.setCategoria(null);
        Desejo b = buildDesejo();
        b.setCategoria(null);
        assertEquals(a, b);
    }

    @Test
    public void nullLojas_naoAfetaEquals() {
        Desejo a = buildDesejo();
        a.setLojas(null);
        Desejo b = buildDesejo();
        b.setLojas(null);
        assertEquals(a, b);
    }

    @Test
    public void nullProduto_naoAfetaEquals() {
        Desejo a = buildDesejo();
        a.setProduto(null);
        Desejo b = buildDesejo();
        b.setProduto(null);
        assertEquals(a, b);
    }

    @Test
    public void toString_nullProduto_doesNotThrow() {
        Desejo d = new Desejo(8, null);
        String s = d.toString(); // não deve lançar NullPointerException
        assertNotNull(s);
        assertTrue(s.contains("id=8"));
    }

    @Test
    public void hashCode_differentId_differentHash() {
        Desejo a = buildDesejo(); // id=1
        Desejo b = buildDesejo();
        b.setId(2);
        assertNotEquals(a.hashCode(), b.hashCode());
    }
}
