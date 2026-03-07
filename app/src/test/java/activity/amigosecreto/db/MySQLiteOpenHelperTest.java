package activity.amigosecreto.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MySQLiteOpenHelperTest {

    private MySQLiteOpenHelper helper;
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        helper = new MySQLiteOpenHelper(ctx);
        db = helper.getWritableDatabase();
    }

    @After
    public void tearDown() {
        helper.close();
    }

    private List<String> getTabelasExistentes() {
        List<String> tabelas = new ArrayList<>();
        Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
            null
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    tabelas.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return tabelas;
    }

    private boolean colunaExiste(String tabela, String coluna) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tabela + ")", null);
        try {
            if (cursor.moveToFirst()) {
                int nomeIdx = cursor.getColumnIndexOrThrow("name");
                do {
                    if (coluna.equals(cursor.getString(nomeIdx))) return true;
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    // --- onCreate ---

    @Test
    public void onCreate_cria_tabela_grupo() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_GRUPO));
    }

    @Test
    public void onCreate_cria_tabela_participante() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_PARTICIPANTE));
    }

    @Test
    public void onCreate_cria_tabela_exclusao() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_EXCLUSAO));
    }

    @Test
    public void onCreate_cria_tabela_desejo() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_DESEJO));
    }

    @Test
    public void tabela_grupo_tem_colunas_esperadas() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_ID));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_NOME));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_DATA));
    }

    @Test
    public void tabela_participante_tem_colunas_esperadas() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_ID));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_NOME));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_EMAIL));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_TELEFONE));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_ENVIADO));
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID));
    }

    @Test
    public void tabela_desejo_tem_coluna_participante_id() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_DESEJO, MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID));
    }
}
