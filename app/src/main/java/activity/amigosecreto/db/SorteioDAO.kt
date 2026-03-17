package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SorteioDAO(context: Context) {

    private val dbHelper = MySQLiteOpenHelper(context)
    private lateinit var database: SQLiteDatabase

    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    /**
     * Persiste um sorteio completo de forma atômica:
     * 1. Insere o evento em [MySQLiteOpenHelper.TABLE_SORTEIO]
     * 2. Insere todos os pares em [MySQLiteOpenHelper.TABLE_SORTEIO_PAR]
     * 3. Atualiza [MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID] em cada participante
     *
     * Os nomes dos participantes são snapshotados para preservar integridade histórica.
     *
     * @param grupoId ID do grupo
     * @param participantes lista de quem tirou (mesma ordem que [sorteados])
     * @param sorteados lista de quem foi tirado
     * @return ID do sorteio criado, ou -1 em caso de falha
     */
    fun salvarSorteioCompleto(
        grupoId: Int,
        participantes: List<Participante>,
        sorteados: List<Participante>
    ): Long {
        if (participantes.size != sorteados.size) {
            Log.e("SorteioDAO", "salvarSorteioCompleto: list size mismatch")
            return -1
        }
        val dataHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        database.beginTransaction()
        return try {
            // 1. Inserir evento de sorteio
            val sorteioValues = ContentValues().apply {
                put(MySQLiteOpenHelper.COLUMN_SORTEIO_GRUPO_ID, grupoId)
                put(MySQLiteOpenHelper.COLUMN_SORTEIO_DATA_HORA, dataHora)
            }
            val sorteioId = database.insertOrThrow(MySQLiteOpenHelper.TABLE_SORTEIO, null, sorteioValues)

            // 2. Inserir pares e 3. atualizar participante.amigo_sorteado_id
            for (i in participantes.indices) {
                val par = participantes[i]
                val sorteado = sorteados[i]

                val parValues = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEIO_ID, sorteioId)
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_PARTICIPANTE_ID, par.id)
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEADO_ID, sorteado.id)
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE, par.nome)
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_SORTEADO, sorteado.nome)
                    put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_ENVIADO, 0)
                }
                database.insertOrThrow(MySQLiteOpenHelper.TABLE_SORTEIO_PAR, null, parValues)

                val participanteValues = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID, sorteado.id)
                    put(MySQLiteOpenHelper.COLUMN_ENVIADO, 0)
                }
                database.update(
                    MySQLiteOpenHelper.TABLE_PARTICIPANTE, participanteValues,
                    "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(par.id.toString())
                )
            }

            database.setTransactionSuccessful()
            sorteioId
        } catch (e: SQLiteException) {
            Log.e("SorteioDAO", "salvarSorteioCompleto failed", e)
            -1
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Lista todos os sorteios de um grupo em ordem decrescente de data/hora.
     * Os pares de cada sorteio são carregados juntos.
     */
    fun listarPorGrupo(grupoId: Int): List<Sorteio> {
        val sorteios = mutableListOf<Sorteio>()
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_SORTEIO, null,
            "${MySQLiteOpenHelper.COLUMN_SORTEIO_GRUPO_ID} = ?", arrayOf(grupoId.toString()),
            null, null, "${MySQLiteOpenHelper.COLUMN_SORTEIO_DATA_HORA} DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val sorteio = mapearSorteioCursor(it)
                    sorteio.pares = listarParesPorSorteio(sorteio.id)
                    sorteios.add(sorteio)
                } while (it.moveToNext())
            }
        }
        return sorteios
    }

    /**
     * Retorna o sorteio mais recente do grupo, ou null se não houver nenhum.
     */
    fun buscarUltimoPorGrupo(grupoId: Int): Sorteio? {
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_SORTEIO, null,
            "${MySQLiteOpenHelper.COLUMN_SORTEIO_GRUPO_ID} = ?", arrayOf(grupoId.toString()),
            null, null, "${MySQLiteOpenHelper.COLUMN_SORTEIO_DATA_HORA} DESC",
            "1"
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val sorteio = mapearSorteioCursor(it)
                sorteio.pares = listarParesPorSorteio(sorteio.id)
                sorteio
            } else null
        }
    }

    private fun listarParesPorSorteio(sorteioId: Int): List<SorteioPar> {
        val pares = mutableListOf<SorteioPar>()
        val cursor = database.query(
            MySQLiteOpenHelper.TABLE_SORTEIO_PAR, null,
            "${MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEIO_ID} = ?", arrayOf(sorteioId.toString()),
            null, null, MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE
        )
        cursor.use {
            if (it.moveToFirst()) {
                do { pares.add(mapearParCursor(it)) } while (it.moveToNext())
            }
        }
        return pares
    }

    private fun mapearSorteioCursor(cursor: android.database.Cursor): Sorteio {
        return Sorteio().apply {
            id = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_ID))
            grupoId = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_GRUPO_ID))
            dataHora = cursor.getString(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_DATA_HORA)) ?: ""
        }
    }

    private fun mapearParCursor(cursor: android.database.Cursor): SorteioPar {
        return SorteioPar().apply {
            sorteioId = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEIO_ID))
            participanteId = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_PARTICIPANTE_ID))
            sorteadoId = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEADO_ID))
            nomeParticipante = cursor.getString(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE)) ?: ""
            nomeSorteado = cursor.getString(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_SORTEADO)) ?: ""
            enviado = cursor.getInt(cursor.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_ENVIADO)) == 1
        }
    }
}
