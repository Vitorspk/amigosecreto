package activity.amigosecreto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import activity.amigosecreto.util.NotificationHelper
import timber.log.Timber

/**
 * Application class required by Hilt for component generation.
 *
 * Registered in AndroidManifest.xml via android:name=".AmigoSecretoApplication".
 */
@HiltAndroidApp
class AmigoSecretoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.criarCanal(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // TODO: plantar CrashlyticsTree() aqui quando/se integrar Firebase Crashlytics
            // Timber.plant(CrashlyticsTree())
        }
    }
}
