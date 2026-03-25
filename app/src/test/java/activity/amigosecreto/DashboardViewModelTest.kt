package activity.amigosecreto

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.db.room.SorteioRoomDao
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var sorteioDao: SorteioRoomDao
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        grupoDao = db.grupoDao()
        participanteDao = db.participanteDao()
        sorteioDao = db.sorteioDao()
        viewModel = DashboardViewModel(grupoDao, participanteDao, sorteioDao).also {
            it.ioDispatcher = testDispatcher
        }
        grupoId = grupoDao.inserir(Grupo().apply { nome = "Grupo Teste" }).toInt()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun carregarDados_grupoVazio_retornaZeros() {
        viewModel.carregarDados(grupoId)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(0, state!!.totalParticipantes)
        assertFalse(state.sorteioRealizado)
        assertEquals(0, state.confirmados)
        assertNull(state.error)
    }

    @Test
    fun carregarDados_grupoExistente_retornaGrupoNoState() {
        viewModel.carregarDados(grupoId)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertNotNull(state!!.grupo)
        assertEquals("Grupo Teste", state.grupo!!.nome)
    }

    @Test
    fun carregarDados_grupoInexistente_retornaGrupoNulo() {
        viewModel.carregarDados(Int.MAX_VALUE)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertNull(state!!.grupo)
    }

    @Test
    fun carregarDados_comParticipantes_retornaContagem() {
        runBlocking {
            participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId))
            participanteDao.inserir(Participante(nome = "Bob", grupoId = grupoId))
        }

        viewModel.carregarDados(grupoId)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(2, state!!.totalParticipantes)
    }

    @Test
    fun carregarDados_semSorteio_sorteioRealizadoFalso() {
        viewModel.carregarDados(grupoId)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertFalse(state!!.sorteioRealizado)
    }

    @Test
    fun carregarDados_comSorteio_sorteioRealizadoVerdadeiro() {
        runBlocking {
            sorteioDao.inserirSorteio(Sorteio(grupoId = grupoId, dataHora = "2025-01-01T00:00:00"))
        }

        viewModel.carregarDados(grupoId)

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state!!.sorteioRealizado)
    }
}
