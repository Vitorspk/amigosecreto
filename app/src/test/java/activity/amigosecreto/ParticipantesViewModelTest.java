package activity.amigosecreto;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;
import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;
import activity.amigosecreto.repository.DesejoRepository;
import activity.amigosecreto.repository.ParticipanteRepository;

import static org.junit.Assert.*;

/**
 * Testes unitários de ParticipantesViewModel via Robolectric + InstantTaskExecutorRule.
 *
 * O executor do ViewModel é substituído por um executor síncrono (Runnable::run) para que
 * o background work complete antes das asserções, sem precisar de sleeps ou polling.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ParticipantesViewModelTest {

    /** Faz LiveData.setValue() disparar de forma síncrona no thread de teste. */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private android.app.Application app;
    private ParticipantesViewModel viewModel;
    private GrupoDAO grupoDao;
    private ParticipanteDAO participanteDao;
    private DesejoRepository desejoRepository;
    private int grupoId;

    /** Executor síncrono: executa Runnable diretamente na thread chamadora. */
    private final ExecutorService syncExecutor = new ExecutorService() {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() {}
        @Override public List<Runnable> shutdownNow() { return java.util.Collections.emptyList(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
        @Override public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { try { return java.util.concurrent.CompletableFuture.completedFuture(task.call()); } catch (Exception e) { throw new RuntimeException(e); } }
        @Override public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) { task.run(); return java.util.concurrent.CompletableFuture.completedFuture(result); }
        @Override public java.util.concurrent.Future<?> submit(Runnable task) { task.run(); return java.util.concurrent.CompletableFuture.completedFuture(null); }
        @Override public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { return java.util.Collections.emptyList(); }
        @Override public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) { return java.util.Collections.emptyList(); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { return null; }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) { return null; }
    };

    @Before
    public void setUp() {
        app = (android.app.Application) ApplicationProvider.getApplicationContext();

        grupoDao = new GrupoDAO(app);
        grupoDao.open();
        Grupo g = new Grupo();
        g.setNome("Grupo Teste");
        g.setData("01/01/2025");
        grupoId = (int) grupoDao.inserir(g);

        participanteDao = new ParticipanteDAO(app);
        participanteDao.open();

        desejoRepository = new DesejoRepository(app);

        viewModel = new ParticipantesViewModel(app);
        viewModel.setExecutorService(syncExecutor);
    }

    @After
    public void tearDown() {
        grupoDao.limparTudo();
        participanteDao.close();
        grupoDao.close();
    }

    // --- helpers ---

    /** Drena o main looper do Robolectric para que Handler.post() complete antes das asserções. */
    private void idleMainLooper() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    /**
     * Configura o ViewModel com {@code repoQueLanca}, executa {@code acao} via um executor real
     * (necessário para que getApplication().getString() rode no main looper) e verifica que
     * {@code errorMessage} foi postado com um valor não-nulo e não-vazio.
     *
     * <p>Restaura {@code syncExecutor} antes das asserções para evitar
     * {@link java.util.concurrent.RejectedExecutionException} em tearDown.
     */
    private void assertaErroComRealExecutor(ParticipanteRepository repoQueLanca, Runnable acao)
            throws InterruptedException {
        ExecutorService realExecutor = Executors.newSingleThreadExecutor();
        viewModel.setExecutorService(realExecutor);
        viewModel.setRepositories(repoQueLanca, desejoRepository);
        viewModel.init(grupoId);

        idleMainLooper();
        viewModel.clearErrorMessage();
        idleMainLooper();

        acao.run();

        realExecutor.shutdown();
        assertTrue("Executor não terminou: background task travou",
                realExecutor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS));
        viewModel.setExecutorService(syncExecutor);
        idleMainLooper();

        assertNotNull(viewModel.getErrorMessage().getValue());
        assertFalse(viewModel.getErrorMessage().getValue().isEmpty());
    }

    private Participante inserirParticipante(String nome) {
        Participante p = new Participante();
        p.setNome(nome);
        p.setTelefone("11999999999");
        participanteDao.inserir(p, grupoId);
        return p;
    }

    // =========================================================
    // carregarParticipantes / init
    // =========================================================

    @Test
    public void carregarParticipantes_populatesParticipantsLiveData() {
        inserirParticipante("Ana");
        inserirParticipante("Bruno");
        inserirParticipante("Carla");

        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);
        assertEquals(3, lista.size());
    }

    @Test
    public void carregarParticipantes_populatesWishCountsForEachParticipant() {
        inserirParticipante("Diana");
        // Recarregar com ID real após inserção
        List<Participante> apos = participanteDao.listarPorGrupo(grupoId);
        int pid = apos.get(0).getId();

        DesejoDAO desejoDAO = new DesejoDAO(app);
        desejoDAO.open();
        Desejo d1 = new Desejo(); d1.setProduto("Livro"); d1.setParticipanteId(pid); desejoDAO.inserir(d1);
        Desejo d2 = new Desejo(); d2.setProduto("Caneta"); d2.setParticipanteId(pid); desejoDAO.inserir(d2);
        desejoDAO.close();

        viewModel.init(grupoId);
        idleMainLooper();

        Map<Integer, Integer> counts = viewModel.getWishCounts().getValue();
        assertNotNull(counts);
        assertEquals(Integer.valueOf(2), counts.get(pid));
    }

    @Test
    public void carregarParticipantes_setsIsLoadingFalseAfterCompletion() {
        viewModel.init(grupoId);
        idleMainLooper();
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void init_calledTwiceWithSameId_doesNotDoubleLoad() {
        inserirParticipante("Eva");
        inserirParticipante("Felipe");
        inserirParticipante("Gabi");

        viewModel.init(grupoId);
        idleMainLooper();
        List<Participante> firstLoad = viewModel.getParticipants().getValue();
        int firstSize = firstLoad != null ? firstLoad.size() : 0;

        // Segunda chamada com mesmo grupoId — guarda interna impede reload
        viewModel.init(grupoId);
        idleMainLooper();
        List<Participante> secondLoad = viewModel.getParticipants().getValue();
        int secondSize = secondLoad != null ? secondLoad.size() : 0;

        // O tamanho deve ser o mesmo (nenhum item extra foi adicionado entre as duas chamadas)
        assertEquals(firstSize, secondSize);
    }

    @Test
    public void carregarParticipantes_emptyGroup_returnsEmptyList() {
        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }

    // =========================================================
    // realizarSorteio
    // =========================================================

    @Test
    public void realizarSorteio_withLessThan3Participants_emitsFailureNotEnough() {
        inserirParticipante("Hugo");
        inserirParticipante("Iris");
        viewModel.init(grupoId);
        idleMainLooper();

        viewModel.realizarSorteio();

        ParticipantesViewModel.SorteioResultado resultado = viewModel.getSorteioResult().getValue();
        assertNotNull(resultado);
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_NOT_ENOUGH, resultado.status);
    }

    @Test
    public void realizarSorteio_withValidParticipants_emitsSuccess() {
        inserirParticipante("João");
        inserirParticipante("Karen");
        inserirParticipante("Lucas");
        viewModel.init(grupoId);
        idleMainLooper();

        viewModel.realizarSorteio();
        idleMainLooper();

        ParticipantesViewModel.SorteioResultado resultado = viewModel.getSorteioResult().getValue();
        assertNotNull(resultado);
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.SUCCESS, resultado.status);
    }

    @Test
    public void realizarSorteio_successSavesToDatabase() {
        inserirParticipante("Maria");
        inserirParticipante("Nadia");
        inserirParticipante("Otto");
        viewModel.init(grupoId);
        idleMainLooper();

        viewModel.realizarSorteio();
        idleMainLooper();

        // Após sucesso, carregarParticipantes() é chamado internamente; verifica via LiveData
        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);
        for (Participante p : lista) {
            assertNotNull("amigoSorteadoId deve ser não-nulo após sorteio", p.getAmigoSorteadoId());
            assertTrue(p.getAmigoSorteadoId() > 0);
        }
    }

    @Test
    public void realizarSorteio_withAllExclusionsBlocking_emitsFailureImpossible() {
        // 3 participantes onde cada um exclui os outros 2 → impossível
        inserirParticipante("Paulo");
        inserirParticipante("Quesia");
        inserirParticipante("Rita");
        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);
        assertEquals(3, lista.size());

        int id0 = lista.get(0).getId();
        int id1 = lista.get(1).getId();
        int id2 = lista.get(2).getId();

        participanteDao.adicionarExclusao(id0, id1);
        participanteDao.adicionarExclusao(id0, id2);
        participanteDao.adicionarExclusao(id1, id0);
        participanteDao.adicionarExclusao(id1, id2);
        participanteDao.adicionarExclusao(id2, id0);
        participanteDao.adicionarExclusao(id2, id1);

        // Recarrega lista com exclusões
        viewModel.carregarParticipantes();
        idleMainLooper();

        viewModel.realizarSorteio();
        idleMainLooper();

        ParticipantesViewModel.SorteioResultado resultado = viewModel.getSorteioResult().getValue();
        assertNotNull(resultado);
        assertEquals(ParticipantesViewModel.SorteioResultado.Status.FAILURE_IMPOSSIBLE, resultado.status);
    }

    @Test
    public void realizarSorteio_setsIsLoadingFalseAfterCompletion() {
        inserirParticipante("Sara");
        inserirParticipante("Tiago");
        inserirParticipante("Uma");
        viewModel.init(grupoId);
        idleMainLooper();

        viewModel.realizarSorteio();
        idleMainLooper();

        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    // =========================================================
    // clearSorteioResult / clearErrorMessage
    // =========================================================

    @Test
    public void clearSorteioResult_setsValueToNull() {
        // Forçar um resultado FAILURE_NOT_ENOUGH com 0 participantes
        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.realizarSorteio();
        assertNotNull(viewModel.getSorteioResult().getValue());

        viewModel.clearSorteioResult();

        assertNull(viewModel.getSorteioResult().getValue());
    }

    @Test
    public void clearErrorMessage_setsValueToNull() {
        // Dispara uma mensagem de erro chamando carregarParticipantes com grupoId inválido
        // de forma indireta: o ViewModel ainda não foi inicializado (grupoId == -1)
        // então carregarParticipantes() é no-op. Definir manualmente via outro caminho:
        // Verificar somente que clearErrorMessage nula o valor após um setValue manual.
        // (Não há acesso a MutableLiveData diretamente; valida o contrato público.)
        // Chamamos realizarSorteio() sem init() — a lista está vazia (< 3), resultado != SUCCESS.
        viewModel.realizarSorteio();
        viewModel.clearSorteioResult();
        assertNull(viewModel.getSorteioResult().getValue());

        // errorMessage começa null — clearErrorMessage deve mantê-lo null (não lançar exceção)
        viewModel.clearErrorMessage();
        assertNull(viewModel.getErrorMessage().getValue());
    }

    // =========================================================
    // atualizarParticipante
    // =========================================================

    @Test
    public void atualizarParticipante_sucesso_emiteTrue() {
        inserirParticipante("Vera");
        List<Participante> lista = participanteDao.listarPorGrupo(grupoId);
        Participante p = lista.get(0);
        p.setNome("Vera Atualizada");

        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.atualizarParticipante(p);
        idleMainLooper();

        assertEquals(Boolean.TRUE, viewModel.getAtualizarSucesso().getValue());
    }

    @Test
    public void atualizarParticipante_idInvalido_emiteFalse() {
        Participante p = new Participante();
        p.setId(0);
        p.setNome("Fantasma");

        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.atualizarParticipante(p);
        idleMainLooper();

        assertEquals(Boolean.FALSE, viewModel.getAtualizarSucesso().getValue());
    }

    // =========================================================
    // marcarComoEnviado
    // =========================================================

    @Test
    public void marcarComoEnviado_setaFlagNoParticipante() {
        inserirParticipante("Xavier");
        List<Participante> antes = participanteDao.listarPorGrupo(grupoId);
        int id = antes.get(0).getId();

        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.marcarComoEnviado(id);
        idleMainLooper();

        List<Participante> apos = participanteDao.listarPorGrupo(grupoId);
        assertTrue(apos.get(0).isEnviado());
    }

    // =========================================================
    // salvarExclusoes
    // =========================================================

    @Test
    public void salvarExclusoes_adicionaERemoveCorretamente() {
        inserirParticipante("Yara");
        inserirParticipante("Zico");
        List<Participante> lista = participanteDao.listarPorGrupo(grupoId);
        int id1 = lista.get(0).getId();
        int id2 = lista.get(1).getId();

        viewModel.init(grupoId);
        idleMainLooper();

        // Adicionar exclusão
        viewModel.salvarExclusoes(id1, java.util.Arrays.asList(id2), java.util.Collections.emptyList());
        idleMainLooper();

        List<Participante> aposAdicionar = participanteDao.listarPorGrupo(grupoId);
        Participante p1 = aposAdicionar.stream().filter(p -> p.getId() == id1).findFirst().orElse(null);
        assertNotNull(p1);
        assertTrue(p1.getIdsExcluidos().contains(id2));

        // Remover exclusão
        viewModel.salvarExclusoes(id1, java.util.Collections.emptyList(), java.util.Arrays.asList(id2));
        idleMainLooper();

        List<Participante> aposRemover = participanteDao.listarPorGrupo(grupoId);
        Participante p1apos = aposRemover.stream().filter(p -> p.getId() == id1).findFirst().orElse(null);
        assertNotNull(p1apos);
        assertFalse(p1apos.getIdsExcluidos().contains(id2));
    }

    // =========================================================
    // inserirParticipante — caminho de erro
    // =========================================================

    @Test
    public void inserirParticipante_erroNoRepository_postaErrorMessage()
            throws InterruptedException {
        // listarPorGrupo retorna lista vazia para que carregarParticipantes() não polua errorMessage
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public void inserir(Participante participante, int grupoId) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };
        Participante p = new Participante();
        p.setNome("Erro");
        assertaErroComRealExecutor(repoQueLanca, () -> viewModel.inserirParticipante(p, grupoId));
    }

    // =========================================================
    // participants LiveData updates after mutations
    // =========================================================

    @Test
    public void removerParticipante_atualizaParticipantsLiveData() {
        inserirParticipante("Ana");
        inserirParticipante("Bruno");
        inserirParticipante("Carla");
        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> antes = viewModel.getParticipants().getValue();
        assertNotNull(antes);
        assertEquals(3, antes.size());
        int idRemover = antes.get(0).getId();

        viewModel.removerParticipante(idRemover);
        idleMainLooper();

        List<Participante> apos = viewModel.getParticipants().getValue();
        assertNotNull(apos);
        assertEquals(2, apos.size());
        assertFalse(apos.stream().anyMatch(p -> p.getId() == idRemover));
    }

    @Test
    public void inserirParticipante_atualizaParticipantsLiveData() {
        inserirParticipante("Diana");
        inserirParticipante("Eduardo");
        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> antes = viewModel.getParticipants().getValue();
        assertNotNull(antes);
        assertEquals(2, antes.size());

        Participante novo = new Participante();
        novo.setNome("Fernanda");
        novo.setTelefone("11988887777");
        viewModel.inserirParticipante(novo, grupoId);
        idleMainLooper();

        List<Participante> apos = viewModel.getParticipants().getValue();
        assertNotNull(apos);
        assertEquals(3, apos.size());
        assertTrue(apos.stream().anyMatch(p -> "Fernanda".equals(p.getNome())));
    }

    @Test
    public void deletarTodosDoGrupo_atualizaParticipantsLiveData() {
        inserirParticipante("Gabi");
        inserirParticipante("Hugo");
        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> antes = viewModel.getParticipants().getValue();
        assertNotNull(antes);
        assertFalse(antes.isEmpty());

        viewModel.deletarTodosDoGrupo(grupoId);
        idleMainLooper();

        List<Participante> apos = viewModel.getParticipants().getValue();
        assertNotNull(apos);
        assertTrue(apos.isEmpty());
    }

    @Test
    public void salvarExclusoes_atualizaParticipantsLiveData() {
        inserirParticipante("Ines");
        inserirParticipante("Jorge");
        List<Participante> lista = participanteDao.listarPorGrupo(grupoId);
        int id1 = lista.get(0).getId();
        int id2 = lista.get(1).getId();

        viewModel.init(grupoId);
        idleMainLooper();

        viewModel.salvarExclusoes(id1, java.util.Arrays.asList(id2), java.util.Collections.emptyList());
        idleMainLooper();

        // LiveData foi atualizado após a mutação
        List<Participante> apos = viewModel.getParticipants().getValue();
        assertNotNull(apos);
        assertEquals(2, apos.size());
        Participante p1 = apos.stream().filter(p -> p.getId() == id1).findFirst().orElse(null);
        assertNotNull(p1);
        assertTrue(p1.getIdsExcluidos().contains(id2));
    }

    // =========================================================
    // prepararMensagensSms
    // =========================================================

    @Test
    public void prepararMensagensSms_participanteComTelefone_emiteResultadoComMensagem() {
        inserirParticipante("Alice");
        inserirParticipante("Bob");
        inserirParticipante("Carol");
        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.realizarSorteio();
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);

        viewModel.prepararMensagensSms(lista);
        idleMainLooper();

        ParticipantesViewModel.MensagensSmsResultado resultado =
                viewModel.getMensagensSmsResult().getValue();
        assertNotNull(resultado);
        // Todos têm telefone cadastrado
        assertEquals(lista.size(), resultado.participantesComTelefone.size());
        for (Participante p : resultado.participantesComTelefone) {
            assertNotNull(resultado.mensagens.get(p.getId()));
            assertFalse(resultado.mensagens.get(p.getId()).isEmpty());
        }
    }

    @Test
    public void prepararMensagensSms_nenhumComTelefone_emiteListaVazia() {
        // Participante sem telefone
        Participante p = new Participante();
        p.setNome("SemFone");
        participanteDao.inserir(p, grupoId);

        viewModel.init(grupoId);
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);

        viewModel.prepararMensagensSms(lista);
        idleMainLooper();

        ParticipantesViewModel.MensagensSmsResultado resultado =
                viewModel.getMensagensSmsResult().getValue();
        assertNotNull(resultado);
        assertTrue(resultado.participantesComTelefone.isEmpty());
    }

    // =========================================================
    // prepararMensagemCompartilhamento
    // =========================================================

    @Test
    public void prepararMensagemCompartilhamento_emiteMensagemEMarcaEnviado() {
        inserirParticipante("David");
        inserirParticipante("Elena");
        inserirParticipante("Fred");
        viewModel.init(grupoId);
        idleMainLooper();
        viewModel.realizarSorteio();
        idleMainLooper();

        List<Participante> lista = viewModel.getParticipants().getValue();
        assertNotNull(lista);
        Participante primeiro = lista.get(0);

        viewModel.prepararMensagemCompartilhamento(primeiro);
        idleMainLooper();

        ParticipantesViewModel.MensagemCompartilhamentoResultado resultado =
                viewModel.getMensagemCompartilhamentoResult().getValue();
        assertNotNull(resultado);
        assertEquals(primeiro.getId(), resultado.participante.getId());
        assertNotNull(resultado.mensagem);
        assertFalse(resultado.mensagem.isEmpty());

        // Deve ter marcado como enviado
        List<Participante> apos = participanteDao.listarPorGrupo(grupoId);
        Participante primeirosApos = apos.stream()
                .filter(p -> p.getId() == primeiro.getId()).findFirst().orElse(null);
        assertNotNull(primeirosApos);
        assertTrue(primeirosApos.isEnviado());
    }

    // =========================================================
    // marcarComoEnviado — caminho de erro
    // =========================================================

    @Test
    public void marcarComoEnviado_erroNoRepository_postaErrorMessage()
            throws InterruptedException {
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public void marcarComoEnviado(int id) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };
        assertaErroComRealExecutor(repoQueLanca, () -> viewModel.marcarComoEnviado(1));
    }

    // =========================================================
    // atualizarParticipante — caminho de erro
    // =========================================================

    @Test
    public void atualizarParticipante_excecaoNoRepository_emiteFalse() {
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public boolean atualizar(Participante participante) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };

        viewModel.setRepositories(repoQueLanca, desejoRepository);
        viewModel.init(grupoId);
        idleMainLooper();

        Participante p = new Participante();
        p.setId(1);
        p.setNome("Teste");
        viewModel.atualizarParticipante(p);
        idleMainLooper();

        assertEquals(Boolean.FALSE, viewModel.getAtualizarSucesso().getValue());
    }

    // =========================================================
    // removerParticipante — caminho de erro
    // =========================================================

    @Test
    public void removerParticipante_erroNoRepository_postaErrorMessage()
            throws InterruptedException {
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public void remover(int id) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };
        assertaErroComRealExecutor(repoQueLanca, () -> viewModel.removerParticipante(1));
    }

    // =========================================================
    // deletarTodosDoGrupo — caminho de erro
    // =========================================================

    @Test
    public void deletarTodosDoGrupo_erroNoRepository_postaErrorMessage()
            throws InterruptedException {
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public void deletarTodosDoGrupo(int grupoId) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };
        assertaErroComRealExecutor(repoQueLanca, () -> viewModel.deletarTodosDoGrupo(grupoId));
    }

    // =========================================================
    // salvarExclusoes — caminho de erro
    // =========================================================

    @Test
    public void salvarExclusoes_erroNoRepository_postaErrorMessage()
            throws InterruptedException {
        ParticipanteRepository repoQueLanca = new ParticipanteRepository(app) {
            @Override
            public void salvarExclusoes(int participanteId, List<Integer> adicionar,
                    List<Integer> remover) {
                throw new android.database.sqlite.SQLiteException("falha simulada");
            }
            @Override
            public List<Participante> listarPorGrupo(int grupoId) {
                return java.util.Collections.emptyList();
            }
        };
        assertaErroComRealExecutor(repoQueLanca,
                () -> viewModel.salvarExclusoes(1, java.util.Arrays.asList(2), java.util.Collections.emptyList()));
    }
}
