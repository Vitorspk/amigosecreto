package activity.amigosecreto

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.repository.BackupRepository
import activity.amigosecreto.repository.GruposRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.util.BackupManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários de GruposViewModel via Robolectric + Room in-memory +
 * InstantTaskExecutorRule.
 *
 * Segue o mesmo padrão de ParticipantesViewModelTest:
 * - ioDispatcher substituído por UnconfinedTestDispatcher → operações assíncronas completam
 *   de forma síncrona antes das asserções.
 * - BackupRepository injetado via subclasse anônima (open class) para isolar testes de exportação
 *   e importação sem depender do filesystem real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GruposViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var app: android.app.Application
    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteRoomDao: ParticipanteRoomDao
    private lateinit var gruposRepository: GruposRepository
    private lateinit var participanteRepository: ParticipanteRepository
    private lateinit var viewModel: GruposViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    // BackupRepository fake que não usa filesystem
    private val fakeBackupRepository = object : BackupRepository(ApplicationProvider.getApplicationContext()) {
        var exportarResult: String? = "{\"grupos\":[]}"
        var importarResult: BackupManager.ImportResult = BackupManager.ImportResult.Success(0)
        var shouldThrow = false

        override fun exportar(): String {
            if (shouldThrow) throw RuntimeException("Erro simulado de exportação")
            return exportarResult ?: throw RuntimeException("resultado nulo")
        }

        override fun importar(jsonString: String): BackupManager.ImportResult {
            if (shouldThrow) throw RuntimeException("Erro simulado de importação")
            return importarResult
        }
    }

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        grupoDao = db.grupoDao()
        participanteRoomDao = db.participanteDao()

        gruposRepository = GruposRepository(grupoDao)
        participanteRepository = ParticipanteRepository(participanteRoomDao)

        viewModel = GruposViewModel(app, gruposRepository, participanteRepository, fakeBackupRepository)
        viewModel.ioDispatcher = testDispatcher
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    // --- helpers ---

    private suspend fun inserirGrupo(nome: String, dataEvento: String? = null): Grupo {
        val g = Grupo(nome = nome, dataEvento = dataEvento)
        val id = grupoDao.inserir(g).toInt()
        g.id = id
        return g
    }

    private suspend fun inserirParticipante(grupoId: Int, enviado: Boolean = false): Participante {
        val p = Participante(nome = "P$grupoId", grupoId = grupoId)
        p.isEnviado = enviado
        val id = participanteRoomDao.inserir(p).toInt()
        p.id = id
        return p
    }

    // =========================================================
    // carregarGrupos — caminho feliz
    // =========================================================

    @Test
    fun carregarGrupos_lista_vazia_publica_lista_vazia() = runTest {
        viewModel.carregarGrupos()

        assertEquals(emptyList<GruposViewModel.GrupoComContagem>(), viewModel.grupos.value)
    }

    @Test
    fun carregarGrupos_publica_grupos_com_contagem_correta() = runTest {
        val g = inserirGrupo("Amigos")
        inserirParticipante(g.id)
        inserirParticipante(g.id)
        inserirParticipante(g.id, enviado = true)

        viewModel.carregarGrupos()

        val lista = viewModel.grupos.value!!
        assertEquals(1, lista.size)
        assertEquals("Amigos", lista[0].grupo.nome)
        assertEquals(3, lista[0].totalParticipantes)
        assertEquals(1, lista[0].totalEnviados)
    }

    @Test
    fun carregarGrupos_define_isLoading_false_apos_conclusao() = runTest {
        viewModel.carregarGrupos()

        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun carregarGrupos_nao_posta_errorMessage_no_caminho_feliz() = runTest {
        inserirGrupo("Familia")
        viewModel.carregarGrupos()

        assertNull(viewModel.errorMessage.value)
    }

    // =========================================================
    // carregarGrupos — ordenação
    // =========================================================

    @Test
    fun carregarGrupos_sort_nome_ordena_case_insensitive() = runTest {
        inserirGrupo("zebra")
        inserirGrupo("AMIGOS")
        inserirGrupo("Familia")

        viewModel.carregarGrupos(GruposViewModel.SORT_NOME)

        val nomes = viewModel.grupos.value!!.map { it.grupo.nome }
        assertEquals(listOf("AMIGOS", "Familia", "zebra"), nomes)
    }

    @Test
    fun carregarGrupos_sort_criacao_retorna_ordem_decrescente_de_id() = runTest {
        inserirGrupo("Primeiro")
        inserirGrupo("Segundo")
        inserirGrupo("Terceiro")

        viewModel.carregarGrupos(GruposViewModel.SORT_CRIACAO)

        val nomes = viewModel.grupos.value!!.map { it.grupo.nome }
        // DAO retorna por id DESC — mais recente primeiro
        assertEquals(listOf("Terceiro", "Segundo", "Primeiro"), nomes)
    }

    @Test
    fun carregarGrupos_sort_evento_ordena_por_data() = runTest {
        inserirGrupo("Natal", dataEvento = "25/12/2025")
        inserirGrupo("Carnaval", dataEvento = "04/03/2025")
        inserirGrupo("Reveillon", dataEvento = "31/12/2025")

        viewModel.carregarGrupos(GruposViewModel.SORT_EVENTO)

        val nomes = viewModel.grupos.value!!.map { it.grupo.nome }
        assertEquals(listOf("Carnaval", "Natal", "Reveillon"), nomes)
    }

    @Test
    fun carregarGrupos_sort_evento_grupos_sem_data_ficam_no_inicio() = runTest {
        inserirGrupo("Sem data")
        inserirGrupo("Com data", dataEvento = "01/06/2025")

        viewModel.carregarGrupos(GruposViewModel.SORT_EVENTO)

        val nomes = viewModel.grupos.value!!.map { it.grupo.nome }
        // nulls (sem dataEvento) vêm antes — sortedWith compareBy coloca null primeiro
        assertEquals("Sem data", nomes[0])
        assertEquals("Com data", nomes[1])
    }

    // =========================================================
    // inserirGrupo
    // =========================================================

    @Test
    fun inserirGrupo_posta_operacaoSucesso_true() = runTest {
        viewModel.inserirGrupo("Novo Grupo", "01/12/2025")

        assertEquals(true, viewModel.operacaoSucesso.value)
    }

    @Test
    fun inserirGrupo_recarrega_lista_apos_insercao() = runTest {
        viewModel.inserirGrupo("Amigos", "15/12/2025")

        val lista = viewModel.grupos.value!!
        assertEquals(1, lista.size)
        assertEquals("Amigos", lista[0].grupo.nome)
    }

    @Test
    fun inserirGrupo_varios_grupos_todos_aparecem_na_lista() = runTest {
        viewModel.inserirGrupo("A", "01/12/2025")
        viewModel.inserirGrupo("B", "02/12/2025")
        viewModel.inserirGrupo("C", "03/12/2025")

        assertEquals(3, viewModel.grupos.value!!.size)
    }

    // =========================================================
    // atualizarNomeGrupo
    // =========================================================

    @Test
    fun atualizarNomeGrupo_posta_operacaoSucesso_true_para_grupo_existente() = runTest {
        val g = inserirGrupo("Original")
        g.nome = "Atualizado"

        viewModel.atualizarNomeGrupo(g)

        assertEquals(true, viewModel.operacaoSucesso.value)
    }

    @Test
    fun atualizarNomeGrupo_recarrega_lista_com_nome_novo() = runTest {
        val g = inserirGrupo("Original")
        g.nome = "Atualizado"

        viewModel.atualizarNomeGrupo(g)

        val lista = viewModel.grupos.value!!
        assertEquals(1, lista.size)
        assertEquals("Atualizado", lista[0].grupo.nome)
    }

    @Test
    fun atualizarNomeGrupo_retorna_false_para_grupo_inexistente() = runTest {
        val grupoFantasma = Grupo(id = 9999, nome = "Fantasma")

        viewModel.atualizarNomeGrupo(grupoFantasma)

        assertEquals(false, viewModel.operacaoSucesso.value)
    }

    @Test
    fun atualizarNomeGrupo_posta_errorMessage_quando_nao_salvo() = runTest {
        val grupoFantasma = Grupo(id = 9999, nome = "Inexistente")

        viewModel.atualizarNomeGrupo(grupoFantasma)

        assertNotNull(viewModel.errorMessage.value)
    }

    // =========================================================
    // removerGrupo
    // =========================================================

    @Test
    fun removerGrupo_remove_da_lista() = runTest {
        val g = inserirGrupo("Temporario")
        viewModel.carregarGrupos()
        assertEquals(1, viewModel.grupos.value!!.size)

        viewModel.removerGrupo(g)

        assertEquals(0, viewModel.grupos.value!!.size)
    }

    @Test
    fun removerGrupo_remove_apenas_o_grupo_correto() = runTest {
        val g1 = inserirGrupo("Manter")
        val g2 = inserirGrupo("Remover")

        viewModel.removerGrupo(g2)

        val lista = viewModel.grupos.value!!
        assertEquals(1, lista.size)
        assertEquals("Manter", lista[0].grupo.nome)
    }

    // =========================================================
    // limparTudo
    // =========================================================

    @Test
    fun limparTudo_remove_todos_os_grupos() = runTest {
        inserirGrupo("A")
        inserirGrupo("B")
        inserirGrupo("C")

        viewModel.limparTudo()

        assertEquals(0, viewModel.grupos.value!!.size)
    }

    @Test
    fun limparTudo_em_banco_vazio_nao_posta_errorMessage() = runTest {
        viewModel.limparTudo()

        assertNull(viewModel.errorMessage.value)
    }

    // =========================================================
    // exportarBackup
    // =========================================================

    @Test
    fun exportarBackup_posta_json_no_exportarResultado() = runTest {
        fakeBackupRepository.exportarResult = """{"grupos":[{"nome":"Familia"}]}"""

        viewModel.exportarBackup()

        assertEquals("""{"grupos":[{"nome":"Familia"}]}""", viewModel.exportarResultado.value)
    }

    @Test
    fun exportarBackup_posta_null_quando_repositorio_lanca_excecao() = runTest {
        fakeBackupRepository.shouldThrow = true

        viewModel.exportarBackup()

        assertNull(viewModel.exportarResultado.value)
    }

    // =========================================================
    // importarBackup
    // =========================================================

    @Test
    fun importarBackup_sucesso_posta_ImportarResultado_Sucesso() = runTest {
        fakeBackupRepository.importarResult = BackupManager.ImportResult.Success(3)

        viewModel.importarBackup("{}")

        val resultado = viewModel.importarResultado.value
        assertNotNull(resultado)
        assertTrue(resultado is GruposViewModel.ImportarResultado.Sucesso)
        assertEquals(3, (resultado as GruposViewModel.ImportarResultado.Sucesso).gruposImportados)
    }

    @Test
    fun importarBackup_falha_posta_ImportarResultado_Falha() = runTest {
        fakeBackupRepository.importarResult = BackupManager.ImportResult.Failure("json inválido")

        viewModel.importarBackup("{}")

        val resultado = viewModel.importarResultado.value
        assertTrue(resultado is GruposViewModel.ImportarResultado.Falha)
    }

    @Test
    fun importarBackup_excecao_posta_ImportarResultado_Falha() = runTest {
        fakeBackupRepository.shouldThrow = true

        viewModel.importarBackup("{}")

        val resultado = viewModel.importarResultado.value
        assertTrue(resultado is GruposViewModel.ImportarResultado.Falha)
    }

    @Test
    fun importarBackup_sucesso_recarrega_lista_de_grupos() = runTest {
        inserirGrupo("Pre-existente")
        fakeBackupRepository.importarResult = BackupManager.ImportResult.Success(1)

        viewModel.importarBackup("{}")

        // Após importar com sucesso, carregarGrupos é chamado internamente
        assertNotNull(viewModel.grupos.value)
    }

    // =========================================================
    // clearers — LiveData cleanup
    // =========================================================

    @Test
    fun clearErrorMessage_zera_errorMessage() = runTest {
        inserirGrupo("Teste")
        viewModel.atualizarNomeGrupo(Grupo(id = 9999, nome = "X"))
        assertNotNull(viewModel.errorMessage.value)

        viewModel.clearErrorMessage()

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun clearOperacaoSucesso_zera_operacaoSucesso() = runTest {
        viewModel.inserirGrupo("Grupo", "01/12/2025")
        assertNotNull(viewModel.operacaoSucesso.value)

        viewModel.clearOperacaoSucesso()

        assertNull(viewModel.operacaoSucesso.value)
    }

    @Test
    fun clearExportarResultado_zera_exportarResultado() = runTest {
        viewModel.exportarBackup()
        assertNotNull(viewModel.exportarResultado.value)

        viewModel.clearExportarResultado()

        assertNull(viewModel.exportarResultado.value)
    }

    @Test
    fun clearImportarResultado_zera_importarResultado() = runTest {
        fakeBackupRepository.importarResult = BackupManager.ImportResult.Success(0)
        viewModel.importarBackup("{}")
        assertNotNull(viewModel.importarResultado.value)

        viewModel.clearImportarResultado()

        assertNull(viewModel.importarResultado.value)
    }

    // =========================================================
    // GrupoComContagem — data class
    // =========================================================

    @Test
    fun grupoComContagem_igualdade_com_mesma_instancia_de_grupo() {
        val g = Grupo(id = 1, nome = "A")
        val c1 = GruposViewModel.GrupoComContagem(g, 5, 2)
        val c2 = GruposViewModel.GrupoComContagem(g, 5, 2)
        assertEquals(c1, c2)
    }

    @Test
    fun grupoComContagem_desigualdade_quando_enviados_difere() {
        val g = Grupo(id = 1, nome = "A")
        val c1 = GruposViewModel.GrupoComContagem(g, 5, 0)
        val c2 = GruposViewModel.GrupoComContagem(g, 5, 3)
        assertNotEquals(c1, c2)
    }

    // =========================================================
    // ImportarResultado — sealed class
    // =========================================================

    @Test
    fun importarResultado_sucesso_carrega_quantidade() {
        val r = GruposViewModel.ImportarResultado.Sucesso(7)
        assertEquals(7, r.gruposImportados)
    }

    @Test
    fun importarResultado_falha_e_objeto_singleton() {
        val r1 = GruposViewModel.ImportarResultado.Falha
        val r2 = GruposViewModel.ImportarResultado.Falha
        assertSame(r1, r2)
    }
}
