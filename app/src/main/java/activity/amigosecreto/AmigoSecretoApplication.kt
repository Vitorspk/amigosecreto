package activity.amigosecreto

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.NotificationHelper
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class required by Hilt for component generation.
 *
 * Implements [Configuration.Provider] to provide [HiltWorkerFactory] to WorkManager,
 * enabling @HiltWorker constructor injection (e.g. [activity.amigosecreto.util.LembreteWorker]).
 * The default WorkManager initializer is removed from AndroidManifest via tools:node="remove"
 * so that our custom configuration takes effect.
 *
 * Registered in AndroidManifest.xml via android:name=".AmigoSecretoApplication".
 */
@HiltAndroidApp
class AmigoSecretoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializa o Room de forma eager e bloqueia até que a migration 10→11 esteja
        // completamente concluída antes que qualquer DAO legado (MySQLiteOpenHelper) tente
        // abrir o mesmo arquivo de banco. A query SELECT 1 garante que o connection pool
        // do Room está totalmente estabelecido (migration finalizada) antes de retornar.
        // Sem isso, GruposActivity cria GrupoDAO/ParticipanteDAO no onCreate em paralelo
        // com a migration do Room, causando race condition que fecha o connection pool.
        try {
            val db = AppDatabase.getInstance(this)
            // Força abertura completa do banco (executa migration se necessário)
            db.openHelper.writableDatabase
        } catch (e: Exception) {
            Timber.e(e, "AmigoSecretoApplication: falha ao inicializar Room")
        }
        NotificationHelper.criarCanal(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // TODO: plantar CrashlyticsTree() aqui quando/se integrar Firebase Crashlytics
            // Timber.plant(CrashlyticsTree())
        }
    }
}
