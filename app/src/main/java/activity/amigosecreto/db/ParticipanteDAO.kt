package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

class ParticipanteDAO(ctx: Context) {

    private val helper = MySQLiteOpenHelper(ctx)
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open() {
        database = helper.writableDatabase
    }

    fun close() {
        helper.close()
    }

    fun inserir(p: Participante, grupoId: Int) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_NOME, p.nome)
            put(MySQLiteOpenHelper.COLUMN_EMAIL, p.email)
            put(MySQLiteOpenHelper.COLUMN_TELEFONE, p.telefone)
            put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0)
            put(MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID, grupoId)
        }
        val id = database.insert(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, values)
        p.id = id.toInt()
    }

    fun atualizar(p: Participante): Boolean {
        if (p.id <= 0) return false
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_NOME, p.nome)
            put(MySQLiteOpenHelper.COLUMN_EMAIL, p.email)
            put(MySQLiteOpenHelper.COLUMN_TELEFONE, p.telefone)
            // amigo_sorteado_id e enviado são preservados intencionalmente
        }
        val rows = database.update(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE, values,
            "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(p.id.toString())
        )
        return rows > 0
    }

    fun remover(id: Int) {
        val idStr = id.toString()
        database.beginTransaction()
        try {
            // Delete child records before parent to be FK-safe (schema v9 will add ON DELETE CASCADE)
            database.delete(
                MySQLiteOpenHelper.TABLE_EXCLUSAO,
                "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} = ? OR ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID} = ?",
                arrayOf(idStr, idStr)
            )
            database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(idStr))
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun deletarTodosDoGrupo(grupoId: Int) {
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE,
            arrayOf(MySQLiteOpenHelper.COLUMN_ID),
            "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?",
            arrayOf(grupoId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val idStr = it.getInt(0).toString()
                    database.delete(
                        MySQLiteOpenHelper.TABLE_EXCLUSAO,
                        "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} = ? OR ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID} = ?",
                        arrayOf(idStr, idStr)
                    )
                } while (it.moveToNext())
            }
        }
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?", arrayOf(grupoId.toString()))
    }

    fun limparSorteioDoGrupo(grupoId: Int) {
        val values = ContentValues().apply {
            putNull(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID)
            put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0)
        }
        database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values, "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?", arrayOf(grupoId.toString()))
    }

    fun adicionarExclusao(idParticipante: Int, idExcluido: Int) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID, idParticipante)
            put(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID, idExcluido)
        }
        database.insertWithOnConflict(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removerExclusao(idParticipante: Int, idExcluido: Int) {
        database.delete(
            MySQLiteOpenHelper.TABLE_EXCLUSAO,
            "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} = ? AND ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID} = ?",
            arrayOf(idParticipante.toString(), idExcluido.toString())
        )
    }

    /**
     * Aplica todas as alterações de exclusão de um participante em uma única transação atômica.
     * Evita falha parcial e múltiplos open/close que ocorreriam num loop de chamadas individuais.
     */
    fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
        database.beginTransaction()
        try {
            for (id in adicionar) {
                val values = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID, participanteId)
                    put(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID, id)
                }
                database.insertWithOnConflict(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            for (id in remover) {
                database.delete(
                    MySQLiteOpenHelper.TABLE_EXCLUSAO,
                    "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} = ? AND ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID} = ?",
                    arrayOf(participanteId.toString(), id.toString())
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun salvarSorteio(participantes: List<Participante>, sorteados: List<Participante>): Boolean {
        database.beginTransaction()
        try {
            for (i in participantes.indices) {
                val values = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID, sorteados[i].id)
                    put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0)
                }
                database.update(
                    MySQLiteOpenHelper.TABLE_PARTICIPANTE, values,
                    "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(participantes[i].id.toString())
                )
            }
            database.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            return false
        } finally {
            database.endTransaction()
        }
    }

    fun marcarComoEnviado(id: Int) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_ENVIADO, 1)
        }
        database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values, "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
    }

    fun listarPorGrupo(grupoId: Int): List<Participante> {
        val lista = mutableListOf<Participante>()
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE, null,
            "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?", arrayOf(grupoId.toString()),
            null, null, MySQLiteOpenHelper.COLUMN_NOME
        )
        cursor.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ID)
                val nomeIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_NOME)
                val emailIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_EMAIL)
                val telefoneIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_TELEFONE)
                val amigoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID)
                val enviadoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ENVIADO)
                do {
                    val p = Participante()
                    p.id = it.getInt(idIdx)
                    p.nome = it.getString(nomeIdx)
                    p.email = it.getString(emailIdx)
                    p.telefone = it.getString(telefoneIdx)
                    if (!it.isNull(amigoIdx)) p.amigoSorteadoId = it.getInt(amigoIdx)
                    p.isEnviado = it.getInt(enviadoIdx) == 1
                    // TODO: N+1 — listarExclusoes() is called per participant; replace with a single
                    //  batch query (JOIN or IN clause) similar to contarDesejosPorGrupo()
                    p.idsExcluidos = listarExclusoes(p.id).toMutableList()
                    lista.add(p)
                } while (it.moveToNext())
            }
        }
        return lista
    }

    private fun listarExclusoes(idParticipante: Int): List<Int> {
        val exclusoes = mutableListOf<Int>()
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_EXCLUSAO,
            arrayOf(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID),
            "${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} = ?",
            arrayOf(idParticipante.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do { exclusoes.add(it.getInt(0)) } while (it.moveToNext())
            }
        }
        return exclusoes
    }

    fun getNomeAmigoSorteado(amigoId: Int): String {
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE,
            arrayOf(MySQLiteOpenHelper.COLUMN_NOME),
            "${MySQLiteOpenHelper.COLUMN_ID} = ?",
            arrayOf(amigoId.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else "Ninguém"
        }
    }

    fun contarPorGrupo(): Map<Int, Int> {
        val mapa = mutableMapOf<Int, Int>()
        val cursor = database.rawQuery(
            "SELECT ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}, COUNT(*) AS cnt" +
            " FROM ${MySQLiteOpenHelper.TABLE_PARTICIPANTE}" +
            " GROUP BY ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val grupoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID)
                val cntIdx = it.getColumnIndexOrThrow("cnt")
                do { mapa[it.getInt(grupoIdx)] = it.getInt(cntIdx) } while (it.moveToNext())
            }
        }
        return mapa
    }

    fun buscarPorId(id: Int): Participante? {
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE, null,
            "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (!it.moveToFirst()) return null
            val p = Participante()
            p.id = it.getInt(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ID))
            p.nome = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_NOME))
            p.email = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_EMAIL))
            p.telefone = it.getString(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_TELEFONE))
            val amigoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID)
            if (!it.isNull(amigoIdx)) p.amigoSorteadoId = it.getInt(amigoIdx)
            p.isEnviado = it.getInt(it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_ENVIADO)) == 1
            p
        }
    }
}
