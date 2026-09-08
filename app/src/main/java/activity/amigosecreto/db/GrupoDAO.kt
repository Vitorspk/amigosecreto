package activity.amigosecreto.db

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting

class GrupoDAO(ctx: Context) {

    private val helper = MySQLiteOpenHelper.getInstance(ctx)
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open() {
        database = helper.writableDatabase
    }

    fun close() {
        // No-op: o helper é singleton — fechar aqui fecharia o pool compartilhado por todos
        // os DAOs do processo. O pool fica aberto pelo tempo de vida do app.
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

    /**
     * Lista todos os grupos, incluindo as colunas de configuração adicionadas na v12.
     *
     * As colunas v12 são criadas pela `MIGRATION_11_12` do Room, não por
     * [MySQLiteOpenHelper.onCreate] — que está congelado na v10. O eager init do Room em
     * `AmigoSecretoApplication.onCreate()` garante que existam antes de qualquer DAO legado
     * abrir o arquivo, por isso `getColumnIndexOrThrow` é seguro e falha ruidosamente se
     * essa ordem for quebrada.
     */
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
                val descricaoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_DESCRICAO)
                val dataEventoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_DATA_EVENTO)
                val localEventoIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_LOCAL_EVENTO)
                val dataLimiteIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_DATA_LIMITE_SORTEIO)
                val valorMinIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_VALOR_MINIMO)
                val valorMaxIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_VALOR_MAXIMO)
                val regrasIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_REGRAS)
                val permitirIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_PERMITIR_VER_DESEJOS)
                val exigirIdx = it.getColumnIndexOrThrow(MySQLiteOpenHelper.COLUMN_GRUPO_EXIGIR_CONFIRMACAO_COMPRA)
                do {
                    val g = Grupo()
                    g.id = it.getInt(idIndex)
                    g.nome = it.getString(nomeIndex)
                    g.data = it.getString(dataIndex)
                    g.descricao = it.getString(descricaoIdx)
                    g.dataEvento = it.getString(dataEventoIdx)
                    g.localEvento = it.getString(localEventoIdx)
                    g.dataLimiteSorteio = it.getString(dataLimiteIdx)
                    g.valorMinimo = it.getDouble(valorMinIdx)
                    g.valorMaximo = it.getDouble(valorMaxIdx)
                    g.regras = it.getString(regrasIdx)
                    g.permitirVerDesejos = it.getInt(permitirIdx) == 1
                    g.exigirConfirmacaoCompra = it.getInt(exigirIdx) == 1
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
