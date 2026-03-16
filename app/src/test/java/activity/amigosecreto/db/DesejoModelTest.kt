package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test

class DesejoModelTest {

    private fun buildDesejo() = Desejo().apply {
        id = 1
        produto = "Livro Java"
        categoria = "Livros"
        lojas = "Amazon"
        precoMinimo = 29.90
        precoMaximo = 59.90
        participanteId = 10
    }

    @Test
    fun defaultConstructor_allFieldsDefault() {
        val d = Desejo()
        assertEquals(0, d.id)
        assertNull(d.produto)
        assertNull(d.categoria)
        assertNull(d.lojas)
        assertEquals(0.0, d.precoMinimo, 0.001)
        assertEquals(0.0, d.precoMaximo, 0.001)
        assertEquals(0, d.participanteId)
    }

    @Test
    fun twoArgConstructor_setsIdAndProduto() {
        val d = Desejo(5, "Fone de ouvido")
        assertEquals(5, d.id)
        assertEquals("Fone de ouvido", d.produto)
    }

    @Test
    fun settersAndGetters_roundtrip() {
        val d = buildDesejo()
        assertEquals(1, d.id)
        assertEquals("Livro Java", d.produto)
        assertEquals("Livros", d.categoria)
        assertEquals("Amazon", d.lojas)
        assertEquals(29.90, d.precoMinimo, 0.001)
        assertEquals(59.90, d.precoMaximo, 0.001)
        assertEquals(10, d.participanteId)
    }

    // equals/hashCode are based on id only (plain class, not data class).

    @Test
    fun equals_mesmoId_isEqual() {
        assertEquals(buildDesejo(), buildDesejo())
    }

    @Test
    fun equals_differentId_notEqual() {
        val a = buildDesejo()
        val b = buildDesejo().also { it.id = 2 }
        assertNotEquals(a, b)
    }

    @Test
    fun equals_mesmoId_diferenteProduto_aindaEqual() {
        val a = buildDesejo()
        val b = buildDesejo().also { it.produto = "Outro produto" }
        assertEquals(a, b)
    }

    @Test
    fun equals_mesmoId_diferenteParticipanteId_aindaEqual() {
        val a = buildDesejo()
        val b = buildDesejo().also { it.participanteId = it.participanteId + 1 }
        assertEquals(a, b)
    }

    @Test
    fun equals_null_notEqual() {
        assertNotEquals(buildDesejo(), null)
    }

    @Test
    fun equals_sameInstance_isEqual() {
        val d = buildDesejo()
        assertEquals(d, d)
    }

    @Test
    fun hashCode_mesmoId_sameHash() {
        assertEquals(buildDesejo().hashCode(), buildDesejo().hashCode())
    }

    @Test
    fun compareTo_smallerIdComesFirst() {
        val a = Desejo(1, "A")
        val b = Desejo(2, "B")
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
    }

    @Test
    fun compareTo_sameId_returnsZero() {
        val a = Desejo(3, "A")
        val b = Desejo(3, "B")
        assertEquals(0, a.compareTo(b))
    }

    @Test
    fun toString_containsIdAndProduto() {
        val s = Desejo(7, "Tablet").toString()
        assertTrue(s.contains("id=7"))
        assertTrue(s.contains("Tablet"))
    }

    @Test
    fun nullCategoria_naoAfetaEquals() {
        val a = buildDesejo().also { it.categoria = null }
        val b = buildDesejo().also { it.categoria = null }
        assertEquals(a, b)
    }

    @Test
    fun nullLojas_naoAfetaEquals() {
        val a = buildDesejo().also { it.lojas = null }
        val b = buildDesejo().also { it.lojas = null }
        assertEquals(a, b)
    }

    @Test
    fun nullProduto_naoAfetaEquals() {
        val a = buildDesejo().also { it.produto = null }
        val b = buildDesejo().also { it.produto = null }
        assertEquals(a, b)
    }

    @Test
    fun toString_nullProduto_doesNotThrow() {
        val s = Desejo(8, null).toString()
        assertNotNull(s)
        assertTrue(s.contains("id=8"))
    }

    @Test
    fun hashCode_differentId_differentHash() {
        val a = buildDesejo() // id=1
        val b = buildDesejo().also { it.id = 2 }
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}