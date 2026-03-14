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

    /** Insere participante e retorna o ID atribuído pelo banco. */
    private int inserir(String nome) {
        Participante p = new Participante();
        p.setNome(nome);
        repository.inserir(p, grupoId);
        return p.getId();
    }

    /** Busca exclusões de um participante com uma única query. */
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
        int idAna = inserir("Ana");
        int idBruno = inserir("Bruno");

        repository.salvarExclusoes(idAna, Arrays.asList(idBruno), Collections.emptyList());

        assertTrue(exclusoesDeParticipante(idAna).contains(idBruno));
    }

    @Test
    public void salvarExclusoes_adicionarMultiplas_todasPersistidas() {
        int idCarla = inserir("Carla");
        int idDiego = inserir("Diego");
        int idEva = inserir("Eva");

        repository.salvarExclusoes(idCarla, Arrays.asList(idDiego, idEva), Collections.emptyList());

        List<Integer> excl = exclusoesDeParticipante(idCarla);
        assertTrue(excl.contains(idDiego));
        assertTrue(excl.contains(idEva));
    }

    // ===== remover exclusões =====

    @Test
    public void salvarExclusoes_remover_removeExclusaoExistente() {
        int idFelipe = inserir("Felipe");
        int idGabi = inserir("Gabi");

        repository.salvarExclusoes(idFelipe, Arrays.asList(idGabi), Collections.emptyList());
        assertTrue(exclusoesDeParticipante(idFelipe).contains(idGabi));

        repository.salvarExclusoes(idFelipe, Collections.emptyList(), Arrays.asList(idGabi));
        assertFalse(exclusoesDeParticipante(idFelipe).contains(idGabi));
    }

    // ===== adicionar e remover na mesma chamada =====

    @Test
    public void salvarExclusoes_adicionarERemover_transacaoAtomicaCorreta() {
        int idHugo = inserir("Hugo");
        int idIris = inserir("Iris");
        int idJoao = inserir("João");

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
        int idKaren = inserir("Karen");
        int idLucas = inserir("Lucas");

        repository.salvarExclusoes(idKaren, Arrays.asList(idLucas), Collections.emptyList());
        int antes = exclusoesDeParticipante(idKaren).size();

        repository.salvarExclusoes(idKaren, Collections.emptyList(), Collections.emptyList());

        assertEquals(antes, exclusoesDeParticipante(idKaren).size());
    }

    @Test
    public void salvarExclusoes_ambosVazios_naoLancaExcecao() {
        int idMaria = inserir("Maria");
        // Não deve lançar exceção
        repository.salvarExclusoes(idMaria, Collections.emptyList(), Collections.emptyList());
    }

    // ===== não contamina outros participantes =====

    @Test
    public void salvarExclusoes_naoAfetaOutrosParticipantes() {
        int idNadia = inserir("Nadia");
        int idOtto = inserir("Otto");
        int idPaulo = inserir("Paulo");

        repository.salvarExclusoes(idNadia, Arrays.asList(idOtto), Collections.emptyList());

        assertTrue(exclusoesDeParticipante(idOtto).isEmpty());
        assertTrue(exclusoesDeParticipante(idPaulo).isEmpty());
    }
}