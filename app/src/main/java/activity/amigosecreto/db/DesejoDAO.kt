package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

class DesejoDAO(ctx: Context) {

    private val helper = MySQLiteOpenHelper(ctx)
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open() {
        database = helper.writableDatabase
    }

    fun close() {
        helper.close()
    }

    fun alterar(oldDesejo: Desejo, newDesejo: Desejo) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_PRODUTO, newDesejo.produto)
            put(MySQLiteOpenHelper.COLUMN_CATEGORIA, newDesejo.categoria)
            put(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO, newDesejo.precoMinimo)
            put(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO, newDesejo.precoMaximo)
            put(MySQLiteOpenHelper.COLUMN_LOJAS, newDesejo.lojas)
            put(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID, newDesejo.participanteId)
        }
        database.update(MySQLiteOpenHelper.TABLE_DESEJO, values, "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(oldDesejo.id.toString()))
    }

    fun inserir(desejo: Desejo) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_PRODUTO, desejo.produto)
            put(MySQLiteOpenHelper.COLUMN_CATEGORIA, desejo.categoria)
            put(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO, desejo.precoMinimo)
            put(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO, desejo.precoMaximo)
            put(MySQLiteOpenHelper.COLUMN_LOJAS, desejo.lojas)
            put(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID, desejo.participanteId)
        }
        val id = database.insert(MySQLiteOpenHelper.TABLE_DESEJO, null, values)
        // Only set ID if insert succeeded (returns -1 on failure)
        if (id != -1L) desejo.id = id.toInt()
    }

    fun remover(desejo: Desejo) {
        database.delete(MySQLiteOpenHelper.TABLE_DESEJO, "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(desejo.id.toString()))
    }

    fun listar(): List<Desejo> {
        val cursor = database.rawQuery("SELECT * FROM ${MySQLiteOpenHelper.TABLE_DESEJO}", null)
        return mapearDesejosCursor(cursor)
    }

    fun listarPorParticipante(participanteId: Int): List<Desejo> {
        val cursor = database.rawQuery(
            "SELECT * FROM ${MySQLiteOpenHelper.TABLE_DESEJO} WHERE ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID} = ?",
            arrayOf(participanteId.toString())
        )
        return mapearDesejosCursor(cursor)
    }

    /** Maps a SELECT * cursor over TABLE_DESEJO into a List<Desejo>. */
    private fun mapearDesejosCursor(cursor: Cursor): List<Desejo> {
        val lista = mutableListOf<Desejo>()
        cursor.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ID)
                val prodIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRODUTO)
                val catIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_CATEGORIA)
                val minIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO)
                val maxIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO)
                val lojasIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_LOJAS)
                val pidIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID)
                do {
                    val d = Desejo()
                    d.id = it.getInt(idIdx)
                    d.produto = it.getString(prodIdx)
                    d.categoria = it.getString(catIdx)
                    d.precoMinimo = it.getDouble(minIdx)
                    d.precoMaximo = it.getDouble(maxIdx)
                    d.lojas = it.getString(lojasIdx)
                    d.participanteId = it.getInt(pidIdx)
                    lista.add(d)
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun contarDesejosPorParticipante(participanteId: Int): Int {
        val cursor = database.rawQuery(
            "SELECT COUNT(*) FROM ${MySQLiteOpenHelper.TABLE_DESEJO} WHERE ${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID} = ?",
            arrayOf(participanteId.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * Retorna um mapa participante_id → quantidade de desejos para todos os participantes
     * de um grupo, usando uma única query com GROUP BY. Evita o problema N+1 de chamar
     * contarDesejosPorParticipante() individualmente para cada participante.
     */
    fun contarDesejosPorGrupo(grupoId: Int): Map<Int, Int> {
        val mapa = mutableMapOf<Int, Int>()
        val sql = """
            SELECT d.${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}, COUNT(*) AS cnt
            FROM ${MySQLiteOpenHelper.TABLE_DESEJO} d
            INNER JOIN ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} p
                ON d.${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID} = p.${MySQLiteOpenHelper.COLUMN_ID}
            WHERE p.${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?
            GROUP BY d.${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}
        """.trimIndent()
        val cursor = database.rawQuery(sql, arrayOf(grupoId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val pidIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID)
                val cntIdx = it.getColumnIndexOrThrow("cnt")
                do { mapa[it.getInt(pidIdx)] = it.getInt(cntIdx) } while (it.moveToNext())
            }
        }
        return mapa
    }

    /**
     * Retorna um mapa participante_id → lista de desejos para todos os participantes de um grupo,
     * usando uma única query com JOIN. Evita N open/close ao preparar mensagens para o grupo inteiro.
     */
    fun listarDesejosPorGrupo(grupoId: Int): Map<Int, List<Desejo>> {
        val mapa = mutableMapOf<Int, MutableList<Desejo>>()
        val sql = """
            SELECT d.${MySQLiteOpenHelper.COLUMN_ID},
                   d.${MySQLiteOpenHelper.COLUMN_PRODUTO},
                   d.${MySQLiteOpenHelper.COLUMN_CATEGORIA},
                   d.${MySQLiteOpenHelper.COLUMN_PRECO_MINIMO},
                   d.${MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO},
                   d.${MySQLiteOpenHelper.COLUMN_LOJAS},
                   d.${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID}
            FROM ${MySQLiteOpenHelper.TABLE_DESEJO} d
            INNER JOIN ${MySQLiteOpenHelper.TABLE_PARTICIPANTE} p
                ON d.${MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID} = p.${MySQLiteOpenHelper.COLUMN_ID}
            WHERE p.${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?
        """.trimIndent()
        val cursor = database.rawQuery(sql, arrayOf(grupoId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ID)
                val prodIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRODUTO)
                val catIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_CATEGORIA)
                val minIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO)
                val maxIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO)
                val lojasIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_LOJAS)
                val pidIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID)
                do {
                    val d = Desejo()
                    d.id = it.getInt(idIdx)
                    d.produto = it.getString(prodIdx)
                    d.categoria = it.getString(catIdx)
                    d.precoMinimo = it.getDouble(minIdx)
                    d.precoMaximo = it.getDouble(maxIdx)
                    d.lojas = it.getString(lojasIdx)
                    val pid = it.getInt(pidIdx)
                    d.participanteId = pid
                    mapa.getOrPut(pid) { mutableListOf() }.add(d)
                } while (it.moveToNext())
            }
        }
        return mapa
    }

    fun buscarPorId(id: Int): Desejo? {
        val cursor = database.rawQuery(
            "SELECT * FROM ${MySQLiteOpenHelper.TABLE_DESEJO} WHERE ${MySQLiteOpenHelper.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
        return cursor.use {
            if (!it.moveToFirst()) return null
            val d = Desejo()
            d.id = it.getInt(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ID))
            d.produto = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRODUTO))
            d.categoria = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_CATEGORIA))
            d.precoMinimo = it.getDouble(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO))
            d.precoMaximo = it.getDouble(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO))
            d.lojas = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_LOJAS))
            d.participanteId = it.getInt(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID))
            d
        }
    }
}
