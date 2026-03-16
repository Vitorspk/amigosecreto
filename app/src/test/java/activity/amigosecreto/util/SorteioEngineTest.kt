package activity.amigosecreto.util

import activity.amigosecreto.db.Participante
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class SorteioEngineTest {

    private fun criar(id: Int, nome: String, vararg excluidos: Int): Participante =
        Participante().apply {
            this.id = id
            this.nome = nome
            idsExcluidos = excluidos.toMutableList()
        }

    private fun criarGrupo(quantidade: Int): List<Participante> =
        (1..quantidade).map { criar(it, "P$it") }

    /** Tenta até 200 vezes até obter um resultado não-null. Para grupos sem exclusões
     *  extremas isso sempre converge. Lança AssertionError se nunca convergir. */
    private fun sortearComRetry(participantes: List<Participante>): List<Participante> {
        for (i in 0 until 200) {
            val r = SorteioEngine.tentarSorteio(participantes)
            if (r != null) return r
        }
        fail("SorteioEngine retornou null em 200 tentativas para grupo sem exclusoes bloqueantes")
        error("unreachable")
    }

    // --- Tamanho do resultado ---

    @Test
    fun sorteio_retorna_lista_de_mesmo_tamanho() {
        val resultado = sortearComRetry(criarGrupo(4))
        assertEquals(4, resultado.size)
    }

    @Test
    fun sorteio_minimo_3_participantes_funciona() {
        val resultado = sortearComRetry(criarGrupo(3))
        assertEquals(3, resultado.size)
    }

    @Test
    fun sorteio_grande_grupo_funciona() {
        val resultado = sortearComRetry(criarGrupo(20))
        assertEquals(20, resultado.size)
    }

    // --- Unicidade ---

    @Test
    fun sorteio_cada_participante_aparece_exatamente_uma_vez() {
        val participantes = criarGrupo(5)
        val resultado = sortearComRetry(participantes)
        val ids = mutableSetOf<Int>()
        for (p in resultado) {
            assertTrue("ID duplicado no resultado: ${p.id}", ids.add(p.id))
        }
        assertEquals(5, ids.size)
    }

    // --- Ninguem tira a si mesmo ---

    @Test
    fun sorteio_ninguem_tira_a_si_mesmo() {
        val participantes = criarGrupo(5)
        var validados = 0
        var t = 0
        while (t < 200 && validados < 20) {
            val resultado = SorteioEngine.tentarSorteio(participantes)
            if (resultado != null) {
                validados++
                for (i in participantes.indices) {
                    assertNotEquals(
                        "${participantes[i].nome} tirou a si mesmo",
                        participantes[i].id,
                        resultado[i].id
                    )
                }
            }
            t++
        }
        assertTrue("Nao obteve sorteios suficientes para validar", validados >= 5)
    }

    // --- Exclusoes ---

    @Test
    fun sorteio_respeita_exclusao_simples() {
        // A excluiu B — nos resultados validos, A nunca tira B
        val a = criar(1, "A", 2)
        val b = criar(2, "B")
        val c = criar(3, "C")
        val participantes = listOf(a, b, c)

        var validados = 0
        var t = 0
        while (t < 100 && validados < 10) {
            val resultado = SorteioEngine.tentarSorteio(participantes)
            if (resultado != null) {
                validados++
                assertNotEquals("A tirou B em violacao da exclusao", 2, resultado[0].id)
            }
            t++
        }
        assertTrue("Nao obteve sorteios suficientes para validar exclusao", validados >= 5)
    }

    @Test
    fun sorteio_dois_participantes_sem_exclusoes_funciona() {
        val participantes = criarGrupo(2)
        // Com seed fixa, o algoritmo guloso sempre acha solucao para 2 sem exclusoes
        val resultado = SorteioEngine.tentarSorteio(participantes, Random(42))
        assertNotNull(resultado)
        assertEquals(2, resultado!!.size)
        assertNotEquals(participantes[0].id, resultado[0].id)
        assertNotEquals(participantes[1].id, resultado[1].id)
    }

    @Test
    fun sorteio_retorna_null_se_impossivel() {
        val a = criar(1, "A", 2)
        val b = criar(2, "B", 1)
        assertNull(SorteioEngine.tentarSorteio(listOf(a, b)))
    }

    @Test
    fun sorteio_retorna_null_com_lista_de_um_participante() {
        assertNull(SorteioEngine.tentarSorteio(criarGrupo(1)))
    }

    @Test
    fun sorteio_com_exclusao_total_retorna_null() {
        val a = criar(1, "A", 2, 3)
        val b = criar(2, "B", 1, 3)
        val c = criar(3, "C", 1, 2)
        assertNull(SorteioEngine.tentarSorteio(listOf(a, b, c)))
    }

    // --- Imutabilidade da entrada ---

    @Test
    fun sorteio_lista_entrada_nao_e_modificada() {
        val participantes = criarGrupo(4)
        val idsBefore = participantes.map { it.id }
        SorteioEngine.tentarSorteio(participantes)
        assertEquals(idsBefore.size, participantes.size)
        for (i in participantes.indices) {
            assertEquals(idsBefore[i], participantes[i].id)
        }
    }
}