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

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;

import static org.junit.Assert.*;

/**
 * Testes de integração do DesejoRepository via Robolectric + SQLite real.
 *
 * Valida que o repository delega corretamente ao DAO e que os dados são
 * persistidos e recuperados sem intervenção direta das Activities/ViewModels.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DesejoRepositoryTest {

    private DesejoRepository repository;
    private GrupoDAO grupoDao;
    private ParticipanteDAO participanteDao;
    private int grupoId;
    private int participanteId;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();

        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();
        Grupo g = new Grupo();
        g.setNome("Grupo Teste");
        g.setData("01/01/2025");
        grupoId = (int) grupoDao.inserir(g);

        participanteDao = new ParticipanteDAO(ctx);
        participanteDao.open();
        Participante p = new Participante();
        p.setNome("Participante Teste");
        participanteDao.inserir(p, grupoId);
        // Recupera ID gerado pelo banco e fecha imediatamente — não é mais necessário.
        participanteId = participanteDao.listarPorGrupo(grupoId).get(0).getId();
        participanteDao.close();

        repository = new DesejoRepository(ctx);
    }

    @After
    public void tearDown() {
        grupoDao.limparTudo();
        grupoDao.close();
    }

    private Desejo criarDesejo(String produto) {
        Desejo d = new Desejo();
        d.setProduto(produto);
        d.setParticipanteId(participanteId);
        return d;
    }

    // =========================================================
    // inserir / listarPorParticipante
    // =========================================================

    @Test
    public void inserir_e_listarPorParticipante_retornaDesejoInserido() {
        Desejo d = criarDesejo("Livro");
        repository.inserir(d);

        List<Desejo> lista = repository.listarPorParticipante(participanteId);
        assertEquals(1, lista.size());
        assertEquals("Livro", lista.get(0).getProduto());
    }

    @Test
    public void listarPorParticipante_semDesejos_retornaListaVazia() {
        List<Desejo> lista = repository.listarPorParticipante(participanteId);
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }

    @Test
    public void inserir_multiplos_listarRetornaTodos() {
        repository.inserir(criarDesejo("Caneta"));
        repository.inserir(criarDesejo("Caderno"));
        repository.inserir(criarDesejo("Mochila"));

        List<Desejo> lista = repository.listarPorParticipante(participanteId);
        assertEquals(3, lista.size());
    }

    @Test
    public void inserir_atribuiIdGeradoPeloBanco() {
        Desejo d = criarDesejo("Fone");
        repository.inserir(d);
        assertTrue(d.getId() > 0);
    }

    // =========================================================
    // contarDesejosPorParticipante
    // =========================================================

    @Test
    public void contarDesejosPorParticipante_semDesejos_retornaZero() {
        assertEquals(0, repository.contarDesejosPorParticipante(participanteId));
    }

    @Test
    public void contarDesejosPorParticipante_comDesejos_retornaContagem() {
        repository.inserir(criarDesejo("Item A"));
        repository.inserir(criarDesejo("Item B"));
        assertEquals(2, repository.contarDesejosPorParticipante(participanteId));
    }

    // =========================================================
    // alterar
    // =========================================================

    @Test
    public void alterar_produtoAtualizado_persistido() {
        Desejo original = criarDesejo("Produto Original");
        repository.inserir(original);

        Desejo atualizado = criarDesejo("Produto Atualizado");
        atualizado.setId(original.getId());
        repository.alterar(original, atualizado);

        Desejo buscado = repository.buscarPorId(original.getId());
        assertNotNull(buscado);
        assertEquals("Produto Atualizado", buscado.getProduto());
    }

    // =========================================================
    // remover
    // =========================================================

    @Test
    public void remover_desejoExistente_removidoDaLista() {
        Desejo d = criarDesejo("Item para Remover");
        repository.inserir(d);
        assertEquals(1, repository.contarDesejosPorParticipante(participanteId));

        repository.remover(d);
        assertEquals(0, repository.contarDesejosPorParticipante(participanteId));
    }

    // =========================================================
    // buscarPorId
    // =========================================================

    @Test
    public void buscarPorId_retornaDesejoCorreto() {
        Desejo d = criarDesejo("Item Busca");
        repository.inserir(d);

        Desejo buscado = repository.buscarPorId(d.getId());
        assertNotNull(buscado);
        assertEquals("Item Busca", buscado.getProduto());
        assertEquals(participanteId, buscado.getParticipanteId());
    }

    @Test
    public void buscarPorId_idInexistente_retornaNull() {
        assertNull(repository.buscarPorId(99999));
    }

    // =========================================================
    // listar (todos)
    // =========================================================

    @Test
    public void listar_retornaDesejosDeTodosParticipantes() {
        repository.inserir(criarDesejo("Bola"));
        repository.inserir(criarDesejo("Raquete"));

        List<Desejo> todos = repository.listar();
        assertTrue(todos.size() >= 2);
    }

    // =========================================================
    // contarDesejosPorGrupo
    // =========================================================

    @Test
    public void contarDesejosPorGrupo_grupoVazio_retornaMapaVazio() {
        Map<Integer, Integer> mapa = repository.contarDesejosPorGrupo(grupoId);
        assertNotNull(mapa);
        assertTrue(mapa.isEmpty());
    }

    @Test
    public void contarDesejosPorGrupo_multiplosPorParticipante_somaCorreta() {
        repository.inserir(criarDesejo("A"));
        repository.inserir(criarDesejo("B"));
        repository.inserir(criarDesejo("C"));

        Map<Integer, Integer> mapa = repository.contarDesejosPorGrupo(grupoId);
        assertEquals(Integer.valueOf(3), mapa.get(participanteId));
    }

    // =========================================================
    // listarDesejosPorGrupo
    // =========================================================

    @Test
    public void listarDesejosPorGrupo_retornaTodosAgrupadosPorParticipante() {
        repository.inserir(criarDesejo("X"));
        repository.inserir(criarDesejo("Y"));

        Map<Integer, List<Desejo>> mapa = repository.listarDesejosPorGrupo(grupoId);
        assertNotNull(mapa);
        assertTrue(mapa.containsKey(participanteId));
        assertEquals(2, mapa.get(participanteId).size());
    }

    @Test
    public void listarDesejosPorGrupo_participanteSemDesejos_naoAparece() {
        // Sem inserir desejos — mapa deve estar vazio (participante sem desejos não aparece).
        Map<Integer, List<Desejo>> mapa = repository.listarDesejosPorGrupo(grupoId);
        assertFalse(mapa.containsKey(participanteId));
    }

    @Test
    public void inserir_comPrecoECategoria_persistidosCorretamente() {
        Desejo d = criarDesejo("Tênis");
        d.setCategoria("Esporte");
        d.setPrecoMinimo(100.0);
        d.setPrecoMaximo(300.0);
        d.setLojas("Decathlon, Netshoes");
        repository.inserir(d);

        Desejo buscado = repository.buscarPorId(d.getId());
        assertNotNull(buscado);
        assertEquals("Esporte", buscado.getCategoria());
        assertEquals(100.0, buscado.getPrecoMinimo(), 0.01);
        assertEquals(300.0, buscado.getPrecoMaximo(), 0.01);
        assertEquals("Decathlon, Netshoes", buscado.getLojas());
    }
}
