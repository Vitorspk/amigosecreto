package activity.amigosecreto.db

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MySQLiteOpenHelperTest {

    private lateinit var helper: MySQLiteOpenHelper
    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        helper = MySQLiteOpenHelper(ApplicationProvider.getApplicationContext())
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    private fun getTabelasExistentes(): List<String> {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
            null
        )
        return cursor.use {
            val tabelas = mutableListOf<String>()
            if (it.moveToFirst()) {
                do { tabelas.add(it.getString(0)) } while (it.moveToNext())
            }
            tabelas
        }
    }

    private fun colunaExiste(tabela: String, coluna: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($tabela)", null)
        return cursor.use {
            if (it.moveToFirst()) {
                val nomeIdx = it.getColumnIndexOrThrow("name")
                do {
                    if (coluna == it.getString(nomeIdx)) return true
                } while (it.moveToNext())
            }
            false
        }
    }

    // --- onCreate ---

    @Test
    fun onCreate_cria_tabela_grupo() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_GRUPO))
    }

    @Test
    fun onCreate_cria_tabela_participante() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_PARTICIPANTE))
    }

    @Test
    fun onCreate_cria_tabela_exclusao() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_EXCLUSAO))
    }

    @Test
    fun onCreate_cria_tabela_desejo() {
        assertTrue(getTabelasExistentes().contains(MySQLiteOpenHelper.TABLE_DESEJO))
    }

    @Test
    fun tabela_grupo_tem_colunas_esperadas() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_ID))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_NOME))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_GRUPO, MySQLiteOpenHelper.COLUMN_GRUPO_DATA))
    }

    @Test
    fun tabela_participante_tem_colunas_esperadas() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_ID))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_NOME))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_EMAIL))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_TELEFONE))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_ENVIADO))
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_PARTICIPANTE, MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID))
    }

    @Test
    fun tabela_desejo_tem_coluna_participante_id() {
        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_DESEJO, MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID))
    }

    // --- onUpgrade ---

    @Test
    fun onUpgrade_v7_para_v8_adiciona_coluna_participante_id_na_desejo() {
        db.execSQL("DROP TABLE IF EXISTS ${MySQLiteOpenHelper.TABLE_DESEJO}")
        db.execSQL(
            "CREATE TABLE ${MySQLiteOpenHelper.TABLE_DESEJO} (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "produto TEXT NOT NULL, " +
            "categoria TEXT, " +
            "preco_minimo REAL, " +
            "preco_maximo REAL, " +
            "lojas TEXT)"
        )
        assertFalse(colunaExiste(MySQLiteOpenHelper.TABLE_DESEJO, MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID))

        helper.onUpgrade(db, 7, 8)

        assertTrue(colunaExiste(MySQLiteOpenHelper.TABLE_DESEJO, MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID))
    }
}