package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting

class GrupoDAO(ctx: Context) {

    private val helper = MySQLiteOpenHelper(ctx)
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open() {
        database = helper.writableDatabase
    }

    fun close() {
        helper.close()
    }

    fun inserir(g: Grupo): Long {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_GRUPO_NOME, g.nome)
            put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA, g.data)
        }
        return database.insert(MySQLiteOpenHelper.TABLE_GRUPO, null, values)
    }

    fun atualizarNome(g: Grupo): Int {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_GRUPO_NOME, g.nome)
        }
        return database.update(
            MySQLiteOpenHelper.TABLE_GRUPO, values,
            "${MySQLiteOpenHelper.COLUMN_GRUPO_ID} = ?",
            arrayOf(g.id.toString())
        )
    }

    fun remover(id: Int) {
        database.beginTransaction()
        try {
            // ON DELETE CASCADE (schema v9) handles exclusao and desejo deletion automatically.
            database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?", arrayOf(id.toString()))
            database.delete(MySQLiteOpenHelper.TABLE_GRUPO, "${MySQLiteOpenHelper.COLUMN_GRUPO_ID} = ?", arrayOf(id.toString()))
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun listar(): List<Grupo> {
        val lista = mutableListOf<Grupo>()
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_GRUPO, null, null, null, null, null,
            "${MySQLiteOpenHelper.COLUMN_GRUPO_ID} DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_ID)
                val nomeIndex = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_NOME)
                val dataIndex = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_DATA)
                do {
                    val g = Grupo()
                    g.id = it.getInt(idIndex)
                    g.nome = it.getString(nomeIndex)
                    g.data = it.getString(dataIndex)
                    lista.add(g)
                } while (it.moveToNext())
            }
        }
        return lista
    }

    @VisibleForTesting
    fun limparTudo() {
        database.beginTransaction()
        try {
            database.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, null)
            database.delete(MySQLiteOpenHelper.TABLE_DESEJO, null, null)
            database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, null)
            database.delete(MySQLiteOpenHelper.TABLE_GRUPO, null, null)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
