package activity.amigosecreto

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa a expressão de geração de avatar usada em ParticipantesRecyclerAdapter e
 * ParticipantesActivity (dois ViewHolders inline).
 *
 * A lógica é: nome?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
 * Sem dependências Android — teste puro JVM.
 */
class AvatarLogicTest {

    private fun avatarText(nome: String?): String =
        nome?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    @Test
    fun avatarComNomeNulo_retornaInterrogacao() {
        assertEquals("?", avatarText(null))
    }

    @Test
    fun avatarComNomeVazio_retornaInterrogacao() {
        assertEquals("?", avatarText(""))
    }

    @Test
    fun avatarComApenasEspacos_retornaInterrogacao() {
        assertEquals("?", avatarText("   "))
    }

    @Test
    fun avatarComEspacoInicial_retornaLetraCorreta() {
        assertEquals("J", avatarText(" João"))
    }

    @Test
    fun avatarComNomeNormal_retornaPrimeiraLetraMaiuscula() {
        assertEquals("A", avatarText("Alice"))
    }

    @Test
    fun avatarComNomeMinusculo_retornaMaiuscula() {
        assertEquals("M", avatarText("maria"))
    }

    @Test
    fun avatarComAcentuacao_retornaLetraAcentuadaMaiuscula() {
        // 'â' uppercase é 'Â' — sem expansão multi-char, seguro com uppercaseChar()
        assertEquals("Â", avatarText("âncora"))
    }
}
