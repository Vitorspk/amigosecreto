package activity.amigosecreto.db

import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

    private lateinit var db: AppDatabase
    private lateinit var dao: ParticipanteRoomDao
    private lateinit var grupoDao: GrupoRoomDao
    private var grupoId = 0
    private var grupoId2 = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grupoDao = db.grupoDao()
        dao = db.participanteDao()

        grupoId = grupoDao.inserir(Grupo(nome = "Grupo Teste 1", data = "01/01/2025")).toInt()
        grupoId2 = grupoDao.inserir(Grupo(nome = "Grupo Teste 2", data = "01/01/2025")).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun criarParticipante(nome: String, gId: Int): Participante {
        val p = Participante(nome = nome, grupoId = gId)
        val id = dao.inserir(p).toInt()
        p.id = id
        return p
    }

    // --- inserir ---

    @Test
    fun inserir_gera_id_valido() = runTest {
        val p = Participante(nome = "Ana", grupoId = grupoId)
        val id = dao.inserir(p)
        assertTrue(id > 0)
    }

    // --- listarPorGrupo ---

    @Test
    fun listarPorGrupo_retorna_apenas_participantes_do_grupo() = runTest {
        criarParticipante("Ana", grupoId)
        criarParticipante("Bruno", grupoId)
        criarParticipante("Carlos", grupoId2)

        val lista = dao.listarPorGrupo(grupoId)
        assertEquals(2, lista.size)
        assertTrue(lista.all { it.nome == "Ana" || it.nome == "Bruno" })
    }

    @Test
    fun listarPorGrupo_retorna_lista_vazia_para_grupo_sem_participantes() = runTest {
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }

    @Test
    fun listarPorGrupo_ordenado_por_nome() = runTest {
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
    fun atualizar_nome_persiste() = runTest {
        val p = criarParticipante("Nome Antigo", grupoId)
        p.nome = "Nome Novo"
        assertTrue(dao.atualizar(p) > 0)

        assertEquals("Nome Novo", dao.listarPorGrupo(grupoId)[0].nome)
    }

    @Test
    fun atualizar_id_invalido_retorna_zero() = runTest {
        val p = Participante(id = 0, nome = "Fantasma")
        assertEquals(0, dao.atualizar(p))
    }

    // --- remover ---

    @Test
    fun remover_apaga_participante() = runTest {
        val p = criarParticipante("Para Remover", grupoId)
        dao.remover(p.id)
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }

    @Test
    fun remover_nao_afeta_outros_participantes() = runTest {
        val p1 = criarParticipante("Ana", grupoId)
        criarParticipante("Bruno", grupoId)
        dao.remover(p1.id)

        val lista = dao.listarPorGrupo(grupoId)
        assertEquals(1, lista.size)
        assertEquals("Bruno", lista[0].nome)
    }

    // --- exclusoes ---

    @Test
    fun adicionarExclusao_persiste() = runTest {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.inserirExclusao(Exclusao(a.id, b.id))

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertTrue(aAtualizado.idsExcluidos.contains(b.id))
    }

    @Test
    fun adicionarExclusao_duplicada_nao_lanca_excecao() = runTest {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.inserirExclusao(Exclusao(a.id, b.id))
        dao.inserirExclusao(Exclusao(a.id, b.id)) // IGNORE on conflict
    }

    @Test
    fun removerExclusao_remove_apenas_a_exclusao_especificada() = runTest {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        val c = criarParticipante("C", grupoId)
        dao.inserirExclusao(Exclusao(a.id, b.id))
        dao.inserirExclusao(Exclusao(a.id, c.id))
        dao.removerExclusao(a.id, b.id)

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertFalse(aAtualizado.idsExcluidos.contains(b.id))
        assertTrue(aAtualizado.idsExcluidos.contains(c.id))
    }

    // --- salvarSorteio ---

    @Test
    fun salvarSorteio_persiste_amigo_sorteado_id() = runTest {
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
    fun salvarSorteio_atualiza_amigo_sorteado_id_para_cada_participante() = runTest {
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
    fun marcarComoEnviado_seta_flag_enviado() = runTest {
        val p = criarParticipante("Enviado", grupoId)
        dao.marcarComoEnviado(p.id)

        val atualizado = dao.buscarPorId(p.id)
        assertNotNull(atualizado)
        assertTrue(atualizado!!.isEnviado)
    }

    // --- contarPorGrupo ---

    @Test
    fun contarPorGrupo_retorna_mapa_correto() = runTest {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId)
        criarParticipante("D", grupoId2)
        criarParticipante("E", grupoId2)

        val mapa = dao.contarPorTodosGrupos().associate { it.grupoId to it.count }
        assertEquals(3, mapa[grupoId])
        assertEquals(2, mapa[grupoId2])
    }

    @Test
    fun contarPorGrupo_grupo_vazio_nao_esta_no_mapa() = runTest {
        val mapa = dao.contarPorTodosGrupos().associate { it.grupoId to it.count }
        assertFalse(mapa.containsKey(grupoId))
    }

    // --- buscarPorId ---

    @Test
    fun buscarPorId_existente_retorna_participante() = runTest {
        val p = criarParticipante("Ana", grupoId)
        val encontrado = dao.buscarPorId(p.id)
        assertNotNull(encontrado)
        assertEquals("Ana", encontrado!!.nome)
        assertEquals(p.id, encontrado.id)
    }

    @Test
    fun buscarPorId_inexistente_retorna_null() = runTest {
        assertNull(dao.buscarPorId(-1))
    }

    // --- limparSorteioDoGrupo ---

    @Test
    fun limparSorteioDoGrupo_nulifica_amigo_sorteado_e_reset_enviado() = runTest {
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
    fun deletarTodosDoGrupo_remove_todos_participantes() = runTest {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId2)

        dao.deletarTodosDoGrupo(grupoId)

        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
        assertEquals(1, dao.listarPorGrupo(grupoId2).size)
    }

    // --- getNome (getNomeAmigoSorteado replacement) ---

    @Test
    fun getNome_retorna_nome_correto() = runTest {
        val a = criarParticipante("Ana", grupoId)
        val b = criarParticipante("Beatriz", grupoId)
        val c = criarParticipante("Carlos", grupoId)
        dao.salvarSorteio(listOf(a, b, c), listOf(b, c, a))

        val anaAtualizada = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertNotNull(anaAtualizada.amigoSorteadoId)
        assertEquals("Beatriz", dao.getNome(anaAtualizada.amigoSorteadoId!!))
    }

    @Test
    fun getNome_id_inexistente_retorna_null() = runTest {
        assertNull(dao.getNome(-1))
    }

    // --- salvarSorteio tamanhos diferentes ---

    @Test
    fun salvarSorteio_listas_tamanhos_diferentes_retorna_false() = runTest {
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
    fun adicionarExclusao_duplicada_nao_insere_registro_extra() = runTest {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        dao.inserirExclusao(Exclusao(a.id, b.id))
        dao.inserirExclusao(Exclusao(a.id, b.id))

        val aAtualizado = dao.listarPorGrupo(grupoId).first { it.id == a.id }
        assertEquals(1, aAtualizado.idsExcluidos.size)
        assertTrue(aAtualizado.idsExcluidos.contains(b.id))
    }

    // --- deletarTodosDoGrupo grupo vazio ---

    @Test
    fun deletarTodosDoGrupo_grupo_vazio_nao_lanca_excecao() = runTest {
        dao.deletarTodosDoGrupo(grupoId)
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty())
    }

    // --- contarGruposPendentes ---

    @Test
    fun contarGruposPendentes_zero_quando_banco_vazio() = runTest {
        assertEquals(0, dao.contarGruposPendentes())
    }

    @Test
    fun contarGruposPendentes_zero_quando_grupo_tem_menos_de_3_participantes() = runTest {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        assertEquals(0, dao.contarGruposPendentes())
    }

    @Test
    fun contarGruposPendentes_conta_grupo_com_3_ou_mais_sem_sorteio() = runTest {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId)
        assertEquals(1, dao.contarGruposPendentes())
    }

    @Test
    fun contarGruposPendentes_ignora_grupo_com_sorteio_realizado() = runTest {
        val a = criarParticipante("A", grupoId)
        val b = criarParticipante("B", grupoId)
        criarParticipante("C", grupoId)
        // Simula sorteio realizado — a tira b
        dao.atualizarAmigoSorteado(a.id, b.id)
        assertEquals(0, dao.contarGruposPendentes())
    }

    @Test
    fun contarGruposPendentes_conta_apenas_grupos_pendentes_quando_ha_mistura() = runTest {
        // grupoId: 3 participantes sem sorteio → pendente
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        criarParticipante("C", grupoId)
        // grupoId2: 3 participantes com sorteio → não pendente
        val x = criarParticipante("X", grupoId2)
        val y = criarParticipante("Y", grupoId2)
        criarParticipante("Z", grupoId2)
        dao.atualizarAmigoSorteado(x.id, y.id)

        assertEquals(1, dao.contarGruposPendentes())
    }

    @Test
    fun contarGruposPendentes_respeita_minParticipantes_customizado() = runTest {
        criarParticipante("A", grupoId)
        criarParticipante("B", grupoId)
        // Com minParticipantes=2 deve contar; com 3 não deve
        assertEquals(1, dao.contarGruposPendentes(minParticipantes = 2))
        assertEquals(0, dao.contarGruposPendentes(minParticipantes = 3))
    }
}
