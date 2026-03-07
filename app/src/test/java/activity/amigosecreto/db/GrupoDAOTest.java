package activity.amigosecreto.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class GrupoDAOTest {

    private GrupoDAO dao;
    private ParticipanteDAO participanteDao;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        dao = new GrupoDAO(ctx);
        dao.open();
        participanteDao = new ParticipanteDAO(ctx);
        participanteDao.open();
    }

    @After
    public void tearDown() {
        dao.close();
        participanteDao.close();
    }

    private Grupo criarGrupo(String nome) {
        Grupo g = new Grupo();
        g.setNome(nome);
        g.setData(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        long id = dao.inserir(g);
        g.setId((int) id);
        return g;
    }

    // --- inserir ---

    @Test
    public void inserir_retorna_id_valido() {
        Grupo g = new Grupo();
        g.setNome("Familia");
        g.setData("01/01/2025");
        long id = dao.inserir(g);
        assertTrue(id > 0);
    }

    // --- listar ---

    @Test
    public void listar_retorna_lista_vazia_quando_sem_dados() {
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    public void listar_retorna_grupo_inserido() {
        criarGrupo("Trabalho");
        List<Grupo> lista = dao.listar();
        assertEquals(1, lista.size());
        assertEquals("Trabalho", lista.get(0).getNome());
    }

    @Test
    public void listar_ordem_desc_por_id() {
        criarGrupo("Primeiro");
        criarGrupo("Segundo");
        criarGrupo("Terceiro");
        List<Grupo> lista = dao.listar();
        assertEquals(3, lista.size());
        // Mais recente deve vir primeiro (ORDER BY id DESC)
        assertEquals("Terceiro", lista.get(0).getNome());
        assertEquals("Primeiro", lista.get(2).getNome());
    }

    // --- atualizarNome ---

    @Test
    public void atualizarNome_retorna_1_row_afetada() {
        Grupo g = criarGrupo("Nome Antigo");
        g.setNome("Nome Novo");
        int rows = dao.atualizarNome(g);
        assertEquals(1, rows);
    }

    @Test
    public void atualizarNome_persiste_novo_nome() {
        Grupo g = criarGrupo("Nome Antigo");
        String dataOriginal = g.getData();
        g.setNome("Nome Novo");
        dao.atualizarNome(g);

        List<Grupo> lista = dao.listar();
        assertEquals(1, lista.size());
        assertEquals("Nome Novo", lista.get(0).getNome());
        // Data nao deve ser alterada por atualizarNome
        assertEquals(dataOriginal, lista.get(0).getData());
    }

    @Test
    public void atualizarNome_id_inexistente_retorna_0() {
        Grupo g = new Grupo();
        g.setId(999);
        g.setNome("Fantasma");
        int rows = dao.atualizarNome(g);
        assertEquals(0, rows);
    }

    // --- remover ---

    @Test
    public void remover_apaga_grupo() {
        Grupo g = criarGrupo("Para Remover");
        dao.remover(g.getId());
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    public void remover_apaga_participantes_do_grupo() {
        Grupo g = criarGrupo("Grupo Com Participantes");
        Participante p = new Participante();
        p.setNome("Membro");
        participanteDao.inserir(p, g.getId());

        dao.remover(g.getId());

        List<Participante> participantes = participanteDao.listarPorGrupo(g.getId());
        assertTrue(participantes.isEmpty());
    }

    @Test
    public void remover_nao_afeta_outros_grupos() {
        Grupo g1 = criarGrupo("Grupo 1");
        Grupo g2 = criarGrupo("Grupo 2");
        dao.remover(g1.getId());

        List<Grupo> lista = dao.listar();
        assertEquals(1, lista.size());
        assertEquals("Grupo 2", lista.get(0).getNome());
    }

    // --- limparTudo ---

    @Test
    public void limparTudo_remove_todos_os_grupos() {
        criarGrupo("G1");
        criarGrupo("G2");
        dao.limparTudo();
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    public void limparTudo_remove_participantes_tambem() {
        Grupo g = criarGrupo("Grupo");
        Participante p = new Participante();
        p.setNome("Pessoa");
        participanteDao.inserir(p, g.getId());

        dao.limparTudo();

        assertTrue(participanteDao.listarPorGrupo(g.getId()).isEmpty());
    }
}
