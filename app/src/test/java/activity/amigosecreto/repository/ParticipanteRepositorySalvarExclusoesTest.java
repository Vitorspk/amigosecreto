package activity.amigosecreto.repository;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.Participante;

import static org.junit.Assert.*;

/**
 * Testes diretos para ParticipanteRepository.salvarExclusoes().
 *
 * Esse método executa uma transação atômica que adiciona e remove exclusões
 * em uma única chamada. Não tinha nenhuma cobertura direta — era acionado
 * apenas indiretamente via ViewModel. Esses testes garantem que o contrato
 * da transação é preservado após a migração para Kotlin.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ParticipanteRepositorySalvarExclusoesTest {

    private ParticipanteRepository repository;
    private GrupoDAO grupoDao;
    private int grupoId;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();

        Grupo g = new Grupo();
        g.setNome("Grupo Exclusões");
        grupoId = (int) grupoDao.inserir(g);

        repository = new ParticipanteRepository(ctx);
    }

    @After
    public void tearDown() {
        grupoDao.limparTudo();
        grupoDao.close();
    }

    private Participante inserir(String nome) {
        Participante p = new Participante();
        p.setNome(nome);
        repository.inserir(p, grupoId);
        return p;
    }

    private int idOf(String nome) {
        return repository.listarPorGrupo(grupoId).stream()
                .filter(p -> p.getNome().equals(nome))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participante não encontrado: " + nome))
                .getId();
    }

    private List<Integer> exclusoesDeParticipante(int participanteId) {
        return repository.listarPorGrupo(grupoId).stream()
                .filter(p -> p.getId() == participanteId)
                .findFirst()
                .map(Participante::getIdsExcluidos)
                .orElse(Collections.emptyList());
    }

    // ===== adicionar exclusões =====

    @Test
    public void salvarExclusoes_adicionar_persisteExclusao() {
        inserir("Ana");
        inserir("Bruno");
        int idAna = idOf("Ana");
        int idBruno = idOf("Bruno");

        repository.salvarExclusoes(idAna, Arrays.asList(idBruno), Collections.emptyList());

        List<Integer> excl = exclusoesDeParticipante(idAna);
        assertTrue(excl.contains(idBruno));
    }

    @Test
    public void salvarExclusoes_adicionarMultiplas_todasPersistidas() {
        inserir("Carla");
        inserir("Diego");
        inserir("Eva");
        int idCarla = idOf("Carla");
        int idDiego = idOf("Diego");
        int idEva = idOf("Eva");

        repository.salvarExclusoes(idCarla, Arrays.asList(idDiego, idEva), Collections.emptyList());

        List<Integer> excl = exclusoesDeParticipante(idCarla);
        assertTrue(excl.contains(idDiego));
        assertTrue(excl.contains(idEva));
    }

    // ===== remover exclusões =====

    @Test
    public void salvarExclusoes_remover_removeExclusaoExistente() {
        inserir("Felipe");
        inserir("Gabi");
        int idFelipe = idOf("Felipe");
        int idGabi = idOf("Gabi");

        // Primeiro adiciona
        repository.salvarExclusoes(idFelipe, Arrays.asList(idGabi), Collections.emptyList());
        assertTrue(exclusoesDeParticipante(idFelipe).contains(idGabi));

        // Agora remove
        repository.salvarExclusoes(idFelipe, Collections.emptyList(), Arrays.asList(idGabi));
        assertFalse(exclusoesDeParticipante(idFelipe).contains(idGabi));
    }

    // ===== adicionar e remover na mesma chamada =====

    @Test
    public void salvarExclusoes_adicionarERemover_transacaoAtomicaCorreta() {
        inserir("Hugo");
        inserir("Iris");
        inserir("João");
        int idHugo = idOf("Hugo");
        int idIris = idOf("Iris");
        int idJoao = idOf("João");

        // Adiciona Hugo → Iris
        repository.salvarExclusoes(idHugo, Arrays.asList(idIris), Collections.emptyList());
        assertTrue(exclusoesDeParticipante(idHugo).contains(idIris));

        // Troca: remove Hugo → Iris, adiciona Hugo → João
        repository.salvarExclusoes(idHugo, Arrays.asList(idJoao), Arrays.asList(idIris));

        List<Integer> excl = exclusoesDeParticipante(idHugo);
        assertTrue("Hugo deve excluir João", excl.contains(idJoao));
        assertFalse("Hugo não deve mais excluir Iris", excl.contains(idIris));
    }

    // ===== listas vazias =====

    @Test
    public void salvarExclusoes_listasVazias_naoAlteraEstadoAtual() {
        inserir("Karen");
        inserir("Lucas");
        int idKaren = idOf("Karen");
        int idLucas = idOf("Lucas");

        repository.salvarExclusoes(idKaren, Arrays.asList(idLucas), Collections.emptyList());
        int antes = exclusoesDeParticipante(idKaren).size();

        // Chamada com listas vazias — não deve mudar nada
        repository.salvarExclusoes(idKaren, Collections.emptyList(), Collections.emptyList());

        assertEquals(antes, exclusoesDeParticipante(idKaren).size());
    }

    @Test
    public void salvarExclusoes_ambosVazios_naoLancaExcecao() {
        inserir("Maria");
        int idMaria = idOf("Maria");
        // Não deve lançar exceção
        repository.salvarExclusoes(idMaria, Collections.emptyList(), Collections.emptyList());
    }

    // ===== não contamina outros participantes =====

    @Test
    public void salvarExclusoes_naoAfetaOutrosParticipantes() {
        inserir("Nadia");
        inserir("Otto");
        inserir("Paulo");
        int idNadia = idOf("Nadia");
        int idOtto = idOf("Otto");
        int idPaulo = idOf("Paulo");

        // Adiciona exclusão apenas para Nadia
        repository.salvarExclusoes(idNadia, Arrays.asList(idOtto), Collections.emptyList());

        // Otto e Paulo não devem ter exclusões
        assertTrue(exclusoesDeParticipante(idOtto).isEmpty());
        assertTrue(exclusoesDeParticipante(idPaulo).isEmpty());
    }
}