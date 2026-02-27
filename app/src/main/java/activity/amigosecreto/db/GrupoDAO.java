package activity.amigosecreto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class GrupoDAO {
    private MySQLiteOpenHelper helper;
    private SQLiteDatabase database;

    public GrupoDAO(Context ctx) {
        helper = new MySQLiteOpenHelper(ctx);
    }

    public void open() throws SQLException {
        database = helper.getWritableDatabase();
    }

    public void close() {
        helper.close();
    }

    public long inserir(Grupo g) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COLUMN_GRUPO_NOME, g.getNome());
        values.put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA, g.getData());
        return database.insert(MySQLiteOpenHelper.TABLE_GRUPO, null, values);
    }

    public void remover(int id) {
        // First, get all participant IDs to clean up their exclusion records
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_PARTICIPANTE,
                new String[]{MySQLiteOpenHelper.COLUMN_ID},
                MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int participantId = cursor.getInt(0);
                String participantIdStr = String.valueOf(participantId);
                // Remove all exclusion records involving this participant
                database.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO,
                        MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID + " = ? OR " + MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID + " = ?",
                        new String[]{participantIdStr, participantIdStr});
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Then remove participants and the group
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID + " = ?", new String[]{String.valueOf(id)});
        database.delete(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Grupo> listar() {
        List<Grupo> lista = new ArrayList<>();
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_GRUPO, null, null, null, null, null, MySQLiteOpenHelper.COLUMN_GRUPO_ID + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                Grupo g = new Grupo();
                g.setId(cursor.getInt(0));
                g.setNome(cursor.getString(1));
                g.setData(cursor.getString(2));
                lista.add(g);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }
}
