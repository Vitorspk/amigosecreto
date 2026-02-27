package activity.amigosecreto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

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
        values.put(helper.COLUMN_ID, old_desejo.getId());
        values.put(helper.COLUMN_PRODUTO, new_desejo.getProduto());
        values.put(helper.COLUMN_CATEGORIA, new_desejo.getCategoria());
        values.put(helper.COLUMN_PRECO_MINIMO, new_desejo.getPrecoMinimo());
        values.put(helper.COLUMN_PRECO_MAXIMO, new_desejo.getPrecoMaximo());
        values.put(helper.COLUMN_LOJAS, new_desejo.getLojas());
        values.put(helper.COLUMN_DESEJO_PARTICIPANTE_ID, new_desejo.getParticipanteId());
        database.update(helper.TABLE_DESEJO, values, helper.COLUMN_ID+" = "+old_desejo.getId(), null);
    }

    public final void inserir(Desejo desejo){
        ContentValues values = new ContentValues();
        values.put(helper.COLUMN_ID, desejo.getId());
        values.put(helper.COLUMN_PRODUTO, desejo.getProduto());
        values.put(helper.COLUMN_CATEGORIA, desejo.getCategoria());
        values.put(helper.COLUMN_PRECO_MINIMO, desejo.getPrecoMinimo());
        values.put(helper.COLUMN_PRECO_MAXIMO, desejo.getPrecoMaximo());
        values.put(helper.COLUMN_LOJAS, desejo.getLojas());
        values.put(helper.COLUMN_DESEJO_PARTICIPANTE_ID, desejo.getParticipanteId());
        database.insert(helper.TABLE_DESEJO, null, values);
    }

    public final void remover(Desejo desejo){
        database.delete(helper.TABLE_DESEJO, helper.COLUMN_ID+" = "+desejo.getId(), null);
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

    public final int proximoId(){
        try {
            int id = 0;
            Cursor cursor = database.rawQuery("select max(id) from desejo", null);
            cursor.moveToFirst();
            id = cursor.getInt(0);
            return id + 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}
