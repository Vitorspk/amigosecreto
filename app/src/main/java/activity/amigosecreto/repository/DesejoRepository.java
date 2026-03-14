package activity.amigosecreto.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;

/**
 * Repository que encapsula todo o acesso a DesejoDAO.
 *
 * Isola Activities e ViewModels do SQLite, facilitando testes (mock/substituto)
 * e evolução futura (ex: Room, sync com servidor).
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
public class DesejoRepository {

    private final DesejoDAO dao;

    public DesejoRepository(Context context) {
        this.dao = new DesejoDAO(context);
    }

    @VisibleForTesting
    DesejoRepository(DesejoDAO dao) {
        this.dao = dao;
    }

    public List<Desejo> listar() {
        dao.open();
        try {
            return dao.listar();
        } finally {
            dao.close();
        }
    }

    public List<Desejo> listarPorParticipante(int participanteId) {
        dao.open();
        try {
            return dao.listarPorParticipante(participanteId);
        } finally {
            dao.close();
        }
    }

    public int contarDesejosPorParticipante(int participanteId) {
        dao.open();
        try {
            return dao.contarDesejosPorParticipante(participanteId);
        } finally {
            dao.close();
        }
    }

    /**
     * Retorna contagens de desejos por participante para um grupo inteiro em uma única query.
     * Use no lugar de chamar contarDesejosPorParticipante() em loop (evita problema N+1).
     */
    public Map<Integer, Integer> contarDesejosPorGrupo(int grupoId) {
        dao.open();
        try {
            return dao.contarDesejosPorGrupo(grupoId);
        } finally {
            dao.close();
        }
    }

    /**
     * Retorna um mapa participante_id → lista de desejos para todos os participantes de um grupo,
     * usando uma única query (evita N open/close ao preparar mensagens para o grupo inteiro).
     */
    public Map<Integer, List<Desejo>> listarDesejosPorGrupo(int grupoId) {
        dao.open();
        try {
            return dao.listarDesejosPorGrupo(grupoId);
        } finally {
            dao.close();
        }
    }

    public void inserir(Desejo desejo) {
        dao.open();
        try {
            dao.inserir(desejo);
        } finally {
            dao.close();
        }
    }

    public void alterar(Desejo oldDesejo, Desejo newDesejo) {
        dao.open();
        try {
            dao.alterar(oldDesejo, newDesejo);
        } finally {
            dao.close();
        }
    }

    public void remover(Desejo desejo) {
        dao.open();
        try {
            dao.remover(desejo);
        } finally {
            dao.close();
        }
    }

    public Desejo buscarPorId(int id) {
        dao.open();
        try {
            return dao.buscarPorId(id);
        } finally {
            dao.close();
        }
    }
}
