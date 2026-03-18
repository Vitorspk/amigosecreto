package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log

class ParticipanteDAO(ctx: Context) {

    companion object {
        // TODO: move to strings.xml when RevelarAmigoActivity migrates to Kotlin (Fase 10e)
        //  and getNomeAmigoSorteado() can return String? instead.
        const val NOME_AMIGO_DESCONHECIDO = "Ninguém"
    }

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
        p.id = id.toInt() // safe: participant count never approaches Int.MAX_VALUE (2^31-1)
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
        // ON DELETE CASCADE (schema v9) handles exclusao and desejo deletion automatically.
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
    }

    fun deletarTodosDoGrupo(grupoId: Int) {
        // ON DELETE CASCADE (schema v9) handles exclusao and desejo deletion automatically.
        database.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, "${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID} = ?", arrayOf(grupoId.toString()))
    }

    fun atualizarAmigoSorteado(participanteId: Int, amigoSorteadoId: Int) {
        val values = ContentValues().apply {
            put(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID, amigoSorteadoId)
        }
        database.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, values,
            "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(participanteId.toString()))
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
        if (participantes.size != sorteados.size) {
            Log.e("ParticipanteDAO", "salvarSorteio: list size mismatch (${participantes.size} vs ${sorteados.size})")
            return false
        }
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
        } catch (e: SQLiteException) {
            Log.e("ParticipanteDAO", "salvarSorteio failed", e)
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
        // Pass 1 — fetch participants
        val mapaParticipantes = linkedMapOf<Int, Participante>()
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
                    mapaParticipantes[p.id] = p
                } while (it.moveToNext())
            }
        }
        if (mapaParticipantes.isEmpty()) return emptyList()

        // Pass 2 — fetch all exclusions for the group in one query (eliminates N+1).
        // IDs are integers from our own DB — no SQL injection risk. rawQuery with '?'
        // does not support dynamic IN lists, so interpolation is the correct approach here.
        val ids = mapaParticipantes.keys.joinToString(",")
        val exclCursor = database.rawQuery(
            "SELECT ${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID}, ${MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID}" +
            " FROM ${MySQLiteOpenHelper.TABLE_EXCLUSAO}" +
            " WHERE ${MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID} IN ($ids)",
            null
        )
        exclCursor.use {
            if (it.moveToFirst()) {
                val pidIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID)
                val eidIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID)
                do {
                    mapaParticipantes[it.getInt(pidIdx)]?.idsExcluidos?.add(it.getInt(eidIdx))
                } while (it.moveToNext())
            }
        }

        return mapaParticipantes.values.toList()
    }

    fun getNomeAmigoSorteado(amigoId: Int): String {
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_PARTICIPANTE,
            arrayOf(MySQLiteOpenHelper.COLUMN_NOME),
            "${MySQLiteOpenHelper.COLUMN_ID} = ?",
            arrayOf(amigoId.toString()),
            null, null, null
        )
        // "Ninguém" is a DAO-level fallback; ideally the ViewModel would own this string
        // (via getString R.string.*), but changing the return type cascades to RevelarAmigoActivity.java
        // which calls the DAO directly. TODO: migrate to String? return in Fase 10e when
        // RevelarAmigoActivity migrates to Kotlin.
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else NOME_AMIGO_DESCONHECIDO
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

    /**
     * Retorna o número de grupos que têm >= [minParticipantes] participantes mas onde
     * nenhum participante possui amigo_sorteado_id definido (sorteio ainda não realizado).
     *
     * Usa GROUP BY + HAVING numa única query, evitando N+1.
     * Padrão equivalente a [contarPorGrupo] e ao batch de DesejoDAO.
     *
     * Nota: [minParticipantes] é um Int interno injetado como literal no SQL (não como ?).
     * rawQuery com ? dentro de subquery tem comportamento inconsistente no Robolectric 4.13;
     * o valor não vem de entrada externa, portanto não há risco de SQL injection.
     */
    fun contarGruposPendentes(minParticipantes: Int = 3): Int {
        // COUNT(coluna) conta apenas valores não-NULL — se todos amigo_sorteado_id são NULL,
        // COUNT = 0, indicando que o sorteio ainda não foi realizado.
        val sql =
            "SELECT ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}" +
            " FROM ${MySQLiteOpenHelper.TABLE_PARTICIPANTE}" +
            " GROUP BY ${MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID}" +
            " HAVING COUNT(*) >= $minParticipantes" +
            "   AND COUNT(${MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID}) = 0"
        val cursor = database.rawQuery(sql, null)
        return cursor.use { it.count }
    }

    /**
     * Retorna o participante pelo id, ou null se não existir.
     * Nota: [Participante.idsExcluidos] NÃO é populado — use [listarPorGrupo] quando
     * as exclusões forem necessárias.
     */
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
