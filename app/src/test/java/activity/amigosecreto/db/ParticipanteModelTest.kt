package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ParticipanteModelTest {

    @Test
    fun construtor_padrao_amigo_sorteado_e_null() {
        assertNull(Participante().amigoSorteadoId)
    }

    @Test
    fun construtor_padrao_ids_excluidos_e_lista_vazia() {
        val p = Participante()
        assertNotNull(p.idsExcluidos)
        assertTrue(p.idsExcluidos.isEmpty())
    }

    @Test
    fun construtor_padrao_enviado_e_false() {
        assertFalse(Participante().isEnviado)
    }

    @Test
    fun setNome_getNome_round_trip() {
        val p = Participante()
        p.nome = "Maria"
        assertEquals("Maria", p.nome)
    }

    @Test
    fun setEmail_getEmail_round_trip() {
        val p = Participante()
        p.email = "maria@email.com"
        assertEquals("maria@email.com", p.email)
    }

    @Test
    fun setTelefone_getTelefone_round_trip() {
        val p = Participante()
        p.telefone = "(11) 91234-5678"
        assertEquals("(11) 91234-5678", p.telefone)
    }

    @Test
    fun setAmigoSorteadoId_getAmigoSorteadoId_round_trip() {
        val p = Participante()
        p.amigoSorteadoId = 99
        assertEquals(99, p.amigoSorteadoId)
    }

    @Test
    fun setEnviado_true_getEnviado_retorna_true() {
        val p = Participante()
        p.isEnviado = true
        assertTrue(p.isEnviado)
    }

    @Test
    fun setIdsExcluidos_getIdsExcluidos_round_trip() {
        val p = Participante()
        p.idsExcluidos = mutableListOf(1, 2, 3)
        assertEquals(3, p.idsExcluidos.size)
        assertTrue(p.idsExcluidos.contains(2))
    }

    @Test
    fun setIdsExcluidos_comArraysAsList_continuaMutavel() {
        // setter deve fazer cópia defensiva para que add() não lance UnsupportedOperationException
        val p = Participante()
        p.idsExcluidos = mutableListOf(1, 2, 3)
        p.idsExcluidos.add(4)
        assertEquals(4, p.idsExcluidos.size)
        assertTrue(p.idsExcluidos.contains(4))
    }

    @Test
    fun toString_retorna_nome() {
        val p = Participante()
        p.nome = "Joao"
        assertEquals("Joao", p.toString())
    }

    @Test
    fun toString_com_nome_null_retorna_string_vazia() {
        assertEquals("", Participante().toString())
    }

    @Test
    fun serializable_round_trip_preserva_campos_principais() {
        val original = Participante().apply {
            id = 10
            nome = "Carlos"
            email = "carlos@teste.com"
            telefone = "(21) 99999-8888"
            isEnviado = true
            amigoSorteadoId = 5
        }

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }

        val desserializado = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as Participante
        }

        assertEquals(original.id, desserializado.id)
        assertEquals(original.nome, desserializado.nome)
        assertEquals(original.email, desserializado.email)
        assertEquals(original.telefone, desserializado.telefone)
        assertEquals(original.isEnviado, desserializado.isEnviado)
        assertEquals(original.amigoSorteadoId, desserializado.amigoSorteadoId)
    }
}