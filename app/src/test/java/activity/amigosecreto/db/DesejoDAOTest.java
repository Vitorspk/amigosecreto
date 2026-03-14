package activity.amigosecreto.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DesejoDAOTest {

    private DesejoDAO dao;
    private ParticipanteDAO participanteDao;
    private GrupoDAO grupoDao;
    private int grupoId;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        dao = new DesejoDAO(ctx);
        dao.open();
        participanteDao = new ParticipanteDAO(ctx);
        participanteDao.open();
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();

        Grupo g = new Grupo();
        g.setNome("Grupo Teste");
        grupoId = (int) grupoDao.inserir(g);
    }

    @After
    public void tearDown() {
        grupoDao.limparTudo(); // limpa desejo, participante, exclusao e grupo em cascata
        dao.close();
        participanteDao.close();
        grupoDao.close();
    }

    private Participante criarParticipante(String nome) {
        Participante p = new Participante();
        p.setNome(nome);
        participanteDao.inserir(p, grupoId);
        assertTrue("ParticipanteDAO.inserir deve atribuir ID > 0", p.getId() > 0);
        return p;
    }

    private Desejo buildDesejo(String produto, int participanteId) {
        Desejo d = new Desejo();
        d.setProduto(produto);
        d.setCategoria("Eletrônicos");
        d.setLojas("Amazon");
        d.setPrecoMinimo(100.0);
        d.setPrecoMaximo(200.0);
        d.setParticipanteId(participanteId);
        return d;
    }

    // --- inserir ---

    @Test
    public void inserir_assignsId() {
        Participante p = criarParticipante("Ana");
        Desejo d = buildDesejo("Fone", p.getId());
        dao.inserir(d);
        assertTrue("ID deve ser > 0 após inserir", d.getId() > 0);
    }

    @Test
    public void inserir_persists_listar() {
        Participante p = criarParticipante("Bruno");
        Desejo d = buildDesejo("Teclado", p.getId());
        dao.inserir(d);

        List<Desejo> lista = dao.listar();
        assertTrue(lista.stream().anyMatch(x -> "Teclado".equals(x.getProduto())));
    }

    @Test
    public void inserir_multiple_allPersisted() {
        Participante p = criarParticipante("Carla");
        dao.inserir(buildDesejo("Mouse", p.getId()));
        dao.inserir(buildDesejo("Monitor", p.getId()));
        dao.inserir(buildDesejo("Webcam", p.getId()));

        List<Desejo> lista = dao.listarPorParticipante(p.getId());
        assertEquals(3, lista.size());
    }

    // --- listar ---

    @Test
    public void listar_emptyDatabase_returnsEmptyList() {
        List<Desejo> lista = dao.listar();
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }

    @Test
    public void listar_returnsAllInserted() {
        Participante p = criarParticipante("Daniel");
        dao.inserir(buildDesejo("Livro A", p.getId()));
        dao.inserir(buildDesejo("Livro B", p.getId()));

        List<Desejo> lista = dao.listar();
        assertEquals(2, lista.size());
    }

    // --- listarPorParticipante ---

    @Test
    public void listarPorParticipante_returnsOnlyThatParticipant() {
        Participante p1 = criarParticipante("Eva");
        Participante p2 = criarParticipante("Felipe");

        dao.inserir(buildDesejo("Tablet", p1.getId()));
        dao.inserir(buildDesejo("Smartphone", p2.getId()));
        dao.inserir(buildDesejo("Notebook", p1.getId()));

        List<Desejo> deP1 = dao.listarPorParticipante(p1.getId());
        assertEquals(2, deP1.size());
        assertTrue(deP1.stream().allMatch(d -> d.getParticipanteId() == p1.getId()));
    }

    @Test
    public void listarPorParticipante_noDesejo_returnsEmpty() {
        Participante p = criarParticipante("Gabi");
        List<Desejo> lista = dao.listarPorParticipante(p.getId());
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }

    // --- contarDesejosPorParticipante ---

    @Test
    public void contarDesejosPorParticipante_correctCount() {
        Participante p = criarParticipante("Hugo");
        dao.inserir(buildDesejo("Item1", p.getId()));
        dao.inserir(buildDesejo("Item2", p.getId()));

        int count = dao.contarDesejosPorParticipante(p.getId());
        assertEquals(2, count);
    }

    @Test
    public void contarDesejosPorParticipante_noDesejo_returnsZero() {
        Participante p = criarParticipante("Iris");
        assertEquals(0, dao.contarDesejosPorParticipante(p.getId()));
    }

    // --- buscarPorId ---

    @Test
    public void buscarPorId_existingId_returnsDesejo() {
        Participante p = criarParticipante("João");
        Desejo d = buildDesejo("Câmera", p.getId());
        dao.inserir(d);

        Desejo found = dao.buscarPorId(d.getId());
        assertNotNull(found);
        assertEquals("Câmera", found.getProduto());
        assertEquals("Eletrônicos", found.getCategoria());
        assertEquals(100.0, found.getPrecoMinimo(), 0.001);
        assertEquals(200.0, found.getPrecoMaximo(), 0.001);
        assertEquals("Amazon", found.getLojas());
        assertEquals(p.getId(), found.getParticipanteId());
    }

    @Test
    public void buscarPorId_nonExistingId_returnsNull() {
        Desejo found = dao.buscarPorId(99999);
        assertNull(found);
    }

    // --- alterar ---

    @Test
    public void alterar_updatesProduto() {
        Participante p = criarParticipante("Karen");
        Desejo original = buildDesejo("Impressora", p.getId());
        dao.inserir(original);

        Desejo paraAtualizar = dao.buscarPorId(original.getId());
        assertNotNull(paraAtualizar);
        paraAtualizar.setProduto("Impressora 3D");
        dao.alterar(original, paraAtualizar);

        Desejo found = dao.buscarPorId(original.getId());
        assertNotNull(found);
        assertEquals("Impressora 3D", found.getProduto());
        assertEquals(paraAtualizar.getCategoria(), found.getCategoria());
        assertEquals(paraAtualizar.getLojas(), found.getLojas());
        assertEquals(paraAtualizar.getPrecoMinimo(), found.getPrecoMinimo(), 0.001);
        assertEquals(paraAtualizar.getPrecoMaximo(), found.getPrecoMaximo(), 0.001);
    }

    @Test
    public void alterar_updatesAllFields() {
        Participante p = criarParticipante("Lucas");
        Desejo original = buildDesejo("Produto X", p.getId());
        dao.inserir(original);

        Desejo updated = new Desejo();
        updated.setId(original.getId());
        updated.setProduto("Produto Y");
        updated.setCategoria("Games");
        updated.setLojas("Nuuvem");
        updated.setPrecoMinimo(50.0);
        updated.setPrecoMaximo(150.0);
        updated.setParticipanteId(p.getId());
        dao.alterar(original, updated);

        Desejo found = dao.buscarPorId(original.getId());
        assertNotNull(found);
        assertEquals("Produto Y", found.getProduto());
        assertEquals("Games", found.getCategoria());
        assertEquals("Nuuvem", found.getLojas());
        assertEquals(50.0, found.getPrecoMinimo(), 0.001);
        assertEquals(150.0, found.getPrecoMaximo(), 0.001);
        assertEquals(p.getId(), found.getParticipanteId());
    }

    // --- remover ---

    @Test
    public void remover_deletesFromDatabase() {
        Participante p = criarParticipante("Maria");
        Desejo d = buildDesejo("Perfume", p.getId());
        dao.inserir(d);

        dao.remover(d);

        Desejo found = dao.buscarPorId(d.getId());
        assertNull(found);
    }

    @Test
    public void remover_onlyRemovesTarget() {
        Participante p = criarParticipante("Nadia");
        Desejo d1 = buildDesejo("Bolsa", p.getId());
        Desejo d2 = buildDesejo("Carteira", p.getId());
        dao.inserir(d1);
        dao.inserir(d2);

        dao.remover(d1);

        assertNull(dao.buscarPorId(d1.getId()));
        assertNotNull(dao.buscarPorId(d2.getId()));
    }

    // --- proximoId ---

    @Test
    public void proximoId_emptyTable_returnsOne() {
        assertEquals(1, dao.proximoId());
    }

    @Test
    public void proximoId_afterInsert_returnsMaxPlusOne() {
        Participante p = criarParticipante("Otto");
        Desejo d = buildDesejo("Relógio", p.getId());
        dao.inserir(d);

        int next = dao.proximoId();
        assertEquals(d.getId() + 1, next);
    }
}
