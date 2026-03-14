package activity.amigosecreto.db;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Testes de comportamento do Participante relevantes para migração para Kotlin.
 *
 * Documenta contratos que devem ser preservados:
 *
 * 1. Semântica de igualdade — Participante NÃO implementa equals/hashCode,
 *    portanto usa igualdade por referência. Se a migração para Kotlin usar
 *    data class, a semântica muda (estrutural). Esses testes documentam o
 *    comportamento ATUAL para que a decisão de manter ou mudar seja explícita.
 *
 * 2. Mutabilidade de idsExcluidos — o campo é ArrayList mutável. Código que
 *    chama .add()/.remove() diretamente depende disso. Kotlin data class com
 *    List<Int> imutável quebraria esses call sites.
 *
 * 3. amigoSorteadoId nullable — Integer? em Kotlin, deve aceitar null sem NPE.
 */
public class ParticipanteKotlinMigrationTest {

    // ===== Semântica de igualdade atual (referência) =====

    @Test
    public void equals_mesmaInstancia_retornaTrue() {
        Participante p = new Participante();
        p.setNome("Ana");
        assertEquals(p, p); // identidade de referência
    }

    @Test
    public void equals_instanciasDiferentes_mesmosValores_retornaFalse() {
        // Comportamento ATUAL: sem equals() → usa Object.equals() → referência
        // Ao migrar para Kotlin data class, isso mudará para TRUE (estrutural)
        // Este teste documenta o contrato atual — a mudança deve ser deliberada
        Participante a = new Participante();
        a.setId(1);
        a.setNome("Bruno");

        Participante b = new Participante();
        b.setId(1);
        b.setNome("Bruno");

        assertNotEquals("Participante usa igualdade por referência (sem equals override)", a, b);
    }

    @Test
    public void hashCode_instanciasDiferentes_mesmosValores_hashsDiferentes() {
        // Mesmo raciocínio: hashCode() atual é o da identidade de objeto
        Participante a = new Participante();
        a.setId(1);
        a.setNome("Carla");

        Participante b = new Participante();
        b.setId(1);
        b.setNome("Carla");

        assertNotEquals("hashCode atual é baseado em identidade", a.hashCode(), b.hashCode());
    }

    // ===== Mutabilidade de idsExcluidos =====

    @Test
    public void idsExcluidos_mutavel_addFunciona() {
        Participante p = new Participante();
        p.getIdsExcluidos().add(10);
        p.getIdsExcluidos().add(20);
        assertEquals(2, p.getIdsExcluidos().size());
        assertTrue(p.getIdsExcluidos().contains(10));
        assertTrue(p.getIdsExcluidos().contains(20));
    }

    @Test
    public void idsExcluidos_mutavel_removeFunciona() {
        Participante p = new Participante();
        p.getIdsExcluidos().add(5);
        p.getIdsExcluidos().add(6);
        p.getIdsExcluidos().remove(Integer.valueOf(5));
        assertEquals(1, p.getIdsExcluidos().size());
        assertFalse(p.getIdsExcluidos().contains(5));
    }

    @Test
    public void idsExcluidos_setListaMutavel_continuaMutavel() {
        Participante p = new Participante();
        List<Integer> nova = new ArrayList<>();
        nova.add(99);
        p.setIdsExcluidos(nova);

        // deve ser possível adicionar após set
        p.getIdsExcluidos().add(100);
        assertEquals(2, p.getIdsExcluidos().size());
    }

    // ===== amigoSorteadoId nullable =====

    @Test
    public void amigoSorteadoId_defaultNull_semNPE() {
        Participante p = new Participante();
        assertNull(p.getAmigoSorteadoId());
        // acesso seguro — não deve lançar NPE
        boolean temAmigo = p.getAmigoSorteadoId() != null;
        assertFalse(temAmigo);
    }

    @Test
    public void amigoSorteadoId_setNull_getNull() {
        Participante p = new Participante();
        p.setAmigoSorteadoId(42);
        assertEquals(Integer.valueOf(42), p.getAmigoSorteadoId());

        p.setAmigoSorteadoId(null);
        assertNull(p.getAmigoSorteadoId());
    }

    @Test
    public void amigoSorteadoId_unboxingNulo_naoDeveSerFeitoSemVerificacao() {
        // Documenta que o padrão seguro é checar != null antes de unboxar.
        // Em Kotlin, amigoSorteadoId deve ser Int? (nullable) — usar ?.let ou ?: default
        Participante p = new Participante();
        Integer val = p.getAmigoSorteadoId();
        // padrão seguro: verificar antes de usar
        int id = (val != null) ? val : -1;
        assertEquals(-1, id);
    }

    // ===== enviado boolean padrão =====

    @Test
    public void enviado_default_false() {
        Participante p = new Participante();
        assertFalse(p.isEnviado());
    }

    @Test
    public void enviado_setTrue_getTrue() {
        Participante p = new Participante();
        p.setEnviado(true);
        assertTrue(p.isEnviado());
    }

    @Test
    public void enviado_setFalse_getFalse() {
        Participante p = new Participante();
        p.setEnviado(true);
        p.setEnviado(false);
        assertFalse(p.isEnviado());
    }
}