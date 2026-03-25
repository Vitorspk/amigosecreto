package activity.amigosecreto

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
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
class EstatisticasViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var viewModel: EstatisticasViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        grupoDao = db.grupoDao()
        viewModel = EstatisticasViewModel(grupoDao).also {
            it.ioDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun carregarEstatisticas_bancoVazio_retornaZeros() {
        viewModel.carregarEstatisticas()

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(0, state!!.totalGrupos)
        assertEquals(0, state.totalParticipantes)
        assertEquals(0, state.totalSorteios)
        assertEquals(0, state.totalDesejos)
        assertNull(state.mediaValor)
        assertNull(state.error)
    }

    @Test
    fun carregarEstatisticas_comGrupos_retornaContagem() {
        runBlocking {
            grupoDao.inserir(Grupo().apply { nome = "Grupo 1" })
            grupoDao.inserir(Grupo().apply { nome = "Grupo 2" })
        }

        viewModel.carregarEstatisticas()

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(2, state!!.totalGrupos)
    }

    @Test
    fun carregarEstatisticas_comParticipantes_retornaContagem() {
        runBlocking {
            val grupoId = grupoDao.inserir(Grupo().apply { nome = "G" }).toInt()
            db.participanteDao().inserir(Participante().apply { nome = "P1"; this.grupoId = grupoId })
            db.participanteDao().inserir(Participante().apply { nome = "P2"; this.grupoId = grupoId })
        }

        viewModel.carregarEstatisticas()

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(2, state!!.totalParticipantes)
    }
}
