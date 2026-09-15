package activity.amigosecreto.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Exclusao
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.SorteioPar

/**
 * Room database for the application.
 *
 * Version history:
 * - 1..9:  MySQLiteOpenHelper (discontinued)
 * - 10:    MySQLiteOpenHelper — last migrations via legacy helper
 * - 11:    Room takes over management; schema identical to v10.
 *          Migration 10→11 adapts NOT NULL constraints and indexes to match Room expectations.
 * - 12:    Adds group configuration columns and participant tracking columns.
 * - 13:    Schema correction: adds missing defaultValue to @ColumnInfo annotations on
 *          participante (enviado, grupo_id) and desejo (preco_minimo, preco_maximo,
 *          participante_id). No data changes — no-op migration.
 *
 * The physical database file remains "amigosecreto_v10.db" to preserve user data.
 */
@Database(
    entities = [
        Grupo::class,
        Participante::class,
        Exclusao::class,
        Desejo::class,
        Sorteio::class,
        SorteioPar::class,
    ],
    version = 13,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun grupoDao(): GrupoRoomDao
    abstract fun participanteDao(): ParticipanteRoomDao
    abstract fun desejoDao(): DesejoRoomDao
    abstract fun sorteioDao(): SorteioRoomDao

    companion object {
        private const val DATABASE_NAME = "amigosecreto_v10.db"

        /**
         * Versão do schema gerenciado pelo Room — deve espelhar `version` da anotação
         * [Database] acima (a anotação exige um literal, por isso a duplicação).
         *
         * Usada por `BackupManager` para marcar e validar o `schema_version` dos arquivos
         * de backup.
         *
         * Guarda contra drift: `BackupManagerTest.schema_version_bate_com_a_versao_gravada_pelo_room`
         * compara esta constante com a versão que o Room gravou no arquivo. Bumpar `version`
         * na anotação sem atualizar esta constante quebra aquele teste — caso contrário o
         * backup passaria a declarar um `schema_version` silenciosamente desatualizado.
         *
         * A anotação tem retenção BINARY e não é legível por reflexão em runtime, por isso a
         * comparação é feita contra o banco aberto, e não contra a anotação.
         */
        const val SCHEMA_VERSION = 13

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 10 → 11: adapts the schema created by MySQLiteOpenHelper to the exact
         * format expected by Room (NOT NULL columns, indexes, FKs with ON DELETE CASCADE).
         *
         * MySQLiteOpenHelper created tables without NOT NULL and without auxiliary indexes.
         * Room validates the schema strictly — divergences cause IllegalStateException.
         * Strategy: rename → recreate with correct schema → copy → drop old → create indexes.
         *
         * Affected tables:
         * - participante: grupo_id and enviado need NOT NULL DEFAULT 0
         * - desejo:        preco_minimo, preco_maximo, participante_id need NOT NULL DEFAULT 0
         * - exclusao:      needs index index_exclusao_excluido_id
         *
         * grupo, sorteio and sorteio_par are already correct or are new — no changes.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            /** Retorna true se [table] possui a coluna [column]. */
            private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIdx) == column) return true
                    }
                }
                return false
            }

            /** true se `grupo` está no formato do helper legado (`nome` NOT NULL). */
            private fun grupoNoFormatoLegado(db: SupportSQLiteDatabase): Boolean {
                db.query("PRAGMA table_info(`grupo`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    val notNullIdx = cursor.getColumnIndex("notnull")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIdx) == "nome") return cursor.getInt(notNullIdx) == 1
                    }
                }
                return false
            }

            override fun migrate(db: SupportSQLiteDatabase) {
                // Um banco criado pelo MySQLiteOpenHelper tem TODAS as tabelas no formato
                // legado, que diverge do que o Room espera em nullability. Um banco que já
                // passou pelo Room tem todas corretas — o bug do user_version só corrompia o
                // carimbo, nunca o schema. Por isso uma detecção só, em `grupo`, decide pelo
                // conjunto: `participante` e `desejo` são recriados de qualquer forma abaixo,
                // e as demais só quando o banco é legado.
                val bancoLegado = grupoNoFormatoLegado(db)

                // --- grupo ---
                // Esta migration nunca corrigiu `grupo`. O helper legado cria
                // `id INTEGER PRIMARY KEY AUTOINCREMENT` (sem NOT NULL) e `nome TEXT NOT NULL`;
                // o Room espera exatamente o inverso desde a v11. Bancos genuinamente v10 —
                // instalações anteriores ao Room — falhavam na validação de schema, o Room
                // revertia a migration e o app ficava sem carregar dados (o try/catch de
                // AmigoSecretoApplication engole a exceção, então nem havia crash visível).
                //
                // Recriamos só quando a tabela está no formato legado: bancos que já passaram
                // pelo Room têm `grupo` correto e não são tocados. Vem antes de `participante`
                // porque este a referencia por FK.
                if (bancoLegado) {
                    db.execSQL("ALTER TABLE grupo RENAME TO grupo_old")
                    db.execSQL("""
                        CREATE TABLE grupo (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `nome` TEXT,
                            `data` TEXT
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO grupo (id, nome, data) SELECT id, nome, data FROM grupo_old")
                    db.execSQL("DROP TABLE grupo_old")
                }

                // --- participante ---
                db.execSQL("ALTER TABLE participante RENAME TO participante_old")

                // Bancos rebaixados pelo bug do user_version chegam aqui carimbados como v10
                // mas com o schema da v12/v13 completo — inclusive as colunas de rastreamento,
                // com dados. Até a v3.1 esta migration copiava só as colunas da v10 e as
                // descartava justamente na atualização que corrige o bug.
                //
                // Quando elas existem em participante_old, são carregadas junto; a
                // MIGRATION_11_12 usa addColumnIfMissing e simplesmente as pula depois.
                // Em bancos genuinamente v10 as colunas não existem e nada muda.
                val temRastreamento = columnExists(db, "participante_old", "confirmou_presente") &&
                    columnExists(db, "participante_old", "foi_notificado") &&
                    columnExists(db, "participante_old", "observacoes")

                val colunasRastreamento = if (temRastreamento) {
                    """,
                        `confirmou_presente` INTEGER NOT NULL DEFAULT 0,
                        `foi_notificado` INTEGER NOT NULL DEFAULT 0,
                        `observacoes` TEXT"""
                } else ""

                db.execSQL("""
                    CREATE TABLE participante (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT,
                        `email` TEXT,
                        `telefone` TEXT,
                        `amigo_sorteado_id` INTEGER,
                        `enviado` INTEGER NOT NULL DEFAULT 0,
                        `grupo_id` INTEGER NOT NULL DEFAULT 0$colunasRastreamento,
                        FOREIGN KEY(`grupo_id`) REFERENCES `grupo`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())

                if (temRastreamento) {
                    db.execSQL("""
                        INSERT INTO participante (id, nome, email, telefone, amigo_sorteado_id,
                                                  enviado, grupo_id,
                                                  confirmou_presente, foi_notificado, observacoes)
                        SELECT id, nome, email, telefone, amigo_sorteado_id,
                               COALESCE(enviado, 0), COALESCE(grupo_id, 0),
                               COALESCE(confirmou_presente, 0), COALESCE(foi_notificado, 0),
                               observacoes
                        FROM participante_old
                    """.trimIndent())
                } else {
                    db.execSQL("""
                        INSERT INTO participante (id, nome, email, telefone, amigo_sorteado_id, enviado, grupo_id)
                        SELECT id, nome, email, telefone, amigo_sorteado_id,
                               COALESCE(enviado, 0), COALESCE(grupo_id, 0)
                        FROM participante_old
                    """.trimIndent())
                }
                db.execSQL("DROP TABLE participante_old")

                // --- desejo ---
                db.execSQL("ALTER TABLE desejo RENAME TO desejo_old")
                db.execSQL("""
                    CREATE TABLE desejo (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `produto` TEXT,
                        `categoria` TEXT,
                        `lojas` TEXT,
                        `preco_minimo` REAL NOT NULL DEFAULT 0,
                        `preco_maximo` REAL NOT NULL DEFAULT 0,
                        `participante_id` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`participante_id`) REFERENCES `participante`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO desejo (id, produto, categoria, lojas, preco_minimo, preco_maximo, participante_id)
                    SELECT id, produto, categoria, lojas,
                           COALESCE(preco_minimo, 0), COALESCE(preco_maximo, 0),
                           COALESCE(participante_id, 0)
                    FROM desejo_old
                """.trimIndent())
                db.execSQL("DROP TABLE desejo_old")

                // --- exclusao, sorteio e sorteio_par ---
                // Também nunca foram corrigidas. No formato legado `exclusao` tem as duas
                // colunas nullable (o Room as exige NOT NULL), `sorteio.id` não é NOT NULL e
                // `sorteio_par` tem nomes e `enviado` nullable. Os CREATE TABLE IF NOT EXISTS
                // abaixo eram no-op justamente nos bancos legados, que já tinham as tabelas.
                if (bancoLegado) {
                    db.execSQL("ALTER TABLE exclusao RENAME TO exclusao_old")
                    db.execSQL("""
                        CREATE TABLE exclusao (
                            `participante_id` INTEGER NOT NULL,
                            `excluido_id` INTEGER NOT NULL,
                            PRIMARY KEY(`participante_id`, `excluido_id`),
                            FOREIGN KEY(`participante_id`) REFERENCES `participante`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`excluido_id`) REFERENCES `participante`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    // Linhas com NULL não cabem no schema novo e não têm significado útil.
                    db.execSQL("""
                        INSERT INTO exclusao (participante_id, excluido_id)
                        SELECT participante_id, excluido_id FROM exclusao_old
                        WHERE participante_id IS NOT NULL AND excluido_id IS NOT NULL
                    """.trimIndent())
                    db.execSQL("DROP TABLE exclusao_old")

                    db.execSQL("ALTER TABLE sorteio RENAME TO sorteio_old")
                    db.execSQL("""
                        CREATE TABLE sorteio (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `grupo_id` INTEGER NOT NULL,
                            `data_hora` TEXT NOT NULL,
                            FOREIGN KEY(`grupo_id`) REFERENCES `grupo`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO sorteio (id, grupo_id, data_hora)
                        SELECT id, grupo_id, data_hora FROM sorteio_old
                        WHERE grupo_id IS NOT NULL AND data_hora IS NOT NULL
                    """.trimIndent())
                    db.execSQL("DROP TABLE sorteio_old")

                    db.execSQL("ALTER TABLE sorteio_par RENAME TO sorteio_par_old")
                    db.execSQL("""
                        CREATE TABLE sorteio_par (
                            `sorteio_id` INTEGER NOT NULL,
                            `participante_id` INTEGER NOT NULL,
                            `sorteado_id` INTEGER NOT NULL,
                            `nome_participante` TEXT NOT NULL,
                            `nome_sorteado` TEXT NOT NULL,
                            `enviado` INTEGER NOT NULL,
                            PRIMARY KEY(`sorteio_id`, `participante_id`),
                            FOREIGN KEY(`sorteio_id`) REFERENCES `sorteio`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO sorteio_par (sorteio_id, participante_id, sorteado_id,
                                                 nome_participante, nome_sorteado, enviado)
                        SELECT sorteio_id, participante_id, sorteado_id,
                               COALESCE(nome_participante, ''), COALESCE(nome_sorteado, ''),
                               COALESCE(enviado, 0)
                        FROM sorteio_par_old
                        WHERE sorteio_id IS NOT NULL AND participante_id IS NOT NULL
                          AND sorteado_id IS NOT NULL
                    """.trimIndent())
                    db.execSQL("DROP TABLE sorteio_par_old")
                }

                // --- exclusao: index required by Room ---
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exclusao_excluido_id` ON `exclusao` (`excluido_id`)")

                // --- sorteio and sorteio_par: created by MySQLiteOpenHelper v10 via SorteioDAO ---
                // Ensure they exist (database may have been created before v10 was complete)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sorteio (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `grupo_id` INTEGER NOT NULL,
                        `data_hora` TEXT NOT NULL,
                        FOREIGN KEY(`grupo_id`) REFERENCES `grupo`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sorteio_par (
                        `sorteio_id` INTEGER NOT NULL,
                        `participante_id` INTEGER NOT NULL,
                        `sorteado_id` INTEGER NOT NULL,
                        `nome_participante` TEXT NOT NULL,
                        `nome_sorteado` TEXT NOT NULL,
                        `enviado` INTEGER NOT NULL,
                        PRIMARY KEY(`sorteio_id`, `participante_id`),
                        FOREIGN KEY(`sorteio_id`) REFERENCES `sorteio`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 11 → 12: adds new columns to grupo and participante for extended
         * group configuration (event details, value range, rules, preferences) and
         * participant tracking (confirmation, notification, observations).
         *
         * All new columns have default values so existing rows are preserved intact.
         *
         * Uses addColumnIfMissing() to guard each ALTER TABLE — some devices had these
         * columns already present (added by a previous Room instance before the migration
         * ran), causing "duplicate column name" errors and crash on startup.
         *
         * Note: sorteio and sorteio_par already exist — they were created in MIGRATION_10_11.
         * This migration only alters grupo and participante via ALTER TABLE ADD COLUMN.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIdx) == column) return true
                    }
                }
                return false
            }

            private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
                if (!columnExists(db, table, column)) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
                }
            }

            override fun migrate(db: SupportSQLiteDatabase) {
                // --- grupo: new configuration columns ---
                addColumnIfMissing(db, "grupo", "descricao", "TEXT")
                addColumnIfMissing(db, "grupo", "data_evento", "TEXT")
                addColumnIfMissing(db, "grupo", "local_evento", "TEXT")
                addColumnIfMissing(db, "grupo", "data_limite_sorteio", "TEXT")
                addColumnIfMissing(db, "grupo", "valor_minimo", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(db, "grupo", "valor_maximo", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(db, "grupo", "regras", "TEXT")
                addColumnIfMissing(db, "grupo", "permitir_ver_desejos", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "grupo", "exigir_confirmacao_compra", "INTEGER NOT NULL DEFAULT 0")

                // --- participante: new tracking columns ---
                addColumnIfMissing(db, "participante", "confirmou_presente", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "participante", "foi_notificado", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "participante", "observacoes", "TEXT")
            }
        }

        /**
         * Migration 12 → 13: no-op schema correction.
         *
         * Adds defaultValue to @ColumnInfo annotations on participante (enviado, grupo_id)
         * and desejo (preco_minimo, preco_maximo, participante_id) to match what
         * MIGRATION_10_11 already created via DDL. No data is changed — the actual columns
         * already exist with the correct DEFAULT constraint. This migration only bumps the
         * version so Room re-validates the schema against the corrected entity definitions,
         * resolving the IllegalStateException crash on app startup.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No DDL changes needed — columns already have correct DEFAULT constraints
                // from MIGRATION_10_11. This migration only forces Room schema re-validation.
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                    .also { INSTANCE = it }
            }

        /**
         * Initializes the database using the same file as MySQLiteOpenHelper (Robolectric).
         * Must be called in @Before for tests that use BackupManager.importarDeJson,
         * ensuring Room and legacy DAOs share the same database connection.
         */
        @androidx.annotation.VisibleForTesting
        fun initForTesting(context: Context) {
            synchronized(this) {
                // Close any existing instance before creating a new one.
                // This prevents the old WAL connection from interfering with the new one
                // and ensures MySQLiteOpenHelper can see data written by legacy DAOs.
                INSTANCE?.close()
                // Uses the same database file as MySQLiteOpenHelper so that Room and legacy DAOs
                // share the same data in Robolectric tests.
                // fallbackToDestructiveMigration avoids schema validation failures on a new database.
                // disableWriteAheadLogging: avoids WAL conflict between Room and MySQLiteOpenHelper.
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        // Forces database materialization (creates file + applies full schema)
                        // before any legacy DAO opens it via MySQLiteOpenHelper.
                        // Without this, MySQLiteOpenHelper opens first with DATABASE_VERSION=10
                        // and creates the database without columns added in migrations v11→v12
                        // (e.g. confirmou_presente), causing IllegalArgumentException in tests.
                        it.openHelper.writableDatabase
                    }
            }
        }

        /** Closes and clears the singleton after tests. */
        @androidx.annotation.VisibleForTesting
        fun closeForTesting() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
