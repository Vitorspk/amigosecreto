package activity.amigosecreto

import activity.amigosecreto.db.Grupo
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
class ConfiguracoesGrupoViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var viewModel: ConfiguracoesGrupoViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var grupoExistente: Grupo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        grupoDao = db.grupoDao()
        viewModel = ConfiguracoesGrupoViewModel(grupoDao).also {
            it.ioDispatcher = testDispatcher
        }
        val id = runBlocking { grupoDao.inserir(Grupo().apply { nome = "Original" }).toInt() }
        grupoExistente = runBlocking { grupoDao.buscarPorId(id)!! }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun salvar_grupoExistente_emiteSucesso() {
        grupoExistente.nome = "Atualizado"

        viewModel.salvar(grupoExistente)

        val resultado = viewModel.resultado.value
        assertTrue(resultado is SalvarResultado.Sucesso)
        assertEquals("Atualizado", (resultado as SalvarResultado.Sucesso).grupo.nome)
    }

    @Test
    fun salvar_grupoInexistente_emiteSemLinhasAfetadas() {
        val grupoFantasma = Grupo().apply { id = Int.MAX_VALUE; nome = "Fantasma" }

        viewModel.salvar(grupoFantasma)

        val resultado = viewModel.resultado.value
        assertTrue(resultado is SalvarResultado.SemLinhasAfetadas)
    }

    @Test
    fun resultadoConsumido_limpaState() {
        grupoExistente.nome = "Atualizado"
        viewModel.salvar(grupoExistente)

        viewModel.resultadoConsumido()

        assertNull(viewModel.resultado.value)
    }

    @Test
    fun salvar_persisteNoBanco() {
        grupoExistente.nome = "Novo Nome"
        grupoExistente.descricao = "Desc"

        viewModel.salvar(grupoExistente)

        val gravado = runBlocking { grupoDao.buscarPorId(grupoExistente.id) }
        assertNotNull(gravado)
        assertEquals("Novo Nome", gravado!!.nome)
        assertEquals("Desc", gravado.descricao)
    }
}
