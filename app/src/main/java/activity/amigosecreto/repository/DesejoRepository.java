package activity.amigosecreto.repository;

import android.content.Context;

import java.util.List;

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

    // Construtor para testes — permite injetar um DAO substituível.
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
