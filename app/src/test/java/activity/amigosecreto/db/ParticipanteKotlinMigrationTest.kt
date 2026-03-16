package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test

/**
 * Testes de comportamento do Participante relevantes para migração para Kotlin.
 *
 * Documenta contratos que devem ser preservados:
 *
 * 1. Semântica de igualdade — Participante NÃO implementa equals/hashCode,
 *    portanto usa igualdade por referência. Se a migração para Kotlin usar
 *    data class, a semântica muda (estrutural). Esses testes documentam o
 *    comportamento ATUAL para que a decisão de manter ou mudar seja explícita.
 *
 * 2. Mutabilidade de idsExcluidos — o campo é MutableList mutável. Código que
 *    chama .add()/.remove() diretamente depende disso.
 *
 * 3. amigoSorteadoId nullable — Int? em Kotlin, deve aceitar null sem NPE.
 */
class ParticipanteKotlinMigrationTest {

    // ===== Semântica de igualdade atual (referência) =====

    @Test
    fun equals_mesmaInstancia_retornaTrue() {
        val p = Participante().also { it.nome = "Ana" }
        assertEquals(p, p)
    }

    @Test
    fun equals_instanciasDiferentes_mesmosValores_retornaFalse() {
        // Comportamento ATUAL: sem equals() → usa Object.equals() → referência
        // Ao migrar para Kotlin data class, isso mudará para TRUE (estrutural)
        val a = Participante().apply { id = 1; nome = "Bruno" }
        val b = Participante().apply { id = 1; nome = "Bruno" }
        assertNotEquals("Participante usa igualdade por referência (sem equals override)", a, b)
    }

    @Test
    fun hashCode_instanciasDiferentes_saoObjetosDistintos() {
        val a = Participante().apply { id = 1; nome = "Carla" }
        val b = Participante().apply { id = 1; nome = "Carla" }
        assertNotSame("devem ser objetos distintos", a, b)
        // hashCode não sobrescrito: System.identityHashCode, não baseado em campos
        // ao migrar para data class, hashCode passará a ser estrutural
    }

    // ===== Mutabilidade de idsExcluidos =====

    @Test
    fun idsExcluidos_mutavel_addFunciona() {
        val p = Participante()
        p.idsExcluidos.add(10)
        p.idsExcluidos.add(20)
        assertEquals(2, p.idsExcluidos.size)
        assertTrue(p.idsExcluidos.contains(10))
        assertTrue(p.idsExcluidos.contains(20))
    }

    @Test
    fun idsExcluidos_mutavel_removeFunciona() {
        val p = Participante()
        p.idsExcluidos.add(5)
        p.idsExcluidos.add(6)
        p.idsExcluidos.remove(5)
        assertEquals(1, p.idsExcluidos.size)
        assertFalse(p.idsExcluidos.contains(5))
    }

    @Test
    fun idsExcluidos_setListaMutavel_continuaMutavel() {
        val p = Participante()
        p.idsExcluidos = mutableListOf(99)
        p.idsExcluidos.add(100)
        assertEquals(2, p.idsExcluidos.size)
    }

    // ===== amigoSorteadoId nullable =====

    @Test
    fun amigoSorteadoId_defaultNull_semNPE() {
        val p = Participante()
        assertNull(p.amigoSorteadoId)
        val temAmigo = p.amigoSorteadoId != null
        assertFalse(temAmigo)
    }

    @Test
    fun amigoSorteadoId_setNull_getNull() {
        val p = Participante()
        p.amigoSorteadoId = 42
        assertEquals(42, p.amigoSorteadoId)
        p.amigoSorteadoId = null
        assertNull(p.amigoSorteadoId)
    }

    @Test
    fun amigoSorteadoId_unboxingNulo_naoDeveSerFeitoSemVerificacao() {
        val p = Participante()
        val id = p.amigoSorteadoId ?: -1
        assertEquals(-1, id)
    }

    // ===== enviado boolean padrão =====

    @Test
    fun enviado_default_false() {
        assertFalse(Participante().isEnviado)
    }

    @Test
    fun enviado_setTrue_getTrue() {
        val p = Participante()
        p.isEnviado = true
        assertTrue(p.isEnviado)
    }

    @Test
    fun enviado_setFalse_getFalse() {
        val p = Participante()
        p.isEnviado = true
        p.isEnviado = false
        assertFalse(p.isEnviado)
    }
}