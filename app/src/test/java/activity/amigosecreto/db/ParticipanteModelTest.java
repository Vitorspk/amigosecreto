package activity.amigosecreto.db;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ParticipanteModelTest {

    @Test
    public void construtor_padrao_amigo_sorteado_e_null() {
        Participante p = new Participante();
        assertNull(p.getAmigoSorteadoId());
    }

    @Test
    public void construtor_padrao_ids_excluidos_e_lista_vazia() {
        Participante p = new Participante();
        assertNotNull(p.getIdsExcluidos());
        assertTrue(p.getIdsExcluidos().isEmpty());
    }

    @Test
    public void construtor_padrao_enviado_e_false() {
        Participante p = new Participante();
        assertFalse(p.isEnviado());
    }

    @Test
    public void setNome_getNome_round_trip() {
        Participante p = new Participante();
        p.setNome("Maria");
        assertEquals("Maria", p.getNome());
    }

    @Test
    public void setEmail_getEmail_round_trip() {
        Participante p = new Participante();
        p.setEmail("maria@email.com");
        assertEquals("maria@email.com", p.getEmail());
    }

    @Test
    public void setTelefone_getTelefone_round_trip() {
        Participante p = new Participante();
        p.setTelefone("(11) 91234-5678");
        assertEquals("(11) 91234-5678", p.getTelefone());
    }

    @Test
    public void setAmigoSorteadoId_getAmigoSorteadoId_round_trip() {
        Participante p = new Participante();
        p.setAmigoSorteadoId(99);
        assertEquals(Integer.valueOf(99), p.getAmigoSorteadoId());
    }

    @Test
    public void setEnviado_true_getEnviado_retorna_true() {
        Participante p = new Participante();
        p.setEnviado(true);
        assertTrue(p.isEnviado());
    }

    @Test
    public void setIdsExcluidos_getIdsExcluidos_round_trip() {
        Participante p = new Participante();
        p.setIdsExcluidos(Arrays.asList(1, 2, 3));
        assertEquals(3, p.getIdsExcluidos().size());
        assertTrue(p.getIdsExcluidos().contains(2));
    }

    @Test
    public void toString_retorna_nome() {
        Participante p = new Participante();
        p.setNome("Joao");
        assertEquals("Joao", p.toString());
    }

    @Test
    public void toString_com_nome_null_retorna_string_vazia() {
        Participante p = new Participante();
        assertEquals("", p.toString());
    }

    @Test
    public void serializable_round_trip_preserva_campos_principais() throws Exception {
        Participante original = new Participante();
        original.setId(10);
        original.setNome("Carlos");
        original.setEmail("carlos@teste.com");
        original.setTelefone("(21) 99999-8888");
        original.setEnviado(true);
        original.setAmigoSorteadoId(5);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Participante desserializado = (Participante) ois.readObject();
        ois.close();

        assertEquals(original.getId(), desserializado.getId());
        assertEquals(original.getNome(), desserializado.getNome());
        assertEquals(original.getEmail(), desserializado.getEmail());
        assertEquals(original.getTelefone(), desserializado.getTelefone());
        assertEquals(original.isEnviado(), desserializado.isEnviado());
        assertEquals(original.getAmigoSorteadoId(), desserializado.getAmigoSorteadoId());
    }
}
