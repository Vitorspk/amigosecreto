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
    version = 11,
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
         * Migration 10 → 11: Room assume o gerenciamento sem alterar o schema.
         * Necessária para que Room não recrie o banco (destruindo dados dos usuários).
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema idêntico ao v10 — Room apenas valida e registra a versão.
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_10_11)
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
                // Usa o mesmo arquivo de banco que MySQLiteOpenHelper para que Room e DAOs legados
                // compartilhem os mesmos dados nos testes Robolectric.
                // fallbackToDestructiveMigration evita falhas de schema validation em banco novo.
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
