package activity.amigosecreto.util

import org.junit.Assert.*
import org.junit.Test

class StringExtensionsTest {

    // --- toAvatarText ---

    @Test
    fun toAvatarText_null_retorna_interrogacao() {
        val s: String? = null
        assertEquals("?", s.toAvatarText())
    }

    @Test
    fun toAvatarText_string_vazia_retorna_interrogacao() {
        assertEquals("?", "".toAvatarText())
    }

    @Test
    fun toAvatarText_so_espacos_retorna_interrogacao() {
        assertEquals("?", "   ".toAvatarText())
    }

    @Test
    fun toAvatarText_nome_simples_retorna_primeira_letra_maiuscula() {
        assertEquals("A", "alice".toAvatarText())
    }

    @Test
    fun toAvatarText_nome_ja_maiusculo_retorna_primeira_letra() {
        assertEquals("B", "Bruno".toAvatarText())
    }

    @Test
    fun toAvatarText_nome_com_espaco_lider_retorna_primeira_letra_apos_trim() {
        assertEquals("A", " Alice".toAvatarText())
    }

    @Test
    fun toAvatarText_nome_com_espaco_no_fim_retorna_primeira_letra() {
        assertEquals("C", "Carlos  ".toAvatarText())
    }

    @Test
    fun toAvatarText_nome_com_acentuacao_retorna_primeira_letra_maiuscula() {
        assertEquals("Á", "álvaro".toAvatarText())
    }

    @Test
    fun toAvatarText_string_com_apenas_um_caractere_retorna_esse_caractere_maiusculo() {
        assertEquals("Z", "z".toAvatarText())
    }
}
