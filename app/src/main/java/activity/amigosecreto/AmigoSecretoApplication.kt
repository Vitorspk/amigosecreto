package activity.amigosecreto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required by Hilt for component generation.
 *
 * Registered in AndroidManifest.xml via android:name=".AmigoSecretoApplication".
 */
@HiltAndroidApp
class AmigoSecretoApplication : Application()
