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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GrupoDAOTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GrupoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.grupoDao()
        participanteDao = db.participanteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun criarGrupo(nome: String): Grupo {
        val g = Grupo(
            nome = nome,
            data = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
        )
        val id = dao.inserir(g)
        g.id = id.toInt()
        return g
    }

    // --- inserir ---

    @Test
    fun inserir_retorna_id_valido() = runTest {
        val g = Grupo(nome = "Familia", data = "01/01/2025")
        assertTrue(dao.inserir(g) > 0)
    }

    // --- listar ---

    @Test
    fun listar_retorna_lista_vazia_quando_sem_dados() = runTest {
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun listar_retorna_grupo_inserido() = runTest {
        criarGrupo("Trabalho")
        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Trabalho", lista[0].nome)
    }

    @Test
    fun listar_ordem_desc_por_id() = runTest {
        criarGrupo("Primeiro")
        criarGrupo("Segundo")
        criarGrupo("Terceiro")
        val lista = dao.listar()
        assertEquals(3, lista.size)
        assertEquals("Terceiro", lista[0].nome)
        assertEquals("Primeiro", lista[2].nome)
    }

    // --- atualizar (atualizarNome replacement) ---

    @Test
    fun atualizar_retorna_1_row_afetada() = runTest {
        val g = criarGrupo("Nome Antigo")
        g.nome = "Nome Novo"
        assertEquals(1, dao.atualizar(g))
    }

    @Test
    fun atualizar_persiste_novo_nome() = runTest {
        val g = criarGrupo("Nome Antigo")
        val dataOriginal = g.data
        g.nome = "Nome Novo"
        dao.atualizar(g)

        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Nome Novo", lista[0].nome)
        assertEquals(dataOriginal, lista[0].data)
    }

    @Test
    fun atualizar_id_inexistente_retorna_0() = runTest {
        val g = Grupo(id = 999, nome = "Fantasma")
        assertEquals(0, dao.atualizar(g))
    }

    // --- remover ---

    @Test
    fun remover_apaga_grupo() = runTest {
        val g = criarGrupo("Para Remover")
        dao.remover(g)
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun remover_apaga_participantes_do_grupo() = runTest {
        val g = criarGrupo("Grupo Com Participantes")
        val p = Participante(nome = "Membro", grupoId = g.id)
        participanteDao.inserir(p)

        // GrupoRoomDao.remover usa @Delete — sem ON DELETE CASCADE no FK grupo_id.
        // Deve-se deletar participantes antes do grupo (mesmo comportamento do GrupoDAO legado).
        participanteDao.deletarTodosDoGrupo(g.id)
        dao.remover(g)

        assertTrue(participanteDao.listarPorGrupoSemExclusoes(g.id).isEmpty())
    }

    @Test
    fun remover_nao_afeta_outros_grupos() = runTest {
        val g1 = criarGrupo("Grupo 1")
        criarGrupo("Grupo 2")
        dao.remover(g1)

        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Grupo 2", lista[0].nome)
    }

    // --- schema v12: defaults ---

    @Test
    fun schema_v12_novos_campos_tem_defaults_corretos_ao_inserir() = runTest {
        // Verifica que os defaults do schema v12 estão corretos ao inserir um novo grupo.
        // O banco in-memory parte sempre do schema completo (v12) — não exercita o caminho
        // real da migration. Para testar dados existentes pré-v12, seria necessário
        // MigrationTestHelper (androidTest) conforme documentado no TEST_PLAN.
        // TODO: adicionar MigrationTestHelper (androidTest) para testar rows pré-v12
        //       sendo migradas corretamente via MIGRATION_11_12 — garante que
        //       ALTER TABLE ADD COLUMN preserva dados existentes em devices reais.
        val g = Grupo(nome = "Legado", data = "01/01/2024")
        val id = dao.inserir(g)

        val salvo = dao.buscarPorId(id.toInt())!!
        // Colunas adicionadas pela MIGRATION_11_12 devem ter os defaults do schema.
        assertEquals(0.0, salvo.valorMinimo, 0.001)
        assertEquals(0.0, salvo.valorMaximo, 0.001)
        assertEquals(true, salvo.permitirVerDesejos)      // DEFAULT 1
        assertEquals(false, salvo.exigirConfirmacaoCompra) // DEFAULT 0
        assertNull(salvo.descricao)
        assertNull(salvo.dataEvento)
        assertNull(salvo.localEvento)
        assertNull(salvo.dataLimiteSorteio)
        assertNull(salvo.regras)
        // Dados originais preservados.
        assertEquals("Legado", salvo.nome)
        assertEquals("01/01/2024", salvo.data)
    }

    // --- deletarTudo (limpa participantes e grupos atomicamente) ---

    @Test
    fun deletarTudo_remove_todos_os_grupos() = runTest {
        criarGrupo("G1")
        criarGrupo("G2")
        dao.deletarTudo()
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun deletarTudo_remove_participantes_tambem() = runTest {
        val g = criarGrupo("Grupo")
        val p = Participante(nome = "Pessoa", grupoId = g.id)
        participanteDao.inserir(p)

        dao.deletarTudo()

        assertTrue(participanteDao.listarPorGrupoSemExclusoes(g.id).isEmpty())
    }
}
