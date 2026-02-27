package activity.amigosecreto.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Participante implements Serializable {
    private int id;
    private String nome;
    private String email;
    private String telefone;
    private Integer amigoSorteadoId;
    private boolean enviado;
    private String codigoAcesso;
    private List<Integer> idsExcluidos = new ArrayList<>();

    public Participante() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public Integer getAmigoSorteadoId() { return amigoSorteadoId; }
    public void setAmigoSorteadoId(Integer amigoSorteadoId) { this.amigoSorteadoId = amigoSorteadoId; }

    public boolean isEnviado() { return enviado; }
    public void setEnviado(boolean enviado) { this.enviado = enviado; }

    public String getCodigoAcesso() { return codigoAcesso; }
    public void setCodigoAcesso(String codigoAcesso) { this.codigoAcesso = codigoAcesso; }

    public List<Integer> getIdsExcluidos() { return idsExcluidos; }
    public void setIdsExcluidos(List<Integer> idsExcluidos) { this.idsExcluidos = idsExcluidos; }

    @Override
    public String toString() { return nome; }
}
