package activity.amigosecreto

import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.repository.ParticipanteRepository
import android.database.sqlite.SQLiteException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.collections.Collection

/**
 * Testes unitários de ParticipantesViewModel via Robolectric + InstantTaskExecutorRule.
 *
 * O executor do ViewModel é substituído por um executor síncrono (Runnable::run) para que
 * o background work complete antes das asserções, sem precisar de sleeps ou polling.
 */
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
    private var grupoId = 0

    /** Executor síncrono: executa Runnable diretamente na thread chamadora. */
    private val syncExecutor: ExecutorService = object : ExecutorService {
        override fun execute(command: Runnable) = command.run()
        override fun shutdown() {}
        override fun shutdownNow(): List<Runnable> = emptyList()
        override fun isShutdown(): Boolean = false
        override fun isTerminated(): Boolean = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
        override fun <T> submit(task: Callable<T>): Future<T> =
            try { CompletableFuture.completedFuture(task.call()) }
            catch (e: Exception) { throw RuntimeException(e) }
        override fun <T> submit(task: Runnable, result: T): Future<T> {
            task.run(); return CompletableFuture.completedFuture(result)
        }
        override fun submit(task: Runnable): Future<*> {
            task.run(); return CompletableFuture.completedFuture(null)
        }
        override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> = emptyList()
        override fun <T> invokeAll(tasks: Collection<Callable<T>>, timeout: Long, unit: TimeUnit): List<Future<T>> = emptyList()
        // invokeAny não é chamado pelo código de produção; lança para tornar violação de contrato explícita.
        override fun <T> invokeAny(tasks: Collection<Callable<T>>): T = throw UnsupportedOperationException("invokeAny não suportado pelo syncExecutor")
        override fun <T> invokeAny(tasks: Collection<Callable<T>>, timeout: Long, unit: TimeUnit): T = throw UnsupportedOperationException("invokeAny não suportado pelo syncExecutor")
    }

    @Before
    fun setUp() {
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

        viewModel = ParticipantesViewModel(app, participanteRepository, desejoRepository)
        viewModel.setExecutorService(syncExecutor)
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        participanteDao.close()
        grupoDao.close()
    }

    // --- helpers ---

    /** Drena o main looper do Robolectric para que Handler.post() complete antes das asserções. */
    private fun idleMainLooper() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    /**
     * Configura o ViewModel com repoQueLanca, executa acao via um executor real
     * (necessário para que getApplication().getString() rode no main looper) e verifica que
     * errorMessage foi postado com um valor não-nulo e não-vazio.
     *
     * Restaura syncExecutor antes das asserções para evitar
     * RejectedExecutionException em tearDown.
     */
    private fun assertaErroComRealExecutor(repoQueLanca: ParticipanteRepository, acao: Runnable) {
        val realExecutor = Executors.newSingleThreadExecutor()
        viewModel.setExecutorService(realExecutor)
        viewModel.setRepositories(repoQueLanca, desejoRepository)
        viewModel.init(grupoId)

        idleMainLooper()
        viewModel.clearErrorMessage()
        idleMainLooper()

        acao.run()

        realExecutor.shutdown()
        assertTrue("Executor não terminou: background task travou",
            realExecutor.awaitTermination(3, TimeUnit.SECONDS))
        viewModel.setExecutorService(syncExecutor)
        idleMainLooper()

        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.errorMessage.value!!.isEmpty())
    }

    private fun inserirParticipante(nome: String): Participante {
        val p = Participante().apply {
            this.nome = nome
            telefone = "11999999999"
        }
        participanteDao.inserir(p, grupoId)
        return p
    }

    // =========================================================
    // carregarParticipantes / init
    // =========================================================

    @Test
    fun carregarParticipantes_populatesParticipantsLiveData() {
        inserirParticipante("Ana")
        inserirParticipante("Bruno")
        inserirParticipante("Carla")

        viewModel.init(grupoId)
        idleMainLooper()

        val lista = viewModel.participants.value
        assertNotNull(lista)
        assertEquals(3, lista!!.size)
    }

    @Test
    fun carregarParticipantes_populatesWishCountsForEachParticipant() {
        inserirParticipante("Diana")
        val apos = participanteDao.listarPorGrupo(grupoId)
        val pid = apos[0].id

        val desejoDAO = DesejoDAO(app)
        desejoDAO.open()
        desejoDAO.inserir(Desejo().apply { produto = "Livro"; participanteId = pid })
        desejoDAO.inserir(Desejo().apply { produto = "Caneta"; participanteId = pid })
        desejoDAO.close()

        viewModel.init(grupoId)
        idleMainLooper()

        val counts = viewModel.wishCounts.value
        assertNotNull(counts)
        assertEquals(2, counts!![pid])
    }

    @Test
    fun carregarParticipantes_setsIsLoadingFalseAfterCompletion() {
        viewModel.init(grupoId)
        idleMainLooper()
        assertNotEquals(true, viewModel.isLoading.value)
    }

    @Test
    fun init_calledTwiceWithSameId_doesNotDoubleLoad() {
        inserirParticipante("Eva")
        inserirParticipante("Felipe")
        inserirParticipante("Gabi")

        viewModel.init(grupoId)
        idleMainLooper()
        val firstSize = viewModel.participants.value?.size ?: 0

        // Segunda chamada com mesmo grupoId — guarda interna impede reload
        viewModel.init(grupoId)
        idleMainLooper()
        val secondSize = viewModel.participants.value?.size ?: 0

        assertEquals(firstSize, secondSize)
    }

    @Test
    fun carregarParticipantes_emptyGroup_returnsEmptyList() {
        viewModel.init(grupoId)
        idleMainLooper()

        val lista = viewModel.participants.value
        assertNotNull(lista)
        assertTrue(lista!!.isEmpty())
    }

    // =========================================================
    // realizarSorteio
    // =========================================================

    @Test
    fun realizarSorteio_withLessThan3Participants_emitsFailureNotEnough() {
        inserirParticipante("Hugo")
        inserirParticipante("Iris")
        viewModel.init(grupoId)
        idleMainLooper()

        viewModel.realizarSorteio()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_NOT_ENOUGH, resultado!!.status)
    }

    @Test
    fun realizarSorteio_withValidParticipants_emitsSuccess() {
        inserirParticipante("João")
        inserirParticipante("Karen")
        inserirParticipante("Lucas")
        viewModel.init(grupoId)
        idleMainLooper()

        viewModel.realizarSorteio()
        idleMainLooper()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.SUCCESS, resultado!!.status)
    }

    @Test
    fun realizarSorteio_successSavesToDatabase() {
        inserirParticipante("Maria")
        inserirParticipante("Nadia")
        inserirParticipante("Otto")
        viewModel.init(grupoId)
        idleMainLooper()

        viewModel.realizarSorteio()
        idleMainLooper()

        // Após sucesso, carregarParticipantes() é chamado internamente; verifica via LiveData
        val lista = viewModel.participants.value
        assertNotNull(lista)
        for (p in lista!!) {
            assertNotNull("amigoSorteadoId deve ser não-nulo após sorteio", p.amigoSorteadoId)
            assertTrue(p.amigoSorteadoId!! > 0)
        }
    }

    @Test
    fun realizarSorteio_withAllExclusionsBlocking_emitsFailureImpossible() {
        // 3 participantes onde cada um exclui os outros 2 → impossível
        inserirParticipante("Paulo")
        inserirParticipante("Quesia")
        inserirParticipante("Rita")
        viewModel.init(grupoId)
        idleMainLooper()

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
        idleMainLooper()

        viewModel.realizarSorteio()
        idleMainLooper()

        val resultado = viewModel.sorteioResult.value
        assertNotNull(resultado)
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_IMPOSSIBLE, resultado!!.status)
    }

    @Test
    fun realizarSorteio_setsIsLoadingFalseAfterCompletion() {
        inserirParticipante("Sara")
        inserirParticipante("Tiago")
        inserirParticipante("Uma")
        viewModel.init(grupoId)
        idleMainLooper()

        viewModel.realizarSorteio()
        idleMainLooper()

        assertNotEquals(true, viewModel.isLoading.value)
    }

    // =========================================================
    // clearSorteioResult / clearErrorMessage
    // =========================================================

    @Test
    fun clearSorteioResult_setsValueToNull() {
        // Forçar um resultado FAILURE_NOT_ENOUGH com 0 participantes
        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.realizarSorteio()
        assertNotNull(viewModel.sorteioResult.value)

        viewModel.clearSorteioResult()

        assertNull(viewModel.sorteioResult.value)
    }

    @Test
    fun clearErrorMessage_setsValueToNull() {
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
    fun atualizarParticipante_sucesso_emiteTrue() {
        inserirParticipante("Vera")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val p = lista[0].apply { nome = "Vera Atualizada" }

        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.atualizarParticipante(p)
        idleMainLooper()

        assertEquals(true, viewModel.atualizarSucesso.value)
    }

    @Test
    fun atualizarParticipante_idInvalido_emiteFalse() {
        val p = Participante().apply { id = 0; nome = "Fantasma" }

        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.atualizarParticipante(p)
        idleMainLooper()

        assertEquals(false, viewModel.atualizarSucesso.value)
    }

    // =========================================================
    // marcarComoEnviado
    // =========================================================

    @Test
    fun marcarComoEnviado_setaFlagNoParticipante() {
        inserirParticipante("Xavier")
        val antes = participanteDao.listarPorGrupo(grupoId)
        val id = antes[0].id

        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.marcarComoEnviado(id)
        idleMainLooper()

        val apos = participanteDao.listarPorGrupo(grupoId)
        assertTrue(apos[0].isEnviado)
    }

    // =========================================================
    // salvarExclusoes
    // =========================================================

    @Test
    fun salvarExclusoes_adicionaERemoveCorretamente() {
        inserirParticipante("Yara")
        inserirParticipante("Zico")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val id1 = lista[0].id
        val id2 = lista[1].id

        viewModel.init(grupoId)
        idleMainLooper()

        // Adicionar exclusão
        viewModel.salvarExclusoes(id1, listOf(id2), emptyList())
        idleMainLooper()

        val aposAdicionar = participanteDao.listarPorGrupo(grupoId)
        val p1 = aposAdicionar.firstOrNull { it.id == id1 }
        assertNotNull(p1)
        assertTrue(p1!!.idsExcluidos.contains(id2))

        // Remover exclusão
        viewModel.salvarExclusoes(id1, emptyList(), listOf(id2))
        idleMainLooper()

        val aposRemover = participanteDao.listarPorGrupo(grupoId)
        val p1apos = aposRemover.firstOrNull { it.id == id1 }
        assertNotNull(p1apos)
        assertFalse(p1apos!!.idsExcluidos.contains(id2))
    }

    // =========================================================
    // inserirParticipante — caminho de erro
    // =========================================================

    @Test
    fun inserirParticipante_erroNoRepository_postaErrorMessage() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun inserir(participante: Participante, grupoId: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        val p = Participante().apply { nome = "Erro" }
        assertaErroComRealExecutor(repoQueLanca) { viewModel.inserirParticipante(p, grupoId) }
    }

    // =========================================================
    // participants LiveData updates after mutations
    // =========================================================

    @Test
    fun removerParticipante_atualizaParticipantsLiveData() {
        inserirParticipante("Ana")
        inserirParticipante("Bruno")
        inserirParticipante("Carla")
        viewModel.init(grupoId)
        idleMainLooper()

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertEquals(3, antes!!.size)
        val idRemover = antes[0].id

        viewModel.removerParticipante(idRemover)
        idleMainLooper()

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertEquals(2, apos!!.size)
        assertFalse(apos.any { it.id == idRemover })
    }

    @Test
    fun inserirParticipante_atualizaParticipantsLiveData() {
        inserirParticipante("Diana")
        inserirParticipante("Eduardo")
        viewModel.init(grupoId)
        idleMainLooper()

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertEquals(2, antes!!.size)

        val novo = Participante().apply { nome = "Fernanda"; telefone = "11988887777" }
        viewModel.inserirParticipante(novo, grupoId)
        idleMainLooper()

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertEquals(3, apos!!.size)
        assertTrue(apos.any { it.nome == "Fernanda" })
    }

    @Test
    fun deletarTodosDoGrupo_atualizaParticipantsLiveData() {
        inserirParticipante("Gabi")
        inserirParticipante("Hugo")
        viewModel.init(grupoId)
        idleMainLooper()

        val antes = viewModel.participants.value
        assertNotNull(antes)
        assertFalse(antes!!.isEmpty())

        viewModel.deletarTodosDoGrupo(grupoId)
        idleMainLooper()

        val apos = viewModel.participants.value
        assertNotNull(apos)
        assertTrue(apos!!.isEmpty())
    }

    @Test
    fun salvarExclusoes_atualizaParticipantsLiveData() {
        inserirParticipante("Ines")
        inserirParticipante("Jorge")
        val lista = participanteDao.listarPorGrupo(grupoId)
        val id1 = lista[0].id
        val id2 = lista[1].id

        viewModel.init(grupoId)
        idleMainLooper()

        viewModel.salvarExclusoes(id1, listOf(id2), emptyList())
        idleMainLooper()

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
    fun prepararMensagensSms_participanteComTelefone_emiteResultadoComMensagem() {
        inserirParticipante("Alice")
        inserirParticipante("Bob")
        inserirParticipante("Carol")
        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.realizarSorteio()
        idleMainLooper()

        val lista = viewModel.participants.value
        assertNotNull(lista)

        viewModel.prepararMensagensSms(lista!!)
        idleMainLooper()

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
    fun prepararMensagensSms_nenhumComTelefone_emiteListaVazia() {
        // Participante sem telefone
        participanteDao.inserir(Participante().apply { nome = "SemFone" }, grupoId)

        viewModel.init(grupoId)
        idleMainLooper()

        val lista = viewModel.participants.value
        assertNotNull(lista)

        viewModel.prepararMensagensSms(lista!!)
        idleMainLooper()

        val resultado = viewModel.mensagensSmsResult.value
        assertNotNull(resultado)
        assertTrue(resultado!!.participantesComTelefone.isEmpty())
    }

    // =========================================================
    // prepararMensagemCompartilhamento
    // =========================================================

    @Test
    fun prepararMensagemCompartilhamento_emiteMensagemEMarcaEnviado() {
        inserirParticipante("David")
        inserirParticipante("Elena")
        inserirParticipante("Fred")
        viewModel.init(grupoId)
        idleMainLooper()
        viewModel.realizarSorteio()
        idleMainLooper()

        val lista = viewModel.participants.value
        assertNotNull(lista)
        val primeiro = lista!![0]

        viewModel.prepararMensagemCompartilhamento(primeiro)
        idleMainLooper()

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
    fun marcarComoEnviado_erroNoRepository_postaErrorMessage() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun marcarComoEnviado(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErroComRealExecutor(repoQueLanca) { viewModel.marcarComoEnviado(1) }
    }

    // =========================================================
    // atualizarParticipante — caminho de erro
    // =========================================================

    @Test
    fun atualizarParticipante_excecaoNoRepository_emiteFalse() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun atualizar(participante: Participante): Boolean {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }

        viewModel.setRepositories(repoQueLanca, desejoRepository)
        viewModel.init(grupoId)
        idleMainLooper()

        val p = Participante().apply { id = 1; nome = "Teste" }
        viewModel.atualizarParticipante(p)
        idleMainLooper()

        assertEquals(false, viewModel.atualizarSucesso.value)
    }

    // =========================================================
    // removerParticipante — caminho de erro
    // =========================================================

    @Test
    fun removerParticipante_erroNoRepository_postaErrorMessage() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun remover(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErroComRealExecutor(repoQueLanca) { viewModel.removerParticipante(1) }
    }

    // =========================================================
    // deletarTodosDoGrupo — caminho de erro
    // =========================================================

    @Test
    fun deletarTodosDoGrupo_erroNoRepository_postaErrorMessage() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun deletarTodosDoGrupo(grupoId: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErroComRealExecutor(repoQueLanca) { viewModel.deletarTodosDoGrupo(grupoId) }
    }

    // =========================================================
    // salvarExclusoes — caminho de erro
    // =========================================================

    @Test
    fun salvarExclusoes_erroNoRepository_postaErrorMessage() {
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        assertaErroComRealExecutor(repoQueLanca) {
            viewModel.salvarExclusoes(1, listOf(2), emptyList())
        }
    }

    // =========================================================
    // prepararMensagensSms — caminho de erro
    // =========================================================

    @Test
    fun prepararMensagensSms_erroNoRepository_postaErrorMessage() {
        // Forçar exceção no listarPorGrupo que prepararMensagensSms chama internamente
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun listarPorGrupo(grupoId: Int): List<Participante> {
                throw SQLiteException("falha simulada")
            }
        }
        assertaErroComRealExecutor(repoQueLanca) {
            viewModel.prepararMensagensSms(emptyList())
        }
    }

    // =========================================================
    // prepararMensagemCompartilhamento — caminho de erro
    // =========================================================

    @Test
    fun prepararMensagemCompartilhamento_erroNoRepository_postaErrorMessage() {
        // Forçar exceção em marcarComoEnviado, que é sempre chamado
        val repoQueLanca = object : ParticipanteRepository(app) {
            override fun marcarComoEnviado(id: Int) {
                throw SQLiteException("falha simulada")
            }
            override fun listarPorGrupo(grupoId: Int): List<Participante> = emptyList()
        }
        val p = Participante().apply { id = 1; nome = "Teste" }
        // amigoSorteadoId = null → getNomeAmigoSorteado não é chamado; marcarComoEnviado sempre é
        assertaErroComRealExecutor(repoQueLanca) {
            viewModel.prepararMensagemCompartilhamento(p)
        }
    }
}
