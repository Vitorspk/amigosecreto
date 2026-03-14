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
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Testes para os métodos de batch query do DesejoDAO:
 *   - contarDesejosPorGrupo(int grupoId)
 *   - listarDesejosPorGrupo(int grupoId)
 *
 * Esses métodos usam INNER JOIN + GROUP BY e não tinham cobertura.
 * São críticos para a migração para Kotlin pois qualquer erro de índice
 * de coluna ou alias SQL causaria NullPointerException silencioso.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DesejoDAOBatchQueryTest {

    private DesejoDAO desejoDao;
    private ParticipanteDAO participanteDao;
    private GrupoDAO grupoDao;
    private int grupoId;
    private int grupoId2;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        desejoDao = new DesejoDAO(ctx);
        desejoDao.open();
        participanteDao = new ParticipanteDAO(ctx);
        participanteDao.open();
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();

        Grupo g1 = new Grupo();
        g1.setNome("Grupo A");
        grupoId = (int) grupoDao.inserir(g1);

        Grupo g2 = new Grupo();
        g2.setNome("Grupo B");
        grupoId2 = (int) grupoDao.inserir(g2);
    }

    @After
    public void tearDown() {
        // limparTudo() apaga explicitamente exclusao, desejo, participante e grupo
        // em ordem de dependência — funciona como cascata manual (sem FK CASCADE no DDL)
        grupoDao.limparTudo();
        grupoDao.close();
        participanteDao.close();
        desejoDao.close();
    }

    private Participante inserirParticipante(String nome, int gId) {
        Participante p = new Participante();
        p.setNome(nome);
        participanteDao.inserir(p, gId);
        return p;
    }

    private void inserirDesejo(String produto, int participanteId) {
        Desejo d = new Desejo();
        d.setProduto(produto);
        d.setCategoria("Cat");
        d.setLojas("Loja");
        d.setPrecoMinimo(10.0);
        d.setPrecoMaximo(50.0);
        d.setParticipanteId(participanteId);
        desejoDao.inserir(d);
    }

    // ===== contarDesejosPorGrupo =====

    @Test
    public void contarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() {
        inserirParticipante("Ana", grupoId); // participante sem desejos
        Map<Integer, Integer> mapa = desejoDao.contarDesejosPorGrupo(grupoId);
        assertNotNull(mapa);
        assertTrue(mapa.isEmpty());
    }

    @Test
    public void contarDesejosPorGrupo_umParticipanteComDesejos_contagemCorreta() {
        Participante p = inserirParticipante("Bruno", grupoId);
        inserirDesejo("Item1", p.getId());
        inserirDesejo("Item2", p.getId());
        inserirDesejo("Item3", p.getId());

        Map<Integer, Integer> mapa = desejoDao.contarDesejosPorGrupo(grupoId);
        assertEquals(Integer.valueOf(3), mapa.get(p.getId()));
    }

    @Test
    public void contarDesejosPorGrupo_variosParticipantes_contagemsIndependentes() {
        Participante p1 = inserirParticipante("Carla", grupoId);
        Participante p2 = inserirParticipante("Diego", grupoId);
        inserirDesejo("X", p1.getId());
        inserirDesejo("Y", p1.getId());
        inserirDesejo("Z", p2.getId());

        Map<Integer, Integer> mapa = desejoDao.contarDesejosPorGrupo(grupoId);
        assertEquals(Integer.valueOf(2), mapa.get(p1.getId()));
        assertEquals(Integer.valueOf(1), mapa.get(p2.getId()));
    }

    @Test
    public void contarDesejosPorGrupo_naoContaminaOutroGrupo() {
        Participante pA = inserirParticipante("Eva", grupoId);
        Participante pB = inserirParticipante("Felipe", grupoId2);
        inserirDesejo("ItemA", pA.getId());
        inserirDesejo("ItemB1", pB.getId());
        inserirDesejo("ItemB2", pB.getId());

        Map<Integer, Integer> mapaA = desejoDao.contarDesejosPorGrupo(grupoId);
        Map<Integer, Integer> mapaB = desejoDao.contarDesejosPorGrupo(grupoId2);

        assertEquals(Integer.valueOf(1), mapaA.get(pA.getId()));
        assertNull(mapaA.get(pB.getId())); // pB não pertence ao grupoId
        assertEquals(Integer.valueOf(2), mapaB.get(pB.getId()));
        assertNull(mapaB.get(pA.getId())); // pA não pertence ao grupoId2
    }

    @Test
    public void contarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() {
        Map<Integer, Integer> mapa = desejoDao.contarDesejosPorGrupo(99999);
        assertNotNull(mapa);
        assertTrue(mapa.isEmpty());
    }

    // ===== listarDesejosPorGrupo =====

    @Test
    public void listarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() {
        inserirParticipante("Gabi", grupoId);
        Map<Integer, List<Desejo>> mapa = desejoDao.listarDesejosPorGrupo(grupoId);
        assertNotNull(mapa);
        assertTrue(mapa.isEmpty());
    }

    @Test
    public void listarDesejosPorGrupo_camposPopuladosCorretamente() {
        Participante p = inserirParticipante("Hugo", grupoId);
        Desejo d = new Desejo();
        d.setProduto("Fone");
        d.setCategoria("Audio");
        d.setLojas("Amazon");
        d.setPrecoMinimo(99.0);
        d.setPrecoMaximo(299.0);
        d.setParticipanteId(p.getId());
        desejoDao.inserir(d);

        Map<Integer, List<Desejo>> mapa = desejoDao.listarDesejosPorGrupo(grupoId);
        List<Desejo> desejos = mapa.get(p.getId());
        assertNotNull(desejos);
        assertEquals(1, desejos.size());

        Desejo found = desejos.get(0);
        assertEquals("Fone", found.getProduto());
        assertEquals("Audio", found.getCategoria());
        assertEquals("Amazon", found.getLojas());
        assertEquals(99.0, found.getPrecoMinimo(), 0.001);
        assertEquals(299.0, found.getPrecoMaximo(), 0.001);
        assertEquals(p.getId(), found.getParticipanteId());
    }

    @Test
    public void listarDesejosPorGrupo_variosParticipantes_listasSeparadas() {
        Participante p1 = inserirParticipante("Iris", grupoId);
        Participante p2 = inserirParticipante("João", grupoId);
        inserirDesejo("Livro A", p1.getId());
        inserirDesejo("Livro B", p1.getId());
        inserirDesejo("Game", p2.getId());

        Map<Integer, List<Desejo>> mapa = desejoDao.listarDesejosPorGrupo(grupoId);
        // a query não define ORDER BY — ordem dentro de cada lista é não-determinística;
        // testamos apenas contagem, não posição
        assertEquals(2, mapa.get(p1.getId()).size());
        assertEquals(1, mapa.get(p2.getId()).size());
    }

    @Test
    public void listarDesejosPorGrupo_naoContaminaOutroGrupo() {
        Participante pA = inserirParticipante("Karen", grupoId);
        Participante pB = inserirParticipante("Lucas", grupoId2);
        inserirDesejo("ItemA", pA.getId());
        inserirDesejo("ItemB", pB.getId());

        Map<Integer, List<Desejo>> mapaA = desejoDao.listarDesejosPorGrupo(grupoId);
        Map<Integer, List<Desejo>> mapaB = desejoDao.listarDesejosPorGrupo(grupoId2);

        assertTrue(mapaA.containsKey(pA.getId()));
        assertFalse(mapaA.containsKey(pB.getId()));
        assertTrue(mapaB.containsKey(pB.getId()));
        assertFalse(mapaB.containsKey(pA.getId()));
    }

    @Test
    public void listarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() {
        Map<Integer, List<Desejo>> mapa = desejoDao.listarDesejosPorGrupo(99999);
        assertNotNull(mapa);
        assertTrue(mapa.isEmpty());
    }

    @Test
    public void listarDesejosPorGrupo_camposOpcionaisNulos_naoLancaExcecao() {
        Participante p = inserirParticipante("Maria", grupoId);
        Desejo d = new Desejo();
        d.setProduto("Item sem extras");
        d.setCategoria(null);
        d.setLojas(null);
        d.setPrecoMinimo(0.0);
        d.setPrecoMaximo(0.0);
        d.setParticipanteId(p.getId());
        desejoDao.inserir(d);

        Map<Integer, List<Desejo>> mapa = desejoDao.listarDesejosPorGrupo(grupoId);
        List<Desejo> desejos = mapa.get(p.getId());
        assertNotNull(desejos);
        assertEquals(1, desejos.size());
        assertEquals("Item sem extras", desejos.get(0).getProduto());
    }
}