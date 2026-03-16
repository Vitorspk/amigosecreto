package activity.amigosecreto.db

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParticipanteDAOTest {

    private lateinit var dao: ParticipanteDAO
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0
    private var grupoId2 = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()
        dao = ParticipanteDAO(ctx)
        dao.open()

        grupoId = grupoDao.inserir(Grupo().apply { nome = "Grupo Teste 1"; data = "01/01/2025" }).toInt()
        grupoId2 = grupoDao.inserir(Grupo().apply { nome = "Grupo Teste 2"; data = "01/01/2025" }).toInt()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        dao.close()
        grupoDao.close()
    }

    private fun criarParticipante(nome: String, gId: Int): Participante {
        val p = Participante().apply { this.nome = nome }
        dao.inserir(p, gId)
        return p
    }

    // --- inserir ---

    @Test
    fun inserir_gera_id_valido() {
        val p = Participante().apply { nome = "Ana" }
        dao.inserir(p, grupoId)
        assertTrue(p.id > 0)
    }

    // --- listarPorGrupo ---

    @Test
    fun listarPorGrupo_retorna_apenas_participantes_do_grupo() {
        criarParticipante("Ana", grupoId)
        criarParticipante("Bruno", grupoId)
        criarParticipante("Carlos", grupoId2)

        val lista = dao.listarPorGrupo(grupoId)
        assertEquals(2, lista.size)
        assertTrue(lista.all { it.nome == "Ana" || it.nome == "Bruno" })
    }

    @Test
    fun listarPorGrupo_retorna_lista_vazia_para_grupo_sem_participantes() {
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }

    @Test
    fun listarPorGrupo_ordenado_por_nome() {
        criarParticipante("Zelia", grupoId)
        criarParticipante("Ana", grupoId)
        criarParticipante("Mario", grupoId)

        val lista = dao.listarPorGrupo(grupoId)
        assertEquals("Ana", lista[0].nome)
        assertEquals("Mario", lista[1].nome)
        assertEquals("Zelia", lista[2].nome)
    }

    // --- atualizar ---

    @Test
    fun atualizar_nome_persiste() {
        val p = criarParticipante("Nome Antigo", grupoId)
        p.nome = "Nome Novo"
        assertTrue(dao.atualizar(p))

        assertEquals("Nome Novo", dao.listarPorGrupo(grupoId)[0].nome)
    }

    @Test
    fun atualizar_id_invalido_retorna_false() {
        val p = Participante().apply { id = 0; nome = "Fantasma" }
        assertFalse(dao.atualizar(p))
    }

    // --- remover ---

    @Test
    fun remover_apaga_participante() {
        val p = criarParticipante("Para Remover", grupoId)
        dao.remover(p.id)
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }

    @Test
    fun remover_nao_afeta_outros_participantes() {
        val p1 = criarParticipante("Ana", grupoId)
        criarParticipante("Bruno", grupoId)
        dao.remover(p1.id)

        val lista = dao.listarPorGrupo(grupoId)
        assertEquals(1, lista.size)
        assertEquals("Bruno", lista[0].nome)
    }

    // --- exclusoes ---

    @Test
    fun adicionarExclusao_persiste() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.adicionarExclusao(a.id, b.id)

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertTrue(aAtualizado.idsExcluidos.contains(b.id))
    }

    @Test
    fun adicionarExclusao_duplicada_nao_lanca_excecao() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.adicionarExclusao(a.id, b.id)
        dao.adicionarExclusao(a.id, b.id) // CONFLICT_IGNORE
    }

    @Test
    fun removerExclusao_remove_apenas_a_exclusao_especificada() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)
        dao.adicionarExclusao(a.id, b.id)
        dao.adicionarExclusao(a.id, c.id)
        dao.removerExclusao(a.id, b.id)

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertFalse(aAtualizado.idsExcluidos.contains(b.id))
        assertTrue(aAtualizado.idsExcluidos.contains(c.id))
    }

    // --- salvarSorteio ---

    @Test
    fun salvarSorteio_persiste_amigo_sorteado_id() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)

        assertTrue(dao.salvarSorteio(listOf(a, b, c), listOf(b, c, a)))

        val lista = dao.listarPorGrupo(grupoId)
        val aAtualizado = lista.first { it.id == a.id }
        assertNotNull(aAtualizado.amigoSorteadoId)
        assertEquals(b.id, aAtualizado.amigoSorteadoId)
    }

    @Test
    fun salvarSorteio_atualiza_amigo_sorteado_id_para_cada_participante() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)

        dao.salvarSorteio(listOf(a, b, c), listOf(b, c, a))

        val lista = dao.listarPorGrupo(grupoId)
        for (p in lista) {
            assertNotNull(p.amigoSorteadoId)
            assertTrue(p.amigoSorteadoId!! > 0)
        }
    }

    // --- marcarComoEnviado ---

    @Test
    fun marcarComoEnviado_seta_flag_enviado() {
        val p = criarParticipante("Enviado", grupoId)
        dao.marcarComoEnviado(p.id)

        val atualizado = dao.buscarPorId(p.id)
        assertNotNull(atualizado)
        assertTrue(atualizado!!.isEnviado)
    }

    // --- contarPorGrupo ---

    @Test
    fun contarPorGrupo_retorna_mapa_correto() {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId)
        criarParticipante("D", grupoId2)
        criarParticipante("E", grupoId2)

        val mapa = dao.contarPorGrupo()
        assertEquals(3, mapa[grupoId])
        assertEquals(2, mapa[grupoId2])
    }

    @Test
    fun contarPorGrupo_grupo_vazio_nao_esta_no_mapa() {
        assertFalse(dao.contarPorGrupo().containsKey(grupoId))
    }

    // --- buscarPorId ---

    @Test
    fun buscarPorId_existente_retorna_participante() {
        val p = criarParticipante("Ana", grupoId)
        val encontrado = dao.buscarPorId(p.id)
        assertNotNull(encontrado)
        assertEquals("Ana", encontrado!!.nome)
        assertEquals(p.id, encontrado.id)
    }

    @Test
    fun buscarPorId_inexistente_retorna_null() {
        assertNull(dao.buscarPorId(-1))
    }

    // --- limparSorteioDoGrupo ---

    @Test
    fun limparSorteioDoGrupo_nulifica_amigo_sorteado_e_reset_enviado() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)

        dao.salvarSorteio(listOf(a, b, c), listOf(b, c, a))
        dao.marcarComoEnviado(a.id)
        dao.limparSorteioDoGrupo(grupoId)

        for (p in dao.listarPorGrupo(grupoId)) {
            assertNull(p.amigoSorteadoId)
            assertFalse(p.isEnviado)
        }
    }

    // --- deletarTodosDoGrupo ---

    @Test
    fun deletarTodosDoGrupo_remove_todos_participantes() {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId2)

        dao.deletarTodosDoGrupo(grupoId)

        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
        assertEquals(1, dao.listarPorGrupo(grupoId2).size)
    }

    // --- getNomeAmigoSorteado ---

    @Test
    fun getNomeAmigoSorteado_retorna_nome_correto() {
        val a = criarParticipante("Ana", grupoId)
        val b = criarParticipante("Beatriz", grupoId)
        val c = criarParticipante("Carlos", grupoId)
        dao.salvarSorteio(listOf(a, b, c), listOf(b, c, a))

        val anaAtualizada = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertNotNull(anaAtualizada.amigoSorteadoId)
        assertEquals("Beatriz", dao.getNomeAmigoSorteado(anaAtualizada.amigoSorteadoId!!))
    }

    @Test
    fun getNomeAmigoSorteado_id_inexistente_retorna_ninguem() {
        assertEquals("Ninguém", dao.getNomeAmigoSorteado(-1))
    }

    // --- salvarSorteio tamanhos diferentes ---

    @Test
    fun salvarSorteio_listas_tamanhos_diferentes_retorna_false() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)

        val ok = dao.salvarSorteio(listOf(a, b, c), listOf(b, c))
        assertFalse(ok)

        for (p in dao.listarPorGrupo(grupoId)) {
            assertNull(p.amigoSorteadoId)
        }
    }

    // --- adicionarExclusao idempotência ---

    @Test
    fun adicionarExclusao_duplicada_nao_insere_registro_extra() {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.adicionarExclusao(a.id, b.id)
        dao.adicionarExclusao(a.id, b.id)

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertEquals(1, aAtualizado.idsExcluidos.size)
        assertTrue(aAtualizado.idsExcluidos.contains(b.id))
    }

    // --- deletarTodosDoGrupo grupo vazio ---

    @Test
    fun deletarTodosDoGrupo_grupo_vazio_nao_lanca_excecao() {
        dao.deletarTodosDoGrupo(grupoId)
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }
}