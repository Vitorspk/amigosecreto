package activity.amigosecreto.db;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

public class GrupoModelTest {

    @Test
    public void construtor_padrao_campos_sao_nulos_e_zero() {
        Grupo g = new Grupo();
        assertEquals(0, g.getId());
        assertNull(g.getNome());
        assertNull(g.getData());
    }

    @Test
    public void setNome_getNome_round_trip() {
        Grupo g = new Grupo();
        g.setNome("Familia");
        assertEquals("Familia", g.getNome());
    }

    @Test
    public void setId_getId_round_trip() {
        Grupo g = new Grupo();
        g.setId(42);
        assertEquals(42, g.getId());
    }

    @Test
    public void setData_getData_round_trip() {
        Grupo g = new Grupo();
        g.setData("25/12/2025");
        assertEquals("25/12/2025", g.getData());
    }

    @Test
    public void toString_retorna_nome() {
        Grupo g = new Grupo();
        g.setNome("Trabalho");
        assertEquals("Trabalho", g.toString());
    }

    @Test
    public void toString_com_nome_null_retorna_null() {
        Grupo g = new Grupo();
        assertNull(g.toString());
    }

    @Test
    public void serializable_round_trip_preserva_campos() throws Exception {
        Grupo original = new Grupo();
        original.setId(7);
        original.setNome("Amigos");
        original.setData("01/01/2025");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Grupo desserializado = (Grupo) ois.readObject();
        ois.close();

        assertEquals(original.getId(), desserializado.getId());
        assertEquals(original.getNome(), desserializado.getNome());
        assertEquals(original.getData(), desserializado.getData());
    }
}
