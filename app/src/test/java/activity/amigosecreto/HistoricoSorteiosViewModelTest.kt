package activity.amigosecreto

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.db.room.SorteioRoomDao
import activity.amigosecreto.repository.SorteioRepository
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
 * Testes unitários de HistoricoSorteiosViewModel via Robolectric + Room in-memory.
 *
 * Usa UnconfinedTestDispatcher tanto como main quanto como ioDispatcher injetado,
 * permitindo que advanceUntilIdle() controle todas as coroutines sem Thread.sleep.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoricoSorteiosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var sorteioDao: SorteioRoomDao
    private lateinit var repository: SorteioRepository
    private lateinit var viewModel: HistoricoSorteiosViewModel
    private var grupoId = 0

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)

        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grupoDao = db.grupoDao()
        participanteDao = db.participanteDao()
        sorteioDao = db.sorteioDao()

        val grupo = Grupo(nome = "Grupo Natal")
        grupoId = grupoDao.inserir(grupo).toInt()

        repository = SorteioRepository(sorteioDao)
        // Injeta testDispatcher como ioDispatcher — elimina Thread.sleep
        viewModel = HistoricoSorteiosViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun criarParticipante(nome: String): Participante {
        val p = Participante(nome = nome, grupoId = grupoId)
        val id = participanteDao.inserir(p).toInt()
        p.id = id
        return p
    }

    private suspend fun salvarSorteio() {
        val p1 = criarParticipante("Ana")
        val p2 = criarParticipante("Bruno")
        val p3 = criarParticipante("Carlos")
        repository.salvarSorteioCompleto(grupoId, listOf(p1, p2, p3), listOf(p2, p3, p1))
    }

    /** Cria um ViewModel com repositório que sempre lança exceção. */
    private fun criarVMComErro(): HistoricoSorteiosViewModel {
        val fakeRepo = object : SorteioRepository(sorteioDao) {
            override suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> {
                throw RuntimeException("DB error simulado")
            }
        }
        return HistoricoSorteiosViewModel(fakeRepo, testDispatcher)
    }

    // =========================================================
    // isLoading
    // =========================================================

    @Test
    fun isLoading_inicial_e_null() {
        assertNull(viewModel.isLoading.value)
    }

    @Test
    fun carregarHistorico_isLoading_termina_false() = runTest(testDispatcher) {
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value ?: true)
    }

    // =========================================================
    // sorteios LiveData
    // =========================================================

    @Test
    fun carregarHistorico_grupo_sem_sorteios_retorna_lista_vazia() = runTest(testDispatcher) {
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()

        val sorteios = viewModel.sorteios.value
        assertNotNull(sorteios)
        assertTrue(sorteios!!.isEmpty())
    }

    @Test
    fun carregarHistorico_grupo_com_sorteio_retorna_lista_com_um_item() = runTest(testDispatcher) {
        salvarSorteio()
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()

        val sorteios = viewModel.sorteios.value
        assertNotNull(sorteios)
        assertEquals(1, sorteios!!.size)
    }

    @Test
    fun carregarHistorico_retorna_sorteios_com_pares_populados() = runTest(testDispatcher) {
        salvarSorteio()
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()

        val sorteios = viewModel.sorteios.value!!
        assertEquals(3, sorteios[0].pares.size)
    }

    @Test
    fun carregarHistorico_multiplos_sorteios_retorna_todos() = runTest(testDispatcher) {
        salvarSorteio()
        salvarSorteio()
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()

        val sorteios = viewModel.sorteios.value!!
        assertEquals(2, sorteios.size)
    }

    @Test
    fun carregarHistorico_nao_retorna_sorteios_de_outro_grupo() = runTest(testDispatcher) {
        salvarSorteio()
        val outroGrupoId = grupoDao.inserir(Grupo(nome = "Outro")).toInt()

        viewModel.carregarHistorico(outroGrupoId)
        advanceUntilIdle()

        val sorteios = viewModel.sorteios.value!!
        assertTrue(sorteios.isEmpty())
    }

    // =========================================================
    // hasError LiveData
    // =========================================================

    @Test
    fun hasError_inicial_e_null() {
        assertNull(viewModel.hasError.value)
    }

    @Test
    fun carregarHistorico_sem_erro_nao_posta_hasError() = runTest(testDispatcher) {
        viewModel.carregarHistorico(grupoId)
        advanceUntilIdle()
        assertNull(viewModel.hasError.value)
    }

    @Test
    fun carregarHistorico_com_repositorio_que_lanca_excecao_posta_hasError() = runTest(testDispatcher) {
        val vmComErro = criarVMComErro()
        vmComErro.carregarHistorico(grupoId)
        advanceUntilIdle()
        assertTrue(vmComErro.hasError.value ?: false)
    }

    @Test
    fun carregarHistorico_com_erro_isLoading_fica_false() = runTest(testDispatcher) {
        val vmComErro = criarVMComErro()
        vmComErro.carregarHistorico(grupoId)
        advanceUntilIdle()
        assertFalse(vmComErro.isLoading.value ?: true)
    }
}
