package activity.amigosecreto.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MySQLiteOpenHelper private constructor(context: Context) : SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        @Volatile
        private var INSTANCE: MySQLiteOpenHelper? = null

        /**
         * Retorna a instância singleton do helper.
         *
         * SQLiteOpenHelper é thread-safe por design: o Android serializa chamadas a
         * getWritableDatabase/getReadableDatabase internamente. Usar uma única instância
         * evita que múltiplas threads disparem onUpgrade simultaneamente (race condition
         * que fechava o connection pool antes de todas as threads terminarem a migração).
         */
        @JvmStatic
        fun getInstance(context: Context): MySQLiteOpenHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MySQLiteOpenHelper(context).also { INSTANCE = it }
            }

        /** Apenas para testes — reseta o singleton entre testes. */
        @JvmStatic
        @androidx.annotation.VisibleForTesting
        fun resetInstanceForTesting() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
        const val TABLE_GRUPO = "grupo"
        const val COLUMN_GRUPO_ID = "id"
        const val COLUMN_GRUPO_NOME = "nome"
        const val COLUMN_GRUPO_DATA = "data"

        const val TABLE_PARTICIPANTE = "participante"
        const val COLUMN_ID = "id"
        const val COLUMN_NOME = "nome"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_TELEFONE = "telefone"
        const val COLUMN_AMIGO_SORTEADO_ID = "amigo_sorteado_id"
        const val COLUMN_ENVIADO = "enviado"
        const val COLUMN_FK_GRUPO_ID = "grupo_id"
        const val COLUMN_CONFIRMOU_PRESENTE = "confirmou_presente"

        const val TABLE_EXCLUSAO = "exclusao"
        const val COLUMN_PARTICIPANTE_ID = "participante_id"
        const val COLUMN_EXCLUIDO_ID = "excluido_id"

        // Mantendo colunas antigas para evitar erros de compilação em DAOs não utilizados
        const val TABLE_DESEJO = "desejo"
        const val COLUMN_PRODUTO = "produto"
        const val COLUMN_CATEGORIA = "categoria"
        const val COLUMN_PRECO_MINIMO = "preco_minimo"
        const val COLUMN_PRECO_MAXIMO = "preco_maximo"
        const val COLUMN_LOJAS = "lojas"
        const val COLUMN_DESEJO_PARTICIPANTE_ID = "participante_id"

        const val TABLE_SORTEIO = "sorteio"
        const val COLUMN_SORTEIO_ID = "id"
        const val COLUMN_SORTEIO_GRUPO_ID = "grupo_id"
        const val COLUMN_SORTEIO_DATA_HORA = "data_hora"

        const val TABLE_SORTEIO_PAR = "sorteio_par"
        const val COLUMN_SORTEIO_PAR_SORTEIO_ID = "sorteio_id"
        const val COLUMN_SORTEIO_PAR_PARTICIPANTE_ID = "participante_id"
        const val COLUMN_SORTEIO_PAR_SORTEADO_ID = "sorteado_id"
        const val COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE = "nome_participante"
        const val COLUMN_SORTEIO_PAR_NOME_SORTEADO = "nome_sorteado"
        const val COLUMN_SORTEIO_PAR_ENVIADO = "enviado"

        const val DATABASE_VERSION_PUBLIC = 10
        private const val DATABASE_NAME = "amigosecreto_v10.db"
        private const val DATABASE_VERSION = DATABASE_VERSION_PUBLIC
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(sqlCreateGrupo())
        db.execSQL(sqlCreateParticipante())
        db.execSQL(sqlCreateExclusao())
        db.execSQL(sqlCreateDesejo())
        db.execSQL(sqlCreateSorteio())
        db.execSQL(sqlCreateSorteioPar())
    }

    /**
     * Called when the database version in storage is newer than what this helper expects.
     * This happens when Room (version 11+) has already migrated the database and the legacy
     * MySQLiteOpenHelper (version 10) is opened afterwards. No-op: Room owns schema management
     * for v11+; legacy DAOs still work on the same tables they know about.
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Intentional no-op: allow legacy helper to open databases managed by Room
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations must be ordered oldest first to prevent data loss

        // Se precisar recriar tudo (para versões muito antigas)
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GRUPO")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTICIPANTE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_EXCLUSAO")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DESEJO")
            onCreate(db)
            return // Schema is already at latest version, no further migrations needed
        }

        if (oldVersion < 8) {
            // Adicionar coluna participante_id na tabela desejo
            db.execSQL("ALTER TABLE $TABLE_DESEJO ADD COLUMN $COLUMN_DESEJO_PARTICIPANTE_ID INTEGER")
        }

        if (oldVersion < 9) {
            // Adicionar ON DELETE CASCADE em exclusao e desejo.
            // SQLite não suporta ALTER TABLE ADD CONSTRAINT — é necessário recriar as tabelas.
            db.execSQL("PRAGMA foreign_keys = OFF")

            db.execSQL("ALTER TABLE $TABLE_EXCLUSAO RENAME TO ${TABLE_EXCLUSAO}_old")
            db.execSQL(sqlCreateExclusao())
            db.execSQL("INSERT INTO $TABLE_EXCLUSAO SELECT $COLUMN_PARTICIPANTE_ID, $COLUMN_EXCLUIDO_ID FROM ${TABLE_EXCLUSAO}_old")
            db.execSQL("DROP TABLE ${TABLE_EXCLUSAO}_old")

            db.execSQL("ALTER TABLE $TABLE_DESEJO RENAME TO ${TABLE_DESEJO}_old")
            db.execSQL(sqlCreateDesejo())
            db.execSQL(
                "INSERT INTO $TABLE_DESEJO SELECT $COLUMN_ID, $COLUMN_PRODUTO, $COLUMN_CATEGORIA, " +
                "$COLUMN_PRECO_MINIMO, $COLUMN_PRECO_MAXIMO, $COLUMN_LOJAS, $COLUMN_DESEJO_PARTICIPANTE_ID " +
                "FROM ${TABLE_DESEJO}_old"
            )
            db.execSQL("DROP TABLE ${TABLE_DESEJO}_old")

            db.execSQL("PRAGMA foreign_keys = ON")
        }

        if (oldVersion < 10) {
            // Adicionar tabelas de histórico de sorteios (v10)
            db.execSQL(sqlCreateSorteio().replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS"))
            db.execSQL(sqlCreateSorteioPar().replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS"))

            // Recuperar dados históricos: grupos com sorteio ativo viram um registro retroativo
            db.execSQL(
                "INSERT INTO $TABLE_SORTEIO ($COLUMN_SORTEIO_GRUPO_ID, $COLUMN_SORTEIO_DATA_HORA) " +
                "SELECT DISTINCT $COLUMN_FK_GRUPO_ID, 'Sorteio anterior' " +
                "FROM $TABLE_PARTICIPANTE " +
                "WHERE $COLUMN_AMIGO_SORTEADO_ID IS NOT NULL"
            )
            db.execSQL(
                "INSERT INTO $TABLE_SORTEIO_PAR (" +
                "$COLUMN_SORTEIO_PAR_SORTEIO_ID, $COLUMN_SORTEIO_PAR_PARTICIPANTE_ID, " +
                "$COLUMN_SORTEIO_PAR_SORTEADO_ID, $COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE, " +
                "$COLUMN_SORTEIO_PAR_NOME_SORTEADO, $COLUMN_SORTEIO_PAR_ENVIADO) " +
                "SELECT s.$COLUMN_SORTEIO_ID, p.$COLUMN_ID, p.$COLUMN_AMIGO_SORTEADO_ID, " +
                "p.$COLUMN_NOME, sorteado.$COLUMN_NOME, p.$COLUMN_ENVIADO " +
                "FROM $TABLE_PARTICIPANTE p " +
                "INNER JOIN $TABLE_SORTEIO s ON s.$COLUMN_SORTEIO_GRUPO_ID = p.$COLUMN_FK_GRUPO_ID " +
                "INNER JOIN $TABLE_PARTICIPANTE sorteado ON sorteado.$COLUMN_ID = p.$COLUMN_AMIGO_SORTEADO_ID " +
                "WHERE p.$COLUMN_AMIGO_SORTEADO_ID IS NOT NULL"
            )
        }
    }

    private fun sqlCreateGrupo() =
        "CREATE TABLE $TABLE_GRUPO(" +
        "$COLUMN_GRUPO_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "$COLUMN_GRUPO_NOME TEXT NOT NULL, " +
        "$COLUMN_GRUPO_DATA TEXT)"

    private fun sqlCreateParticipante() =
        "CREATE TABLE $TABLE_PARTICIPANTE(" +
        "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "$COLUMN_NOME TEXT NOT NULL, " +
        "$COLUMN_EMAIL TEXT, " +
        "$COLUMN_TELEFONE TEXT, " +
        "$COLUMN_AMIGO_SORTEADO_ID INTEGER, " +
        "$COLUMN_ENVIADO INTEGER DEFAULT 0, " +
        "$COLUMN_FK_GRUPO_ID INTEGER, " +
        "FOREIGN KEY($COLUMN_FK_GRUPO_ID) REFERENCES $TABLE_GRUPO($COLUMN_GRUPO_ID))"

    private fun sqlCreateExclusao() =
        "CREATE TABLE $TABLE_EXCLUSAO(" +
        "$COLUMN_PARTICIPANTE_ID INTEGER, " +
        "$COLUMN_EXCLUIDO_ID INTEGER, " +
        "PRIMARY KEY ($COLUMN_PARTICIPANTE_ID, $COLUMN_EXCLUIDO_ID), " +
        "FOREIGN KEY($COLUMN_PARTICIPANTE_ID) REFERENCES $TABLE_PARTICIPANTE($COLUMN_ID) ON DELETE CASCADE, " +
        "FOREIGN KEY($COLUMN_EXCLUIDO_ID) REFERENCES $TABLE_PARTICIPANTE($COLUMN_ID) ON DELETE CASCADE)"

    private fun sqlCreateDesejo() =
        "CREATE TABLE $TABLE_DESEJO(" +
        "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "$COLUMN_PRODUTO TEXT NOT NULL, " +
        "$COLUMN_CATEGORIA TEXT, " +
        "$COLUMN_PRECO_MINIMO REAL, " +
        "$COLUMN_PRECO_MAXIMO REAL, " +
        "$COLUMN_LOJAS TEXT, " +
        "$COLUMN_DESEJO_PARTICIPANTE_ID INTEGER, " +
        "FOREIGN KEY($COLUMN_DESEJO_PARTICIPANTE_ID) REFERENCES $TABLE_PARTICIPANTE($COLUMN_ID) ON DELETE CASCADE)"

    internal fun sqlCreateSorteio() =
        "CREATE TABLE $TABLE_SORTEIO(" +
        "$COLUMN_SORTEIO_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "$COLUMN_SORTEIO_GRUPO_ID INTEGER NOT NULL, " +
        "$COLUMN_SORTEIO_DATA_HORA TEXT NOT NULL, " +
        "FOREIGN KEY($COLUMN_SORTEIO_GRUPO_ID) REFERENCES $TABLE_GRUPO($COLUMN_GRUPO_ID) ON DELETE CASCADE)"

    internal fun sqlCreateSorteioPar() =
        "CREATE TABLE $TABLE_SORTEIO_PAR(" +
        "$COLUMN_SORTEIO_PAR_SORTEIO_ID INTEGER NOT NULL, " +
        "$COLUMN_SORTEIO_PAR_PARTICIPANTE_ID INTEGER NOT NULL, " +
        "$COLUMN_SORTEIO_PAR_SORTEADO_ID INTEGER NOT NULL, " +
        "$COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE TEXT, " +
        "$COLUMN_SORTEIO_PAR_NOME_SORTEADO TEXT, " +
        "$COLUMN_SORTEIO_PAR_ENVIADO INTEGER DEFAULT 0, " +
        "PRIMARY KEY ($COLUMN_SORTEIO_PAR_SORTEIO_ID, $COLUMN_SORTEIO_PAR_PARTICIPANTE_ID), " +
        "FOREIGN KEY($COLUMN_SORTEIO_PAR_SORTEIO_ID) REFERENCES $TABLE_SORTEIO($COLUMN_SORTEIO_ID) ON DELETE CASCADE)"
}
