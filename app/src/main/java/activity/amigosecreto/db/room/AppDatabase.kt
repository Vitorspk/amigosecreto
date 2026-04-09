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
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- participante ---
                db.execSQL("ALTER TABLE participante RENAME TO participante_old")
                db.execSQL("""
                    CREATE TABLE participante (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT,
                        `email` TEXT,
                        `telefone` TEXT,
                        `amigo_sorteado_id` INTEGER,
                        `enviado` INTEGER NOT NULL DEFAULT 0,
                        `grupo_id` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`grupo_id`) REFERENCES `grupo`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO participante (id, nome, email, telefone, amigo_sorteado_id, enviado, grupo_id)
                    SELECT id, nome, email, telefone, amigo_sorteado_id,
                           COALESCE(enviado, 0), COALESCE(grupo_id, 0)
                    FROM participante_old
                """.trimIndent())
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
         * Note: sorteio and sorteio_par already exist — they were created in MIGRATION_10_11.
         * This migration only alters grupo and participante via ALTER TABLE ADD COLUMN.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- grupo: new configuration columns ---
                db.execSQL("ALTER TABLE grupo ADD COLUMN `descricao` TEXT")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `data_evento` TEXT")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `local_evento` TEXT")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `data_limite_sorteio` TEXT")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `valor_minimo` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `valor_maximo` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `regras` TEXT")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `permitir_ver_desejos` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE grupo ADD COLUMN `exigir_confirmacao_compra` INTEGER NOT NULL DEFAULT 0")

                // --- participante: new tracking columns ---
                db.execSQL("ALTER TABLE participante ADD COLUMN `confirmou_presente` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE participante ADD COLUMN `foi_notificado` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE participante ADD COLUMN `observacoes` TEXT")
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

        /**
         * Closes and invalidates the Room singleton.
         *
         * Must be called before operations that write directly to SQLite via
         * MySQLiteOpenHelper (e.g. BackupManager.importarDeJson), to ensure:
         * 1. No WAL conflict between the two connections.
         * 2. Room reopens a clean connection the next time getInstance() is called,
         *    making imported data immediately visible to Room/DAO queries.
         */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
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
