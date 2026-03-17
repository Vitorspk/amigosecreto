package activity.amigosecreto

import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.repository.SorteioRepository
import android.database.sqlite.SQLiteException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Testes unitários de ParticipantesViewModel via Robolectric + InstantTaskExecutorRule.
 *
 * O ioDispatcher do ViewModel é substituído por UnconfinedTestDispatcher para que
 * o background work complete antes das asserções, sem precisar de sleeps ou polling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParticipantesViewModelTest {

    /** Faz LiveData.setValue() disparar de forma síncrona no thread de teste. */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var app: android.app.Application
    private lateinit var viewModel: ParticipantesViewModel
    private lateinit var grupoDao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var participanteRepository: ParticipanteRepository
    private lateinit var desejoRepository: DesejoRepository
    private lateinit var sorteioRepository: SorteioRepository
    private var grupoId = 0

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        grupoDao = GrupoDAO(app)
        grupoDao.open()
        grupoId = grupoDao.inserir(Grupo().apply {
            nome = "Grupo Teste"
            data = "01/01/2025"
        }).toInt()

        participanteDao = ParticipanteDAO(app)
        participanteDao.open()

        desejoRepository = DesejoRepository(app)
        participanteRepository = ParticipanteRepository(app)
        sorteioRepository = SorteioRepository(app)

        viewModel = ParticipantesViewModel(app, participanteRepository, desejoRepository, sorteioRepository)
        viewModel.ioDispatcher = testDispatcher
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        grupoDao.limparTudo()
        participanteDao.close()
        grupoDao.close()
    }

    // --- helpers ---

    private fun inserirParticipante(nome: String): Participante {
        val p = Participante().apply {
            this.nome = nome
            telefone = "11999999999"
        }
        participanteDao.inserir(p, grupoId)
        return p
    }

    /**
     * Configura o ViewModel com repoQueLanca, executa acao e verifica que
     * errorMessage foi postado com valor não-nulo e não-vazio.
     * Com UnconfinedTestDispatcher, a coroutine roda de forma síncrona — sem executor real.
     */
    private fun assertaErro(repoQueLanca: ParticipanteRepository, acao: () -> Unit) {
        viewModel.setRepositories(repoQueLanca, desejoRepository)
        viewModel.ioDispatcher = testDispatcher
        viewModel.init(grupoId)
        viewModel.clearErrorMessage()

        acao()

        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.errorMessage.value!!.isEmpty())
    }

    // =========================================================
    // carregarParticipantes / init
    // =========================================================

    @Test
    fun carregarParticipantes_populatesParticipantsLiveData() = runTest(testDispatcher) {
        inserirParticipante("Ana")
        inserirParticipante("Bruno")
        inserirParticipante("Carla")

        viewModel.init(grupoId)

        val lista = viewModel.participants.value
        assertNotNull(lista)
        assertEquals(3, lista!!.size)
    }

    @Test
    fun carregarParticipantes_populatesWishCountsForEachParticipant() = runTest(testDispatcher) {
        inserirParticipante("Diana")
        val apos = participanteDao.listarPorGrupo(grupoId)
        val pid = apos[0].id

        val desejoDAO = DesejoDAO(app)
        desejoDAO.open()
        desejoDAO.inserir(Desejo().apply { produto = "Livro"; participanteId = pid })
        desejoDAO.inserir(Desejo().apply { produto = "Caneta"; participanteId = pid })
        desejoDAO.close()

        viewModel.init(grupoId)

        val counts = viewModel.wishCounts.value
        assertNotNull(counts)
        assertEquals(2, counts!![pid])
    }

    @Test
    fun carregarParticipantes_setsIsLoadingFalseAfterCompletion() = runTest(testDispatcher) {
        viewModel.init(grupoId)
        assertNotEquals(true, viewModel.isLoading.value)
    }

    @Test
    fun init_calledTwiceWithSameId_doesNotDoubleLoad() = runTest(testDispatcher) {
        inserirParticipante("Eva")
        inserirParticipante("Felipe")
        inserirParticipante("Gabi")

        viewModel.init(grupoId)
        val firstSize = viewModel.participants.value?.size ?: 0

        // Segunda chamada com mesmo grupoId — guarda interna impede reload
        viewModel.init(grupoId)
        val secondSize = viewModel.participants.value?.size ?: 0

        assertEquals(firstSize, secondSize)
    }

    @Test
    fun carregarParticipantes_emptyGroup_returnsEmptyList() = runTest(testDispatcher) {
        viewModel.init(grupoId)

        val lista = viewModel.participants.value
        assertNotNull(lista)
        assertTrue(lista!!.isEmpty())
    }

    // =========================================================
    // realizarSorteio
    // =========================================================

    @Test
    fun realizarSorteio_withLessThan3Participants_emitsFailureNotEnough() = runTest(testDispatcher) {
        inserirParticipante("Hugo")
        inserirParticipante("Iris")
        viewModel.init(grupoId)

        viewModel.realizarSorteio()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_NOT_ENOUGH, resultado!!.status)
    }

    @Test
    fun realizarSorteio_withValidParticipants_emitsSuccess() = runTest(testDispatcher) {
        inserirParticipante("João")
        inserirParticipante("Karen")
        inserirParticipante("Lucas")
        viewModel.init(grupoId)

        viewModel.realizarSorteio()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.SUCCESS, resultado!!.status)
    }

    @Test
    fun realizarSorteio_successSavesToDatabase() = runTest(testDispatcher) {
        inserirParticipante("Maria")
        inserirParticipante("Nadia")
        inserirParticipante("Otto")
        viewModel.init(grupoId)

        viewModel.realizarSorteio()

        // Após sucesso, carregarParticipantes() é chamado internamente; verifica via LiveData
        val lista = viewModel.participants.value
        assertNotNull(lista)
        for (p in lista!!) {
            assertNotNull("amigoSorteadoId deve ser não-nulo após sorteio", p.amigoSorteadoId)
            assertTrue(p.amigoSorteadoId!! > 0)
        }
    }

    @Test
    fun realizarSorteio_withAllExclusionsBlocking_emitsFailureImpossible() = runTest(testDispatcher) {
        // 3 participantes onde cada um exclui os outros 2 → impossível
        inserirParticipante("Paulo")
        inserirParticipante("Quesia")
        inserirParticipante("Rita")
        viewModel.init(grupoId)

        val lista = viewModel.participants.value
        assertNotNull(lista)
        assertEquals(3, lista!!.size)

        val id0 = lista[0].id
        val id1 = lista[1].id
        val id2 = lista[2].id

        participanteDao.adicionarExclusao(id0, id1)
        participanteDao.adicionarExclusao(id0, id2)
        participanteDao.adicionarExclusao(id1, id0)
        participanteDao.adicionarExclusao(id1, id2)
        participanteDao.adicionarExclusao(id2, id0)
        participanteDao.adicionarExclusao(id2, id1)

        // Recarrega lista com exclusões
        viewModel.carregarParticipantes()

        viewModel.realizarSorteio()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_IMPOSSIBLE, resultado!!.status)
    }

    @Test
    fun realizarSorteio_setsIsLoadingFalseAfterCompletion() = runTest(testDispatcher) {
        inserirParticipante("Sara")
        inserirParticipante("Tiago")
        inserirParticipante("Uma")
        viewModel.init(grupoId)

        viewModel.realizarSorteio()

        assertNotEquals(true, viewModel.isLoading.value)
    }

    // =========================================================
    // clearSorteioResult / clearErrorMessage
    // =========================================================

    @Test
    fun clearSorteioResult_setsValueToNull() = runTest(testDispatcher) {
        // Forçar um resultado FAILURE_NOT_ENOUGH com 0 participantes
        viewModel.init(grupoId)
        viewModel.realizarSorteio()
        assertNotNull(viewModel.sorteioResult.value)

        viewModel.clearSorteioResult()

        assertNull(viewModel.sorteioResult.value)
    }

    @Test
    fun clearErrorMessage_setsValueToNull() = runTest(testDispatcher) {
        viewModel.realizarSorteio()
        viewModel.clearSorteioResult()
        assertNull(viewModel.sorteioResult.value)

        // errorMessage começa null — clearErrorMessage deve mantê-lo null (não lançar exceção)
        viewModel.clearErrorMessage()
        assertNull(viewModel.errorMessage.value)
    }

    // =========================================================
    // atualizarParticipante
    // =========================================================

    @Test
    fun atualizarParticipante_sucesso_emiteTrue() = runTest(testDispatcher) {
        inserirParticipante("Vera")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val p = lista[0].apply { nome = "Vera Atualizada" }

        viewModel.init(grupoId)
        viewModel.atualizarParticipante(p)

        assertEquals(true, viewModel.atualizarSucesso.value)
    }

    @Test
    fun atualizarParticipante_idInvalido_emiteFalse() = runTest(testDispatcher) {
        val p = Participante().apply { id = 0; nome = "Fantasma" }

        viewModel.init(grupoId)
        viewModel.atualizarParticipante(p)

        assertEquals(false, viewModel.atualizarSucesso.value)
    }

    // =========================================================
    // marcarComoEnviado
    // =========================================================

    @Test
    fun marcarComoEnviado_setaFlagNoParticipante() = runTest(testDispatcher) {
        inserirParticipante("Xavier")
        val antes = participanteDao.listarPorGrupo(grupoId)
        val id = antes[0].id

        viewModel.init(grupoId)
        viewModel.marcarComoEnviado(id)

        val apos = participanteDao.listarPorGrupo(grupoId)
        assertTrue(apos[0].isEnviado)
    }

    // =========================================================
    // salvarExclusoes
    // =========================================================

    @Test
    fun salvarExclusoes_adicionaERemoveCorretamente() = runTest(testDispatcher) {
        inserirParticipante("Yara")
        inserirParticipante("Zico")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val id1 = lista[0].id
        val id2 = lista[1].id

        viewModel.init(grupoId)

        // Adicionar exclusão
        viewModel.salvarExclusoes(id1, listOf(id2), emptyList())

        val aposAdicionar = participanteDao.listarPorGrupo(grupoId)
        val p1 = aposAdicionar.firstOrNull { it.id == id1 }
        assertNotNull(p1)
        assertTrue(p1!!.idsExcluidos.contains(id2))

        // Remover exclusão
        viewModel.salvarExclusoes(id1, emptyList(), listOf(id2))

        val aposRemover = participanteDao.listarPorGrupo(grupoId)
        val p1apos = aposRemover.firstOrNull { it.id == id1 }
        assertNotNull(p1apos)
        assertFalse(p1apos!!.idsExcluidos.contains(id2))
    }

    // =========================================================
    // inserirParticipante — caminho de erro
    // =========================================================

    @Test
    fun inserirParticipante_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun inserir(participante: Participante, grupoId: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        val p = Participante().apply { nome = "Erro" }
        assertaErro(repoQueLanca) { viewModel.inserirParticipante(p, grupoId) }
    }

    // =========================================================
    // participants LiveData updates after mutations
    // =========================================================

    @Test
    fun removerParticipante_atualizaParticipantsLiveData() = runTest(testDispatcher) {
        inserirParticipante("Ana")
        inserirParticipante("Bruno")
        inserirParticipante("Carla")
        viewModel.init(grupoId)

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertEquals(3, antes!!.size)
        val idRemover = antes[0].id

        viewModel.removerParticipante(idRemover)

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertEquals(2, apos!!.size)
        assertFalse(apos.any { it.id == idRemover })
    }

    @Test
    fun inserirParticipante_atualizaParticipantsLiveData() = runTest(testDispatcher) {
        inserirParticipante("Diana")
        inserirParticipante("Eduardo")
        viewModel.init(grupoId)

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertEquals(2, antes!!.size)

        val novo = Participante().apply { nome = "Fernanda"; telefone = "11988887777" }
        viewModel.inserirParticipante(novo, grupoId)

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertEquals(3, apos!!.size)
        assertTrue(apos.any { it.nome == "Fernanda" })
    }

    @Test
    fun deletarTodosDoGrupo_atualizaParticipantsLiveData() = runTest(testDispatcher) {
        inserirParticipante("Gabi")
        inserirParticipante("Hugo")
        viewModel.init(grupoId)

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertFalse(antes!!.isEmpty())

        viewModel.deletarTodosDoGrupo(grupoId)

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertTrue(apos!!.isEmpty())
    }

    @Test
    fun salvarExclusoes_atualizaParticipantsLiveData() = runTest(testDispatcher) {
        inserirParticipante("Ines")
        inserirParticipante("Jorge")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val id1 = lista[0].id
        val id2 = lista[1].id

        viewModel.init(grupoId)

        viewModel.salvarExclusoes(id1, listOf(id2), emptyList())

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertEquals(2, apos!!.size)
        val p1 = apos.firstOrNull { it.id == id1 }
        assertNotNull(p1)
        assertTrue(p1!!.idsExcluidos.contains(id2))
    }

    // =========================================================
    // prepararMensagensSms
    // =========================================================

    @Test
    fun prepararMensagensSms_participanteComTelefone_emiteResultadoComMensagem() = runTest(testDispatcher) {
        inserirParticipante("Alice")
        inserirParticipante("Bob")
        inserirParticipante("Carol")
        viewModel.init(grupoId)
        viewModel.realizarSorteio()

        val lista = viewModel.participants.value
        assertNotNull(lista)

        viewModel.prepararMensagensSms(lista!!)

        val resultado = viewModel.mensagensSmsResult.value
        assertNotNull(resultado)
        // Todos têm telefone cadastrado
        assertEquals(lista.size, resultado!!.participantesComTelefone.size)
        for (p in resultado.participantesComTelefone) {
            assertNotNull(resultado.mensagens[p.id])
            assertFalse(resultado.mensagens[p.id]!!.isEmpty())
        }
    }

    @Test
    fun prepararMensagensSms_nenhumComTelefone_emiteListaVazia() = runTest(testDispatcher) {
        // Participante sem telefone
        participanteDao.inserir(Participante().apply { nome = "SemFone" }, grupoId)

        viewModel.init(grupoId)

        val lista = viewModel.participants.value
        assertNotNull(lista)

        viewModel.prepararMensagensSms(lista!!)

        val resultado = viewModel.mensagensSmsResult.value
        assertNotNull(resultado)
        assertTrue(resultado!!.participantesComTelefone.isEmpty())
    }

    // =========================================================
    // prepararMensagemCompartilhamento
    // =========================================================

    @Test
    fun prepararMensagemCompartilhamento_emiteMensagemEMarcaEnviado() = runTest(testDispatcher) {
        inserirParticipante("David")
        inserirParticipante("Elena")
        inserirParticipante("Fred")
        viewModel.init(grupoId)
        viewModel.realizarSorteio()

        val lista = viewModel.participants.value
        assertNotNull(lista)
        val primeiro = lista!![0]

        viewModel.prepararMensagemCompartilhamento(primeiro)

        val resultado = viewModel.mensagemCompartilhamentoResult.value
        assertNotNull(resultado)
        assertEquals(primeiro.id, resultado!!.participante.id)
        assertNotNull(resultado.mensagem)
        assertFalse(resultado.mensagem.isEmpty())

        // Deve ter marcado como enviado
        val apos = participanteDao.listarPorGrupo(grupoId)
        val primeirosApos = apos.firstOrNull { it.id == primeiro.id }
        assertNotNull(primeirosApos)
        assertTrue(primeirosApos!!.isEnviado)
    }

    // =========================================================
    // marcarComoEnviado — caminho de erro
    // =========================================================

    @Test
    fun marcarComoEnviado_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun marcarComoEnviado(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErro(repoQueLanca) { viewModel.marcarComoEnviado(1) }
    }

    // =========================================================
    // atualizarParticipante — caminho de erro
    // =========================================================

    @Test
    fun atualizarParticipante_excecaoNoRepository_emiteFalse() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun atualizar(participante: Participante): Boolean {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }

        viewModel.setRepositories(repoQueLanca, desejoRepository)
        viewModel.ioDispatcher = testDispatcher
        viewModel.init(grupoId)

        val p = Participante().apply { id = 1; nome = "Teste" }
        viewModel.atualizarParticipante(p)

        assertEquals(false, viewModel.atualizarSucesso.value)
    }

    // =========================================================
    // removerParticipante — caminho de erro
    // =========================================================

    @Test
    fun removerParticipante_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun remover(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErro(repoQueLanca) { viewModel.removerParticipante(1) }
    }

    // =========================================================
    // deletarTodosDoGrupo — caminho de erro
    // =========================================================

    @Test
    fun deletarTodosDoGrupo_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun deletarTodosDoGrupo(grupoId: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErro(repoQueLanca) { viewModel.deletarTodosDoGrupo(grupoId) }
    }

    // =========================================================
    // salvarExclusoes — caminho de erro
    // =========================================================

    @Test
    fun salvarExclusoes_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErro(repoQueLanca) {
            viewModel.salvarExclusoes(1, listOf(2), emptyList())
        }
    }

    // =========================================================
    // prepararMensagensSms — caminho de erro
    // =========================================================

    @Test
    fun prepararMensagensSms_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        // Forçar exceção no listarPorGrupo que prepararMensagensSms chama internamente
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun listarPorGrupo(grupoId: Int): List<Participante> {
                throw SQLiteException("falha simulada")
            }
        }
        assertaErro(repoQueLanca) {
            viewModel.prepararMensagensSms(emptyList())
        }
    }

    // =========================================================
    // prepararMensagemCompartilhamento — caminho de erro
    // =========================================================

    @Test
    fun prepararMensagemCompartilhamento_erroNoRepository_postaErrorMessage() = runTest(testDispatcher) {
        // Forçar exceção em marcarComoEnviado, que é sempre chamado
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun marcarComoEnviado(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        val p = Participante().apply { id = 1; nome = "Teste" }
        // amigoSorteadoId = null → getNomeAmigoSorteado não é chamado; marcarComoEnviado sempre é
        assertaErro(repoQueLanca) {
            viewModel.prepararMensagemCompartilhamento(p)
        }
    }
}
