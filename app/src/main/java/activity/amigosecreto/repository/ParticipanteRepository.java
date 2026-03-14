package activity.amigosecreto.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;

/**
 * Repository que encapsula todo o acesso a ParticipanteDAO.
 *
 * Isola Activities e ViewModels do SQLite, facilitando testes (mock/substituto)
 * e evolução futura (ex: Room, sync com servidor).
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
public class ParticipanteRepository {

    private final ParticipanteDAO dao;

    public ParticipanteRepository(Context context) {
        this.dao = new ParticipanteDAO(context);
    }

    @VisibleForTesting
    ParticipanteRepository(ParticipanteDAO dao) {
        this.dao = dao;
    }

    public List<Participante> listarPorGrupo(int grupoId) {
        dao.open();
        try {
            return dao.listarPorGrupo(grupoId);
        } finally {
            dao.close();
        }
    }

    public void inserir(Participante participante, int grupoId) {
        dao.open();
        try {
            dao.inserir(participante, grupoId);
        } finally {
            dao.close();
        }
    }

    public boolean atualizar(Participante participante) {
        dao.open();
        try {
            return dao.atualizar(participante);
        } finally {
            dao.close();
        }
    }

    public void remover(int id) {
        dao.open();
        try {
            dao.remover(id);
        } finally {
            dao.close();
        }
    }

    public void deletarTodosDoGrupo(int grupoId) {
        dao.open();
        try {
            dao.deletarTodosDoGrupo(grupoId);
        } finally {
            dao.close();
        }
    }

    public void limparSorteioDoGrupo(int grupoId) {
        dao.open();
        try {
            dao.limparSorteioDoGrupo(grupoId);
        } finally {
            dao.close();
        }
    }

    public void adicionarExclusao(int idParticipante, int idExcluido) {
        dao.open();
        try {
            dao.adicionarExclusao(idParticipante, idExcluido);
        } finally {
            dao.close();
        }
    }

    public void removerExclusao(int idParticipante, int idExcluido) {
        dao.open();
        try {
            dao.removerExclusao(idParticipante, idExcluido);
        } finally {
            dao.close();
        }
    }

    /**
     * Aplica todas as alterações de exclusão em uma única transação atômica (evita falha parcial
     * e múltiplos open/close que ocorreriam num loop de chamadas individuais).
     */
    public void salvarExclusoes(int participanteId, List<Integer> adicionar, List<Integer> remover) {
        dao.open();
        try {
            dao.salvarExclusoes(participanteId, adicionar, remover);
        } finally {
            dao.close();
        }
    }

    public boolean salvarSorteio(List<Participante> participantes, List<Participante> sorteados) {
        dao.open();
        try {
            return dao.salvarSorteio(participantes, sorteados);
        } finally {
            dao.close();
        }
    }

    public void marcarComoEnviado(int id) {
        dao.open();
        try {
            dao.marcarComoEnviado(id);
        } finally {
            dao.close();
        }
    }

    public String getNomeAmigoSorteado(int amigoId) {
        dao.open();
        try {
            return dao.getNomeAmigoSorteado(amigoId);
        } finally {
            dao.close();
        }
    }

    public Map<Integer, Integer> contarPorGrupo() {
        dao.open();
        try {
            return dao.contarPorGrupo();
        } finally {
            dao.close();
        }
    }

    public Participante buscarPorId(int id) {
        dao.open();
        try {
            return dao.buscarPorId(id);
        } finally {
            dao.close();
        }
    }
}
