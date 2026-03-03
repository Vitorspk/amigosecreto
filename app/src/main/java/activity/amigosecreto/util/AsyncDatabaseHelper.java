package activity.amigosecreto.util;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for performing database operations asynchronously
 * to avoid blocking the main UI thread
 */
public class AsyncDatabaseHelper {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Execute a database operation in the background
     * @param backgroundTask Task to run in background thread
     * @param resultCallback Callback to run on main thread with result
     * @param <T> Type of result
     */
    public static <T> void execute(
            BackgroundTask<T> backgroundTask,
            ResultCallback<T> resultCallback) {

        executor.execute(() -> {
            try {
                // Execute background task
                T result = backgroundTask.doInBackground();

                // Post result back to main thread
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Post error back to main thread
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Execute a simple database operation without return value
     * @param task Task to run in background
     * @param callback Callback when completed
     */
    public static void executeSimple(Runnable task, Runnable callback) {
        executor.execute(() -> {
            try {
                task.run();
                if (callback != null) {
                    mainHandler.post(callback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Interface for background tasks
     */
    public interface BackgroundTask<T> {
        T doInBackground() throws Exception;
    }

    /**
     * Interface for result callbacks
     */
    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Shutdown executor (call this when app is closing)
     */
    public static void shutdown() {
        executor.shutdown();
    }
}