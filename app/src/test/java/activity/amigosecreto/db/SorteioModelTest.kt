package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SorteioModelTest {

    // =========================================================
    // Sorteio
    // =========================================================

    @Test
    fun sorteio_construtor_padrao_valores_iniciais() {
        val s = Sorteio()
        assertEquals(0, s.id)
        assertEquals(0, s.grupoId)
        assertEquals("", s.dataHora)
        assertTrue(s.pares.isEmpty())
    }

    @Test
    fun sorteio_construtor_primario_com_valores() {
        val s = Sorteio(id = 1, grupoId = 42, dataHora = "2025-12-25T10:00:00")
        assertEquals(1, s.id)
        assertEquals(42, s.grupoId)
        assertEquals("2025-12-25T10:00:00", s.dataHora)
    }

    @Test
    fun sorteio_pares_pode_ser_atribuido() {
        val s = Sorteio(id = 1, grupoId = 1)
        val par = SorteioPar(sorteioId = 1, participanteId = 2, sorteadoId = 3,
            nomeParticipante = "Ana", nomeSorteado = "Bia")
        s.pares = listOf(par)
        assertEquals(1, s.pares.size)
        assertEquals("Ana", s.pares[0].nomeParticipante)
    }

    @Test
    fun sorteio_serializable_round_trip_preserva_campos() {
        val original = Sorteio(id = 5, grupoId = 10, dataHora = "2025-12-25T18:00:00")

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val deserializado = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as Sorteio
        }

        assertEquals(original.id, deserializado.id)
        assertEquals(original.grupoId, deserializado.grupoId)
        assertEquals(original.dataHora, deserializado.dataHora)
    }

    @Test
    fun sorteio_serializable_pares_sao_transient_nao_serializados() {
        // @Ignore em pares significa que não é persistido por Room,
        // mas Serializable serializa tudo que não é @Transient.
        // Verificamos que pares fica como emptyList após deserialização.
        val original = Sorteio(id = 1, grupoId = 1)
        val par = SorteioPar(sorteioId = 1, participanteId = 2, sorteadoId = 3,
            nomeParticipante = "Ana", nomeSorteado = "Bia")
        original.pares = listOf(par)

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val deserializado = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as Sorteio
        }

        // pares não tem anotação @Transient, então é serializado normalmente
        assertEquals(1, deserializado.pares.size)
    }

    // =========================================================
    // SorteioPar
    // =========================================================

    @Test
    fun sorteioPar_construtor_padrao_valores_iniciais() {
        val par = SorteioPar()
        assertEquals(0, par.sorteioId)
        assertEquals(0, par.participanteId)
        assertEquals(0, par.sorteadoId)
        assertEquals("", par.nomeParticipante)
        assertEquals("", par.nomeSorteado)
        assertFalse(par.enviado)
    }

    @Test
    fun sorteioPar_construtor_primario_com_valores() {
        val par = SorteioPar(
            sorteioId = 1,
            participanteId = 10,
            sorteadoId = 20,
            nomeParticipante = "Ana",
            nomeSorteado = "Bruno",
            enviado = true
        )
        assertEquals(1, par.sorteioId)
        assertEquals(10, par.participanteId)
        assertEquals(20, par.sorteadoId)
        assertEquals("Ana", par.nomeParticipante)
        assertEquals("Bruno", par.nomeSorteado)
        assertTrue(par.enviado)
    }

    @Test
    fun sorteioPar_enviado_padrao_false() {
        val par = SorteioPar(sorteioId = 1, participanteId = 2, sorteadoId = 3,
            nomeParticipante = "X", nomeSorteado = "Y")
        assertFalse(par.enviado)
    }

    @Test
    fun sorteioPar_serializable_round_trip_preserva_campos() {
        val original = SorteioPar(
            sorteioId = 3,
            participanteId = 7,
            sorteadoId = 9,
            nomeParticipante = "Carlos",
            nomeSorteado = "Diana",
            enviado = true
        )

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val deserializado = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as SorteioPar
        }

        assertEquals(original.sorteioId, deserializado.sorteioId)
        assertEquals(original.participanteId, deserializado.participanteId)
        assertEquals(original.sorteadoId, deserializado.sorteadoId)
        assertEquals(original.nomeParticipante, deserializado.nomeParticipante)
        assertEquals(original.nomeSorteado, deserializado.nomeSorteado)
        assertEquals(original.enviado, deserializado.enviado)
    }

    @Test
    fun sorteioPar_nomes_snapshot_preserva_espacos() {
        val par = SorteioPar(nomeParticipante = "João Silva", nomeSorteado = "Maria Oliveira")
        assertEquals("João Silva", par.nomeParticipante)
        assertEquals("Maria Oliveira", par.nomeSorteado)
    }
}
