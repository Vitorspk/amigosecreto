package activity.amigosecreto.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import activity.amigosecreto.db.Participante;

import static org.junit.Assert.*;

public class SorteioEngineTest {

    private Participante criar(int id, String nome, Integer... excluidos) {
        Participante p = new Participante();
        p.setId(id);
        p.setNome(nome);
        p.setIdsExcluidos(new ArrayList<>(Arrays.asList(excluidos)));
        return p;
    }

    private List<Participante> criarGrupo(int quantidade) {
        List<Participante> lista = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) lista.add(criar(i, "P" + i));
        return lista;
    }

    /** Tenta ate 200 vezes ate obter um resultado nao-null. Para grupos sem exclusoes
     *  extremas isso sempre converge. Lancar AssertionError se nunca convergir. */
    private List<Participante> sortearComRetry(List<Participante> participantes) {
        for (int i = 0; i < 200; i++) {
            List<Participante> r = SorteioEngine.tentarSorteio(participantes);
            if (r != null) return r;
        }
        fail("SorteioEngine retornou null em 200 tentativas para grupo sem exclusoes bloqueantes");
        return null;
    }

    // --- Tamanho do resultado ---

    @Test
    public void sorteio_retorna_lista_de_mesmo_tamanho() {
        List<Participante> participantes = criarGrupo(4);
        List<Participante> resultado = sortearComRetry(participantes);
        assertEquals(4, resultado.size());
    }

    @Test
    public void sorteio_minimo_3_participantes_funciona() {
        List<Participante> participantes = criarGrupo(3);
        List<Participante> resultado = sortearComRetry(participantes);
        assertEquals(3, resultado.size());
    }

    @Test
    public void sorteio_grande_grupo_funciona() {
        List<Participante> participantes = criarGrupo(20);
        List<Participante> resultado = sortearComRetry(participantes);
        assertEquals(20, resultado.size());
    }

    // --- Unicidade ---

    @Test
    public void sorteio_cada_participante_aparece_exatamente_uma_vez() {
        List<Participante> participantes = criarGrupo(5);
        List<Participante> resultado = sortearComRetry(participantes);
        Set<Integer> ids = new HashSet<>();
        for (Participante p : resultado) {
            assertTrue("ID duplicado no resultado: " + p.getId(), ids.add(p.getId()));
        }
        assertEquals(5, ids.size());
    }

    // --- Ninguem tira a si mesmo ---

    @Test
    public void sorteio_ninguem_tira_a_si_mesmo() {
        List<Participante> participantes = criarGrupo(5);
        // Validar em varios sorteios bem-sucedidos
        int validados = 0;
        for (int t = 0; t < 200 && validados < 20; t++) {
            List<Participante> resultado = SorteioEngine.tentarSorteio(participantes);
            if (resultado == null) continue;
            validados++;
            for (int i = 0; i < participantes.size(); i++) {
                assertNotEquals(
                    participantes.get(i).getNome() + " tirou a si mesmo",
                    participantes.get(i).getId(),
                    resultado.get(i).getId()
                );
            }
        }
        assertTrue("Nao obteve sorteios suficientes para validar", validados >= 5);
    }

    // --- Exclusoes ---

    @Test
    public void sorteio_respeita_exclusao_simples() {
        // A excluiu B — nos resultados validos, A nunca tira B
        Participante a = criar(1, "A", 2);
        Participante b = criar(2, "B");
        Participante c = criar(3, "C");
        List<Participante> participantes = Arrays.asList(a, b, c);

        int validados = 0;
        for (int t = 0; t < 100 && validados < 10; t++) {
            List<Participante> resultado = SorteioEngine.tentarSorteio(participantes);
            if (resultado == null) continue;
            validados++;
            assertNotEquals("A tirou B em violacao da exclusao", 2, resultado.get(0).getId());
        }
        assertTrue("Nao obteve sorteios suficientes para validar exclusao", validados >= 5);
    }

    @Test
    public void sorteio_retorna_null_se_impossivel() {
        Participante a = criar(1, "A", 2);
        Participante b = criar(2, "B", 1);
        assertNull(SorteioEngine.tentarSorteio(Arrays.asList(a, b)));
    }

    @Test
    public void sorteio_retorna_null_com_lista_de_um_participante() {
        assertNull(SorteioEngine.tentarSorteio(criarGrupo(1)));
    }

    @Test
    public void sorteio_com_exclusao_total_retorna_null() {
        Participante a = criar(1, "A", 2, 3);
        Participante b = criar(2, "B", 1, 3);
        Participante c = criar(3, "C", 1, 2);
        assertNull(SorteioEngine.tentarSorteio(Arrays.asList(a, b, c)));
    }

    // --- Imutabilidade da entrada ---

    @Test
    public void sorteio_lista_entrada_nao_e_modificada() {
        List<Participante> participantes = criarGrupo(4);
        List<Integer> idsBefore = new ArrayList<>();
        for (Participante p : participantes) idsBefore.add(p.getId());
        SorteioEngine.tentarSorteio(participantes);
        assertEquals(idsBefore.size(), participantes.size());
        for (int i = 0; i < participantes.size(); i++) {
            assertEquals(idsBefore.get(i).intValue(), participantes.get(i).getId());
        }
    }
}
