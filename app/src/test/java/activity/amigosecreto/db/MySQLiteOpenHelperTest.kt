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

    @Test
    fun onUpgrade_v8_para_v9_preserva_dados_de_exclusao() {
        // Set up v8 schema tables and insert data
        db.execSQL("DROP TABLE IF EXISTS ${MySQLiteOpenHelper.TABLE_EXCLUSAO}")
        db.execSQL(
            "CREATE TABLE ${MySQLiteOpenHelper.TABLE_EXCLUSAO}(" +
            "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} INTEGER, " +
            "${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID} INTEGER, " +
            "PRIMARY KEY (${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID}, ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID}))"
        )
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_EXCLUSAO} VALUES (1, 2)")
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_EXCLUSAO} VALUES (3, 4)")

        helper.onUpgrade(db, 8, 9)

        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${MySQLiteOpenHelper.TABLE_EXCLUSAO}", null)
        val count = cursor.use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, count)
    }

    @Test
    fun onUpgrade_v8_para_v9_preserva_dados_de_desejo() {
        // Remove existing desejo table and create v8 version without CASCADE
        db.execSQL("DROP TABLE IF EXISTS ${MySQLiteOpenHelper.TABLE_DESEJO}")
        db.execSQL(
            "CREATE TABLE ${MySQLiteOpenHelper.TABLE_DESEJO}(" +
            "${MySQLiteOpenHelper.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "${MySQLiteOpenHelper.COLUMN_PRODUTO} TEXT NOT NULL, " +
            "${MySQLiteOpenHelper.COLUMN_CATEGORIA} TEXT, " +
            "${MySQLiteOpenHelper.COLUMN_PRECO_MINIMO} REAL, " +
            "${MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO} REAL, " +
            "${MySQLiteOpenHelper.COLUMN_LOJAS} TEXT, " +
            "${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID} INTEGER)"
        )
        db.execSQL(
            "INSERT INTO ${MySQLiteOpenHelper.TABLE_DESEJO} " +
            "(${MySQLiteOpenHelper.COLUMN_PRODUTO}, ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}) " +
            "VALUES ('Bola', 1)"
        )
        db.execSQL(
            "INSERT INTO ${MySQLiteOpenHelper.TABLE_DESEJO} " +
            "(${MySQLiteOpenHelper.COLUMN_PRODUTO}, ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}) " +
            "VALUES ('Livro', 2)"
        )

        helper.onUpgrade(db, 8, 9)

        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${MySQLiteOpenHelper.TABLE_DESEJO}", null)
        val count = cursor.use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, count)
    }

    // --- ON DELETE CASCADE ---

    @Test
    fun cascade_remover_participante_apaga_exclusoes_associadas() {
        db.execSQL("PRAGMA foreign_keys = ON")
        // Insert grupo and participantes
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_GRUPO} (${MySQLiteOpenHelper.COLUMN_GRUPO_NOME}) VALUES ('G1')")
        val grupoId = db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getInt(0) }
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} (${MySQLiteOpenHelper.COLUMN_NOME}, ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}) VALUES ('Alice', $grupoId)")
        val p1Id = db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getInt(0) }
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} (${MySQLiteOpenHelper.COLUMN_NOME}, ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}) VALUES ('Bob', $grupoId)")
        val p2Id = db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getInt(0) }
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_EXCLUSAO} VALUES ($p1Id, $p2Id)")

        db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} WHERE ${MySQLiteOpenHelper.COLUMN_ID} = $p1Id")

        val count = db.rawQuery("SELECT COUNT(*) FROM ${MySQLiteOpenHelper.TABLE_EXCLUSAO}", null)
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, count)
    }

    @Test
    fun cascade_remover_participante_apaga_desejos_associados() {
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_GRUPO} (${MySQLiteOpenHelper.COLUMN_GRUPO_NOME}) VALUES ('G2')")
        val grupoId = db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getInt(0) }
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} (${MySQLiteOpenHelper.COLUMN_NOME}, ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}) VALUES ('Carlos', $grupoId)")
        val pId = db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getInt(0) }
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_DESEJO} (${MySQLiteOpenHelper.COLUMN_PRODUTO}, ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}) VALUES ('Bola', $pId)")
        db.execSQL("INSERT INTO ${MySQLiteOpenHelper.TABLE_DESEJO} (${MySQLiteOpenHelper.COLUMN_PRODUTO}, ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}) VALUES ('Livro', $pId)")

        db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} WHERE ${MySQLiteOpenHelper.COLUMN_ID} = $pId")

        val count = db.rawQuery("SELECT COUNT(*) FROM ${MySQLiteOpenHelper.TABLE_DESEJO}", null)
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, count)
    }
}