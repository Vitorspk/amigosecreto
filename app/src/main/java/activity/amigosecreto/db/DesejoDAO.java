package activity.amigosecreto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by HP on 21/06/2015.
 */
public class DesejoDAO {
    private MySQLiteOpenHelper helper;
    private SQLiteDatabase database;

    public DesejoDAO(Context ctx) {
        helper = new MySQLiteOpenHelper(ctx);
    }

    public void open() throws SQLException {
        database = helper.getWritableDatabase();
    }

    public void close(){
        helper.close();
    }

    public final void alterar(Desejo old_desejo, Desejo new_desejo){
        ContentValues values = new ContentValues();
        // Don't put COLUMN_ID - it's the WHERE clause, not an update target
        values.put(helper.COLUMN_PRODUTO, new_desejo.getProduto());
        values.put(helper.COLUMN_CATEGORIA, new_desejo.getCategoria());
        values.put(helper.COLUMN_PRECO_MINIMO, new_desejo.getPrecoMinimo());
        values.put(helper.COLUMN_PRECO_MAXIMO, new_desejo.getPrecoMaximo());
        values.put(helper.COLUMN_LOJAS, new_desejo.getLojas());
        values.put(helper.COLUMN_DESEJO_PARTICIPANTE_ID, new_desejo.getParticipanteId());
        database.update(helper.TABLE_DESEJO, values, helper.COLUMN_ID + " = ?", new String[]{String.valueOf(old_desejo.getId())});
    }

    public final void inserir(Desejo desejo){
        ContentValues values = new ContentValues();
        // Don't put COLUMN_ID - let AUTOINCREMENT handle it
        values.put(helper.COLUMN_PRODUTO, desejo.getProduto());
        values.put(helper.COLUMN_CATEGORIA, desejo.getCategoria());
        values.put(helper.COLUMN_PRECO_MINIMO, desejo.getPrecoMinimo());
        values.put(helper.COLUMN_PRECO_MAXIMO, desejo.getPrecoMaximo());
        values.put(helper.COLUMN_LOJAS, desejo.getLojas());
        values.put(helper.COLUMN_DESEJO_PARTICIPANTE_ID, desejo.getParticipanteId());
        long id = database.insert(helper.TABLE_DESEJO, null, values);
        // Only set ID if insert succeeded (returns -1 on failure)
        if (id != -1) {
            desejo.setId((int) id);
        }
    }

    public final void remover(Desejo desejo){
        database.delete(helper.TABLE_DESEJO, helper.COLUMN_ID + " = ?", new String[]{String.valueOf(desejo.getId())});
    }

    public final List<Desejo> listar(){
        ArrayList<Desejo> lista = new ArrayList<Desejo>();
        Cursor cursor = database.rawQuery("select * from "+helper.TABLE_DESEJO, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            Desejo d = new Desejo();
            d.setId(cursor.getInt(0));
            d.setProduto(cursor.getString(1));
            d.setCategoria(cursor.getString(2));
            d.setPrecoMinimo(cursor.getDouble(3));
            d.setPrecoMaximo(cursor.getDouble(4));
            d.setLojas(cursor.getString(5));
            if (cursor.getColumnCount() > 6) {
                d.setParticipanteId(cursor.getInt(6));
            }
            lista.add(d);
            cursor.moveToNext();
        }
        cursor.close();
        return lista;
    }

    public final List<Desejo> listarPorParticipante(int participanteId){
        ArrayList<Desejo> lista = new ArrayList<Desejo>();
        Cursor cursor = database.rawQuery("select * from "+helper.TABLE_DESEJO+" where "+helper.COLUMN_DESEJO_PARTICIPANTE_ID+" = ?", new String[]{String.valueOf(participanteId)});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            Desejo d = new Desejo();
            d.setId(cursor.getInt(0));
            d.setProduto(cursor.getString(1));
            d.setCategoria(cursor.getString(2));
            d.setPrecoMinimo(cursor.getDouble(3));
            d.setPrecoMaximo(cursor.getDouble(4));
            d.setLojas(cursor.getString(5));
            d.setParticipanteId(cursor.getInt(6));
            lista.add(d);
            cursor.moveToNext();
        }
        cursor.close();
        return lista;
    }

    public final int contarDesejosPorParticipante(int participanteId){
        Cursor cursor = database.rawQuery("select count(*) from "+helper.TABLE_DESEJO+" where "+helper.COLUMN_DESEJO_PARTICIPANTE_ID+" = ?", new String[]{String.valueOf(participanteId)});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * Retorna um mapa participante_id → quantidade de desejos para todos os participantes
     * de um grupo, usando uma única query com GROUP BY. Evita o problema N+1 de chamar
     * contarDesejosPorParticipante() individualmente para cada participante.
     */
    public final Map<Integer, Integer> contarDesejosPorGrupo(int grupoId) {
        Map<Integer, Integer> mapa = new HashMap<>();
        // JOIN com participante para filtrar pelo grupo sem subquery complexa.
        String sql = "SELECT d." + helper.COLUMN_DESEJO_PARTICIPANTE_ID + ", COUNT(*) AS cnt"
                + " FROM " + helper.TABLE_DESEJO + " d"
                + " INNER JOIN " + helper.TABLE_PARTICIPANTE + " p"
                + " ON d." + helper.COLUMN_DESEJO_PARTICIPANTE_ID + " = p." + helper.COLUMN_ID
                + " WHERE p." + helper.COLUMN_FK_GRUPO_ID + " = ?"
                + " GROUP BY d." + helper.COLUMN_DESEJO_PARTICIPANTE_ID;
        Cursor cursor = database.rawQuery(sql, new String[]{String.valueOf(grupoId)});
        try {
            if (cursor.moveToFirst()) {
                int pidIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_DESEJO_PARTICIPANTE_ID);
                int cntIdx = cursor.getColumnIndexOrThrow("cnt");
                do {
                    mapa.put(cursor.getInt(pidIdx), cursor.getInt(cntIdx));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return mapa;
    }

    /**
     * Retorna um mapa participante_id → lista de desejos para todos os participantes de um grupo,
     * usando uma única query com JOIN. Evita N open/close ao preparar mensagens para o grupo inteiro.
     */
    public final Map<Integer, List<Desejo>> listarDesejosPorGrupo(int grupoId) {
        Map<Integer, List<Desejo>> mapa = new HashMap<>();
        String sql = "SELECT d." + helper.COLUMN_ID
                + ", d." + helper.COLUMN_PRODUTO
                + ", d." + helper.COLUMN_CATEGORIA
                + ", d." + helper.COLUMN_PRECO_MINIMO
                + ", d." + helper.COLUMN_PRECO_MAXIMO
                + ", d." + helper.COLUMN_LOJAS
                + ", d." + helper.COLUMN_DESEJO_PARTICIPANTE_ID
                + " FROM " + helper.TABLE_DESEJO + " d"
                + " INNER JOIN " + helper.TABLE_PARTICIPANTE + " p"
                + " ON d." + helper.COLUMN_DESEJO_PARTICIPANTE_ID + " = p." + helper.COLUMN_ID
                + " WHERE p." + helper.COLUMN_FK_GRUPO_ID + " = ?";
        Cursor cursor = database.rawQuery(sql, new String[]{String.valueOf(grupoId)});
        try {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_ID);
                int prodIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_PRODUTO);
                int catIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_CATEGORIA);
                int minIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_PRECO_MINIMO);
                int maxIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_PRECO_MAXIMO);
                int lojasIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_LOJAS);
                int pidIdx = cursor.getColumnIndexOrThrow(helper.COLUMN_DESEJO_PARTICIPANTE_ID);
                do {
                    Desejo d = new Desejo();
                    d.setId(cursor.getInt(idIdx));
                    d.setProduto(cursor.getString(prodIdx));
                    d.setCategoria(cursor.getString(catIdx));
                    d.setPrecoMinimo(cursor.getDouble(minIdx));
                    d.setPrecoMaximo(cursor.getDouble(maxIdx));
                    d.setLojas(cursor.getString(lojasIdx));
                    int pid = cursor.getInt(pidIdx);
                    d.setParticipanteId(pid);
                    if (!mapa.containsKey(pid)) mapa.put(pid, new ArrayList<>());
                    mapa.get(pid).add(d);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return mapa;
    }

    public final int proximoId(){
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("select max(id) from desejo", null);
            cursor.moveToFirst();
            return cursor.getInt(0) + 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public final Desejo buscarPorId(int id){
        Desejo desejo = null;
        Cursor cursor = database.rawQuery("select * from "+helper.TABLE_DESEJO+" where "+helper.COLUMN_ID+" = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            desejo = new Desejo();
            desejo.setId(cursor.getInt(cursor.getColumnIndexOrThrow(helper.COLUMN_ID)));
            desejo.setProduto(cursor.getString(cursor.getColumnIndexOrThrow(helper.COLUMN_PRODUTO)));
            desejo.setCategoria(cursor.getString(cursor.getColumnIndexOrThrow(helper.COLUMN_CATEGORIA)));
            desejo.setPrecoMinimo(cursor.getDouble(cursor.getColumnIndexOrThrow(helper.COLUMN_PRECO_MINIMO)));
            desejo.setPrecoMaximo(cursor.getDouble(cursor.getColumnIndexOrThrow(helper.COLUMN_PRECO_MAXIMO)));
            desejo.setLojas(cursor.getString(cursor.getColumnIndexOrThrow(helper.COLUMN_LOJAS)));
            int participanteIdx = cursor.getColumnIndex(helper.COLUMN_DESEJO_PARTICIPANTE_ID);
            if (participanteIdx >= 0) {
                desejo.setParticipanteId(cursor.getInt(participanteIdx));
            }
        }
        cursor.close();
        return desejo;
    }
}
