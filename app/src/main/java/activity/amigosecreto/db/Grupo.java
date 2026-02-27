package activity.amigosecreto.db;

import java.io.Serializable;

public class Grupo implements Serializable {
    private int id;
    private String nome;
    private String data;

    public Grupo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    @Override
    public String toString() { return nome; }
}
