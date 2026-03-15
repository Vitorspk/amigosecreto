package activity.amigosecreto.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ParticipanteDAOTest {

    private ParticipanteDAO dao;
    private GrupoDAO grupoDao;
    private int grupoId;
    private int grupoId2;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();
        dao = new ParticipanteDAO(ctx);
        dao.open();

        Grupo g1 = new Grupo();
        g1.setNome("Grupo Teste 1");
        g1.setData("01/01/2025");
        grupoId = (int) grupoDao.inserir(g1);

        Grupo g2 = new Grupo();
        g2.setNome("Grupo Teste 2");
        g2.setData("01/01/2025");
        grupoId2 = (int) grupoDao.inserir(g2);
    }

    @After
    public void tearDown() {
        grupoDao.limparTudo();
        dao.close();
        grupoDao.close();
    }

    private Participante criarParticipante(String nome, int gId) {
        Participante p = new Participante();
        p.setNome(nome);
        dao.inserir(p, gId);
        return p;
    }

    // --- inserir ---

    @Test
    public void inserir_gera_id_valido() {
        Participante p = new Participante();
        p.setNome("Ana");
        dao.inserir(p, grupoId);
        assertTrue(p.getId() > 0);
    }

    // --- listarPorGrupo ---

    @Test
    public void listarPorGrupo_retorna_apenas_participantes_do_grupo() {
        criarParticipante("Ana", grupoId);
        criarParticipante("Bruno", grupoId);
        criarParticipante("Carlos", grupoId2);

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        assertEquals(2, lista.size());
        for (Participante p : lista) {
            assertTrue(p.getNome().equals("Ana") || p.getNome().equals("Bruno"));
        }
    }

    @Test
    public void listarPorGrupo_retorna_lista_vazia_para_grupo_sem_participantes() {
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty());
    }

    @Test
    public void listarPorGrupo_ordenado_por_nome() {
        criarParticipante("Zelia", grupoId);
        criarParticipante("Ana", grupoId);
        criarParticipante("Mario", grupoId);

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        assertEquals("Ana", lista.get(0).getNome());
        assertEquals("Mario", lista.get(1).getNome());
        assertEquals("Zelia", lista.get(2).getNome());
    }

    // --- atualizar ---

    @Test
    public void atualizar_nome_persiste() {
        Participante p = criarParticipante("Nome Antigo", grupoId);
        p.setNome("Nome Novo");
        boolean ok = dao.atualizar(p);
        assertTrue(ok);

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        assertEquals("Nome Novo", lista.get(0).getNome());
    }

    @Test
    public void atualizar_id_invalido_retorna_false() {
        Participante p = new Participante();
        p.setId(0);
        p.setNome("Fantasma");
        assertFalse(dao.atualizar(p));
    }

    // --- remover ---

    @Test
    public void remover_apaga_participante() {
        Participante p = criarParticipante("Para Remover", grupoId);
        dao.remover(p.getId());
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty());
    }

    @Test
    public void remover_nao_afeta_outros_participantes() {
        Participante p1 = criarParticipante("Ana", grupoId);
        criarParticipante("Bruno", grupoId);
        dao.remover(p1.getId());

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        assertEquals(1, lista.size());
        assertEquals("Bruno", lista.get(0).getNome());
    }

    // --- exclusoes ---

    @Test
    public void adicionarExclusao_persiste() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);

        dao.adicionarExclusao(a.getId(), b.getId());

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        Participante aAtualizado = lista.stream().filter(p -> p.getId() == a.getId()).findFirst().orElse(null);
        assertNotNull(aAtualizado);
        assertTrue(aAtualizado.getIdsExcluidos().contains(b.getId()));
    }

    @Test
    public void adicionarExclusao_duplicada_nao_lanca_excecao() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);

        dao.adicionarExclusao(a.getId(), b.getId());
        // Segunda chamada deve ser ignorada (CONFLICT_IGNORE)
        dao.adicionarExclusao(a.getId(), b.getId());
    }

    @Test
    public void removerExclusao_remove_apenas_a_excluisao_especificada() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);
        Participante c = criarParticipante("C", grupoId);

        dao.adicionarExclusao(a.getId(), b.getId());
        dao.adicionarExclusao(a.getId(), c.getId());
        dao.removerExclusao(a.getId(), b.getId());

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        Participante aAtualizado = lista.stream().filter(p -> p.getId() == a.getId()).findFirst().orElse(null);
        assertNotNull(aAtualizado);
        assertFalse(aAtualizado.getIdsExcluidos().contains(b.getId()));
        assertTrue(aAtualizado.getIdsExcluidos().contains(c.getId()));
    }

    // --- salvarSorteio ---

    @Test
    public void salvarSorteio_persiste_amigo_sorteado_id() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);
        Participante c = criarParticipante("C", grupoId);

        List<Participante> participantes = Arrays.asList(a, b, c);
        List<Participante> sorteados = Arrays.asList(b, c, a);

        boolean ok = dao.salvarSorteio(participantes, sorteados);
        assertTrue(ok);

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        for (Participante p : lista) {
            assertNotNull(p.getAmigoSorteadoId());
        }
    }

    // --- marcarComoEnviado ---

    @Test
    public void marcarComoEnviado_atualiza_flag() {
        Participante p = criarParticipante("Ana", grupoId);
        assertFalse(p.isEnviado());

        dao.marcarComoEnviado(p.getId());

        Participante atualizado = dao.buscarPorId(p.getId());
        assertNotNull(atualizado);
        assertTrue(atualizado.isEnviado());
    }

    // --- contarPorGrupo ---

    @Test
    public void contarPorGrupo_retorna_mapa_correto() {
        criarParticipante("A", grupoId);
        criarParticipante("B", grupoId);
        criarParticipante("C", grupoId);
        criarParticipante("D", grupoId2);
        criarParticipante("E", grupoId2);

        Map<Integer, Integer> mapa = dao.contarPorGrupo();

        assertEquals(Integer.valueOf(3), mapa.get(grupoId));
        assertEquals(Integer.valueOf(2), mapa.get(grupoId2));
    }

    @Test
    public void contarPorGrupo_grupo_vazio_nao_esta_no_mapa() {
        Map<Integer, Integer> mapa = dao.contarPorGrupo();
        assertFalse(mapa.containsKey(grupoId));
    }

    // --- buscarPorId ---

    @Test
    public void buscarPorId_existente_retorna_participante() {
        Participante p = criarParticipante("Ana", grupoId);
        Participante encontrado = dao.buscarPorId(p.getId());
        assertNotNull(encontrado);
        assertEquals("Ana", encontrado.getNome());
        assertEquals(p.getId(), encontrado.getId());
    }

    @Test
    public void buscarPorId_inexistente_retorna_null() {
        assertNull(dao.buscarPorId(-1));
    }

    // --- limparSorteioDoGrupo ---

    @Test
    public void limparSorteioDoGrupo_nulifica_amigo_sorteado_e_reset_enviado() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);
        Participante c = criarParticipante("C", grupoId);

        dao.salvarSorteio(Arrays.asList(a, b, c), Arrays.asList(b, c, a));
        dao.marcarComoEnviado(a.getId());

        dao.limparSorteioDoGrupo(grupoId);

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        for (Participante p : lista) {
            assertNull(p.getAmigoSorteadoId());
            assertFalse(p.isEnviado());
        }
    }

    // --- deletarTodosDoGrupo ---

    @Test
    public void deletarTodosDoGrupo_remove_todos_participantes() {
        criarParticipante("A", grupoId);
        criarParticipante("B", grupoId);
        criarParticipante("C", grupoId2);

        dao.deletarTodosDoGrupo(grupoId);

        assertTrue(dao.listarPorGrupo(grupoId).isEmpty());
        assertEquals(1, dao.listarPorGrupo(grupoId2).size());
    }

    // --- getNomeAmigoSorteado ---

    @Test
    public void getNomeAmigoSorteado_retorna_nome_correto() {
        Participante a = criarParticipante("Ana", grupoId);
        Participante b = criarParticipante("Beatriz", grupoId);
        Participante c = criarParticipante("Carlos", grupoId);

        // Ana tira Beatriz
        dao.salvarSorteio(Arrays.asList(a, b, c), Arrays.asList(b, c, a));

        // getNomeAmigoSorteado recebe o id do amigo sorteado (nao do participante)
        List<Participante> lista = dao.listarPorGrupo(grupoId);
        Participante anaAtualizada = lista.stream().filter(p -> p.getId() == a.getId()).findFirst().orElse(null);
        assertNotNull(anaAtualizada);
        assertNotNull(anaAtualizada.getAmigoSorteadoId());

        String nome = dao.getNomeAmigoSorteado(anaAtualizada.getAmigoSorteadoId());
        assertEquals("Beatriz", nome);
    }

    @Test
    public void getNomeAmigoSorteado_id_inexistente_retorna_ninguem() {
        String nome = dao.getNomeAmigoSorteado(-1);
        assertEquals("Ninguém", nome);
    }

    // --- salvarSorteio tamanhos diferentes ---

    @Test
    public void salvarSorteio_listas_tamanhos_diferentes_retorna_false() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);
        Participante c = criarParticipante("C", grupoId);

        // sorteados tem um elemento a menos — IndexOutOfBoundsException esperado
        boolean ok = dao.salvarSorteio(Arrays.asList(a, b, c), Arrays.asList(b, c));
        assertFalse(ok);

        // banco não deve ter sido alterado (transação revertida)
        List<Participante> lista = dao.listarPorGrupo(grupoId);
        for (Participante p : lista) {
            assertNull(p.getAmigoSorteadoId());
        }
    }

    // --- adicionarExclusao idempotência ---

    @Test
    public void adicionarExclusao_duplicada_nao_insere_registro_extra() {
        Participante a = criarParticipante("A", grupoId);
        Participante b = criarParticipante("B", grupoId);

        dao.adicionarExclusao(a.getId(), b.getId());
        dao.adicionarExclusao(a.getId(), b.getId()); // duplicate — CONFLICT_IGNORE

        List<Participante> lista = dao.listarPorGrupo(grupoId);
        Participante aAtualizado = lista.stream().filter(p -> p.getId() == a.getId()).findFirst().orElse(null);
        assertNotNull(aAtualizado);
        // exactly one entry, not two
        assertEquals(1, aAtualizado.getIdsExcluidos().size());
        assertTrue(aAtualizado.getIdsExcluidos().contains(b.getId()));
    }

    // --- deletarTodosDoGrupo grupo vazio ---

    @Test
    public void deletarTodosDoGrupo_grupo_vazio_nao_lanca_excecao() {
        // grupo sem participantes — moveToFirst() retorna false, nenhuma deleção ocorre
        dao.deletarTodosDoGrupo(grupoId);
        assertTrue(dao.listarPorGrupo(grupoId).isEmpty());
    }
}
