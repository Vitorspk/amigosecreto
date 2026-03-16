package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class GrupoModelTest {

    @Test
    fun construtor_padrao_campos_sao_nulos_e_zero() {
        val g = Grupo()
        assertEquals(0, g.id)
        assertNull(g.nome)
        assertNull(g.data)
    }

    @Test
    fun setNome_getNome_round_trip() {
        val g = Grupo()
        g.nome = "Familia"
        assertEquals("Familia", g.nome)
    }

    @Test
    fun setId_getId_round_trip() {
        val g = Grupo()
        g.id = 42
        assertEquals(42, g.id)
    }

    @Test
    fun setData_getData_round_trip() {
        val g = Grupo()
        g.data = "25/12/2025"
        assertEquals("25/12/2025", g.data)
    }

    @Test
    fun toString_retorna_nome() {
        val g = Grupo()
        g.nome = "Trabalho"
        assertEquals("Trabalho", g.toString())
    }

    @Test
    fun toString_com_nome_null_retorna_string_vazia() {
        // Kotlin toString() retorna "" quando nome é null (String não pode ser null em Kotlin)
        val g = Grupo()
        assertEquals("", g.toString())
    }

    @Test
    fun serializable_round_trip_preserva_campos() {
        val original = Grupo()
        original.id = 7
        original.nome = "Amigos"
        original.data = "01/01/2025"

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }

        val desserializado = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as Grupo
        }

        assertEquals(original.id, desserializado.id)
        assertEquals(original.nome, desserializado.nome)
        assertEquals(original.data, desserializado.data)
    }
}