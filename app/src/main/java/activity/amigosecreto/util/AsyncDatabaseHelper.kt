package activity.amigosecreto.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Helper class for performing database operations asynchronously
 * to avoid blocking the main UI thread
 */
object AsyncDatabaseHelper {

    // TODO: fase10d — singleton com pool fixo: após shutdown() (ex: Application.onTerminate()),
    //  chamadas subsequentes a execute() lançam RejectedExecutionException. Desaparece com coroutines.
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun <T> execute(backgroundTask: BackgroundTask<T>, resultCallback: ResultCallback<T>?) {
        executor.execute {
            try {
                val result = backgroundTask.doInBackground()
                mainHandler.post { resultCallback?.onSuccess(result) }
            } catch (e: Exception) {
                Log.e("AsyncDatabaseHelper", "Background task failed", e)
                mainHandler.post { resultCallback?.onError(e) }
            }
        }
    }

    @JvmStatic
    fun executeSimple(task: Runnable, callback: Runnable?) {
        executor.execute {
            try {
                task.run()
                if (callback != null) mainHandler.post(callback)
            } catch (e: Exception) {
                Log.e("AsyncDatabaseHelper", "Background task failed", e)
                // TODO: fase10d — com coroutines, a exceção propagará ao caller;
                // por ora, executeSimple não tem callback de erro e o caller não recebe feedback.
            }
        }
    }

    /** Interface for background tasks */
    interface BackgroundTask<T> {
        @Throws(Exception::class)
        fun doInBackground(): T
    }

    /** Interface for result callbacks */
    interface ResultCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Exception)
    }

    /** Shutdown executor (call this when app is closing) */
    @JvmStatic
    fun shutdown() {
        executor.shutdown()
    }
}
