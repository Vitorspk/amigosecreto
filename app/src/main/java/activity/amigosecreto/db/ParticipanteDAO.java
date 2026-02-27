package activity.amigosecreto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class ParticipanteDAO {
    private MySQLiteOpenHelper helper;
    private SQLiteDatabase database;

    public ParticipanteDAO(Context ctx) {
        helper = new MySQLiteOpenHelper(ctx);
    }

    public void open() throws SQLException {
        database = helper.getWritableDatabase();
    }

    public void close() {
        helper.close();
    }

    public void inserir(Participante p, int grupoId) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COLUMN_NOME, p.getNome());
        values.put(MySQLiteOpenHelper.COLUMN_EMAIL, p.getEmail());
        values.put(MySQLiteOpenHelper.COLUMN_TELEFONE, p.getTelefone());
        values.put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0);
        values.put(MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID, grupoId);
        long id = database.insert(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, values);
        p.setId((int) id);
    }

    public void remover(int id) {
        String idStr = String.valueOf(id);
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_ID + " = ?", new String[]{idStr});
        database.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO, MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID + " = ? OR " + MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID + " = ?", new String[]{idStr, idStr});
    }

    public void deletarTodosDoGrupo(int grupoId) {
        // Obter todos os IDs dos participantes do grupo para remover as exclusões também
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_PARTICIPANTE, new String[]{MySQLiteOpenHelper.COLUMN_ID},
                MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?", new String[]{String.valueOf(grupoId)}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String idStr = String.valueOf(id);
                database.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO, MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID + " = ? OR " + MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID + " = ?", new String[]{idStr, idStr});
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?", new String[]{String.valueOf(grupoId)});
    }

    public void limparSorteioDoGrupo(int grupoId) {
        ContentValues values = new ContentValues();
        values.putNull(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID);
        values.put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0);
        database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values, MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?", new String[]{String.valueOf(grupoId)});
    }

    public void adicionarExclusao(int idParticipante, int idExcluido) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID, idParticipante);
        values.put(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID, idExcluido);
        database.insertWithOnConflict(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removerExclusao(int idParticipante, int idExcluido) {
        database.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO, 
                MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID + " = ? AND " + MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID + " = ?",
                new String[]{String.valueOf(idParticipante), String.valueOf(idExcluido)});
    }

    public boolean salvarSorteio(List<Participante> participantes, List<Participante> sorteados) {
        database.beginTransaction();
        try {
            for (int i = 0; i < participantes.size(); i++) {
                ContentValues values = new ContentValues();
                values.put(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID, sorteados.get(i).getId());
                values.put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0);
                database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values,
                        MySQLiteOpenHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(participantes.get(i).getId())});
            }
            database.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public void marcarComoEnviado(int id) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COLUMN_ENVIADO, 1);
        database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values, MySQLiteOpenHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Participante> listarPorGrupo(int grupoId) {
        List<Participante> lista = new ArrayList<>();
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, 
                MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?", new String[]{String.valueOf(grupoId)}, null, null, MySQLiteOpenHelper.COLUMN_NOME);
        
        if (cursor.moveToFirst()) {
            do {
                Participante p = new Participante();
                p.setId(cursor.getInt(0));
                p.setNome(cursor.getString(1));
                p.setEmail(cursor.getString(2));
                p.setTelefone(cursor.getString(3));
                if (!cursor.isNull(4)) {
                    p.setAmigoSorteadoId(cursor.getInt(4));
                }
                p.setEnviado(cursor.getInt(5) == 1);
                
                // Carregar exclusões
                p.setIdsExcluidos(listarExclusoes(p.getId()));
                
                lista.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    private List<Integer> listarExclusoes(int idParticipante) {
        List<Integer> exclusoes = new ArrayList<>();
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_EXCLUSAO, 
                new String[]{MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID}, 
                MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID + " = ?", 
                new String[]{String.valueOf(idParticipante)}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                exclusoes.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return exclusoes;
    }

    public String getNomeAmigoSorteado(int amigoId) {
        String nome = "Ninguém";
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_PARTICIPANTE,
                new String[]{MySQLiteOpenHelper.COLUMN_NOME},
                MySQLiteOpenHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(amigoId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            nome = cursor.getString(0);
        }
        cursor.close();
        return nome;
    }

    public Participante buscarPorId(int id) {
        Participante p = null;
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null,
                MySQLiteOpenHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            p = new Participante();
            p.setId(cursor.getInt(0));
            p.setNome(cursor.getString(1));
            p.setEmail(cursor.getString(2));
            p.setTelefone(cursor.getString(3));
            if (!cursor.isNull(4)) {
                p.setAmigoSorteadoId(cursor.getInt(4));
            }
            p.setEnviado(cursor.getInt(5) == 1);
        }
        cursor.close();
        return p;
    }
}
