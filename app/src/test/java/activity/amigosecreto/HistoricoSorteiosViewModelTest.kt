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
 * Usa UnconfinedTestDispatcher como main dispatcher. Como o ViewModel usa
 * withContext(Dispatchers.IO), usamos runBlocking para aguardar a coroutine
 * do viewModelScope antes de fazer as asserções.
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
        viewModel = HistoricoSorteiosViewModel(repository)
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

    /** Chama carregarHistorico e aguarda a coroutine via runBlocking + Thread.sleep mínimo. */
    private fun carregarEAguardar(gId: Int = grupoId) {
        viewModel.carregarHistorico(gId)
        // viewModelScope.launch + withContext(Dispatchers.IO): o UnconfinedTestDispatcher como
        // Main faz o launch rodar imediatamente, mas o IO ainda é real.
        // allowMainThreadQueries + in-memory DB respondem em < 50ms.
        Thread.sleep(200)
    }

    // =========================================================
    // isLoading
    // =========================================================

    @Test
    fun isLoading_inicial_e_null() {
        assertNull(viewModel.isLoading.value)
    }

    @Test
    fun carregarHistorico_isLoading_termina_false() {
        carregarEAguardar()
        assertFalse(viewModel.isLoading.value ?: true)
    }

    // =========================================================
    // sorteios LiveData
    // =========================================================

    @Test
    fun carregarHistorico_grupo_sem_sorteios_retorna_lista_vazia() {
        carregarEAguardar()

        val sorteios = viewModel.sorteios.value
        assertNotNull(sorteios)
        assertTrue(sorteios!!.isEmpty())
    }

    @Test
    fun carregarHistorico_grupo_com_sorteio_retorna_lista_com_um_item() = runBlocking {
        salvarSorteio()
        carregarEAguardar()

        val sorteios = viewModel.sorteios.value
        assertNotNull(sorteios)
        assertEquals(1, sorteios!!.size)
    }

    @Test
    fun carregarHistorico_retorna_sorteios_com_pares_populados() = runBlocking {
        salvarSorteio()
        carregarEAguardar()

        val sorteios = viewModel.sorteios.value!!
        assertEquals(3, sorteios[0].pares.size)
    }

    @Test
    fun carregarHistorico_multiplos_sorteios_retorna_todos() = runBlocking {
        salvarSorteio()
        salvarSorteio()
        carregarEAguardar()

        val sorteios = viewModel.sorteios.value!!
        assertEquals(2, sorteios.size)
    }

    @Test
    fun carregarHistorico_nao_retorna_sorteios_de_outro_grupo() = runBlocking {
        salvarSorteio()
        val outroGrupoId = grupoDao.inserir(Grupo(nome = "Outro")).toInt()

        carregarEAguardar(outroGrupoId)

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
    fun carregarHistorico_sem_erro_nao_posta_hasError() {
        carregarEAguardar()
        assertNull(viewModel.hasError.value)
    }

    @Test
    fun carregarHistorico_com_repositorio_que_lanca_excecao_posta_hasError() {
        val fakeRepository = object : SorteioRepository(sorteioDao) {
            override suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> {
                throw RuntimeException("DB error simulado")
            }
        }
        val vmComErro = HistoricoSorteiosViewModel(fakeRepository)

        vmComErro.carregarHistorico(grupoId)
        Thread.sleep(200)

        assertTrue(vmComErro.hasError.value ?: false)
    }

    @Test
    fun carregarHistorico_com_erro_isLoading_fica_false() {
        val fakeRepository = object : SorteioRepository(sorteioDao) {
            override suspend fun listarPorGrupo(grupoId: Int): List<Sorteio> {
                throw RuntimeException("DB error simulado")
            }
        }
        val vmComErro = HistoricoSorteiosViewModel(fakeRepository)

        vmComErro.carregarHistorico(grupoId)
        Thread.sleep(200)

        assertFalse(vmComErro.isLoading.value ?: true)
    }
}
