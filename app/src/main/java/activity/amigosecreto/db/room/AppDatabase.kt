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
 * Banco de dados Room do aplicativo.
 *
 * Version history:
 * - 1..9:  MySQLiteOpenHelper (descontinuado)
 * - 10:    MySQLiteOpenHelper — últimas migrações via helper legado
 * - 11:    Room assume o gerenciamento; schema idêntico ao v10.
 *          A migration 10→11 é uma no-op: valida o schema existente sem alterar dados.
 *
 * O banco físico continua sendo "amigosecreto_v10.db" para preservar dados dos usuários.
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
    version = 12,
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
         * Migration 10 → 11: adapta o schema criado pelo MySQLiteOpenHelper para o formato
         * exato esperado pelo Room (colunas NOT NULL, índices, FKs com ON DELETE CASCADE).
         *
         * O MySQLiteOpenHelper criava as tabelas sem NOT NULL e sem índices auxiliares.
         * O Room valida o schema rigorosamente — divergências causam IllegalStateException.
         * Solução: rename → recreate com schema correto → copy → drop old → create indexes.
         *
         * Tabelas afetadas:
         * - participante: grupo_id e enviado precisam de NOT NULL
         * - desejo:        preco_minimo, preco_maximo e participante_id precisam de NOT NULL
         * - exclusao:      precisa do índice index_exclusao_excluido_id
         *
         * grupo, sorteio e sorteio_par já estão corretos ou são novas — sem alteração.
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

                // --- exclusao: índice exigido pelo Room ---
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exclusao_excluido_id` ON `exclusao` (`excluido_id`)")

                // --- sorteio e sorteio_par: criados pelo MySQLiteOpenHelper v10 via SorteioDAO ---
                // Garantir que existem (banco pode ter sido criado antes do v10 completo)
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

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                    .also { INSTANCE = it }
            }

        /**
         * Inicializa o banco usando o mesmo arquivo que MySQLiteOpenHelper (Robolectric).
         * Deve ser chamado no @Before de testes que usam BackupManager.importarDeJson,
         * garantindo que Room e DAOs legados compartilhem a mesma conexão de banco.
         */
        @androidx.annotation.VisibleForTesting
        fun initForTesting(context: Context) {
            synchronized(this) {
                // Close any existing instance before creating a new one.
                // This prevents the old WAL connection from interfering with the new one
                // and ensures MySQLiteOpenHelper can see data written by legacy DAOs.
                INSTANCE?.close()
                // Usa o mesmo arquivo de banco que MySQLiteOpenHelper para que Room e DAOs legados
                // compartilhem os mesmos dados nos testes Robolectric.
                // fallbackToDestructiveMigration evita falhas de schema validation em banco novo.
                // disableWriteAheadLogging: evita conflito de WAL entre Room e MySQLiteOpenHelper.
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }

        /**
         * Fecha e invalida o singleton Room.
         *
         * Deve ser chamado antes de operações que escrevem diretamente no SQLite via
         * MySQLiteOpenHelper (ex: BackupManager.importarDeJson), para garantir que:
         * 1. Não haja conflito de WAL entre as duas conexões.
         * 2. O Room reabra uma conexão limpa na próxima vez que getInstance() for chamado,
         *    tornando os dados importados imediatamente visíveis para queries Room/DAOs.
         */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /** Fecha e limpa o singleton após testes. */
        @androidx.annotation.VisibleForTesting
        fun closeForTesting() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
