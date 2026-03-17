package activity.amigosecreto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import activity.amigosecreto.util.NotificationHelper

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
    }
}
