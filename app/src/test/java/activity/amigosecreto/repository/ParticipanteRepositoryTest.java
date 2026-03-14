package activity.amigosecreto.repository;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Map;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.Participante;

import static org.junit.Assert.*;

/**
 * Testes de integração do ParticipanteRepository via Robolectric + SQLite real.
 *
 * Valida que o repository delega corretamente ao DAO e que os dados são
 * persistidos e recuperados sem intervenção direta das Activities/ViewModels.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ParticipanteRepositoryTest {

    private ParticipanteRepository repository;
    private GrupoDAO grupoDao;
    private int grupoId;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();

        Grupo g = new Grupo();
        g.setNome("Grupo Teste");
        g.setData("01/01/2025");
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
        p.setTelefone("11999999999");
        repository.inserir(p, grupoId);
        return p;
    }

    // =========================================================
    // inserir / listarPorGrupo
    // =========================================================

    @Test
    public void inserir_e_listarPorGrupo_retornaParticipanteInserido() {
        inserir("Ana");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).getNome());
    }

    @Test
    public void listarPorGrupo_grupoVazio_retornaListaVazia() {
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }

    @Test
    public void inserir_multiplos_listarRetornaTodos() {
        inserir("Bruno");
        inserir("Carla");
        inserir("Diego");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        assertEquals(3, lista.size());
    }

    // =========================================================
    // atualizar
    // =========================================================

    @Test
    public void atualizar_nomeAlterado_persistido() {
        Participante p = inserir("Eva");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        Participante inserido = lista.get(0);

        inserido.setNome("Eva Atualizada");
        boolean ok = repository.atualizar(inserido);

        assertTrue(ok);
        List<Participante> apos = repository.listarPorGrupo(grupoId);
        assertEquals("Eva Atualizada", apos.get(0).getNome());
    }

    @Test
    public void atualizar_idInvalido_retornaFalse() {
        Participante p = new Participante();
        p.setId(0);
        p.setNome("Fantasma");
        assertFalse(repository.atualizar(p));
    }

    // =========================================================
    // remover
    // =========================================================

    @Test
    public void remover_participanteExistente_removidoDaLista() {
        inserir("Felipe");
        List<Participante> antes = repository.listarPorGrupo(grupoId);
        int id = antes.get(0).getId();

        repository.remover(id);

        List<Participante> depois = repository.listarPorGrupo(grupoId);
        assertTrue(depois.isEmpty());
    }

    // =========================================================
    // deletarTodosDoGrupo
    // =========================================================

    @Test
    public void deletarTodosDoGrupo_removeTodasEntradas() {
        inserir("Gabi");
        inserir("Hugo");
        repository.deletarTodosDoGrupo(grupoId);
        assertTrue(repository.listarPorGrupo(grupoId).isEmpty());
    }

    // =========================================================
    // exclusões
    // =========================================================

    @Test
    public void adicionarExclusao_apareceCampoIdsExcluidos() {
        Participante p1 = inserir("Iris");
        Participante p2 = inserir("João");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        int id1 = lista.get(0).getId();
        int id2 = lista.get(1).getId();

        repository.adicionarExclusao(id1, id2);

        List<Participante> apos = repository.listarPorGrupo(grupoId);
        Participante p1Apos = apos.stream().filter(p -> p.getId() == id1).findFirst().orElse(null);
        assertNotNull(p1Apos);
        assertTrue(p1Apos.getIdsExcluidos().contains(id2));
    }

    @Test
    public void removerExclusao_removeCorretamente() {
        inserir("Karen");
        inserir("Lucas");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        int id1 = lista.get(0).getId();
        int id2 = lista.get(1).getId();

        repository.adicionarExclusao(id1, id2);
        repository.removerExclusao(id1, id2);

        List<Participante> apos = repository.listarPorGrupo(grupoId);
        Participante p1Apos = apos.stream().filter(p -> p.getId() == id1).findFirst().orElse(null);
        assertNotNull(p1Apos);
        assertFalse(p1Apos.getIdsExcluidos().contains(id2));
    }

    // =========================================================
    // marcarComoEnviado
    // =========================================================

    @Test
    public void marcarComoEnviado_setaFlagEnviado() {
        inserir("Maria");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        int id = lista.get(0).getId();

        repository.marcarComoEnviado(id);

        List<Participante> apos = repository.listarPorGrupo(grupoId);
        assertTrue(apos.get(0).isEnviado());
    }

    // =========================================================
    // salvarSorteio / limparSorteioDoGrupo
    // =========================================================

    @Test
    public void salvarSorteio_persisteAmigoSorteadoId() {
        inserir("Nadia");
        inserir("Otto");
        inserir("Paulo");
        List<Participante> lista = repository.listarPorGrupo(grupoId);

        // sorteio circular: 0→1, 1→2, 2→0
        List<Participante> sorteados = java.util.Arrays.asList(lista.get(1), lista.get(2), lista.get(0));
        boolean ok = repository.salvarSorteio(lista, sorteados);
        assertTrue(ok);

        List<Participante> apos = repository.listarPorGrupo(grupoId);
        for (Participante p : apos) {
            assertNotNull(p.getAmigoSorteadoId());
            assertTrue(p.getAmigoSorteadoId() > 0);
        }
    }

    @Test
    public void limparSorteioDoGrupo_resetaAmigoSorteadoEEnviado() {
        inserir("Quesia");
        inserir("Rafael");
        inserir("Sara");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        List<Participante> sorteados = java.util.Arrays.asList(lista.get(1), lista.get(2), lista.get(0));
        repository.salvarSorteio(lista, sorteados);

        repository.limparSorteioDoGrupo(grupoId);

        List<Participante> apos = repository.listarPorGrupo(grupoId);
        for (Participante p : apos) {
            assertNull(p.getAmigoSorteadoId());
            assertFalse(p.isEnviado());
        }
    }

    // =========================================================
    // getNomeAmigoSorteado
    // =========================================================

    @Test
    public void getNomeAmigoSorteado_retornaNomedoAmigo() {
        inserir("Tiago");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        int id = lista.get(0).getId();

        String nome = repository.getNomeAmigoSorteado(id);
        assertEquals("Tiago", nome);
    }

    @Test
    public void getNomeAmigoSorteado_idInexistente_retornaNinguem() {
        String nome = repository.getNomeAmigoSorteado(99999);
        assertEquals("Ninguém", nome);
    }

    // =========================================================
    // contarPorGrupo
    // =========================================================

    @Test
    public void contarPorGrupo_retornaContagemCorreta() {
        inserir("Uma");
        inserir("Vitor");
        Map<Integer, Integer> mapa = repository.contarPorGrupo();
        assertEquals(Integer.valueOf(2), mapa.get(grupoId));
    }

    // =========================================================
    // buscarPorId
    // =========================================================

    @Test
    public void buscarPorId_retornaParticipanteCorreto() {
        inserir("Wesley");
        List<Participante> lista = repository.listarPorGrupo(grupoId);
        int id = lista.get(0).getId();

        Participante p = repository.buscarPorId(id);
        assertNotNull(p);
        assertEquals("Wesley", p.getNome());
    }

    @Test
    public void buscarPorId_idInexistente_retornaNull() {
        assertNull(repository.buscarPorId(99999));
    }
}
