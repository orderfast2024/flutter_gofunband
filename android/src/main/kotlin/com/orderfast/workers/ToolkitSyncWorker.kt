package com.orderfast.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.easygoband.toolkit.sdk.core.transaction.transaction.data.SyncTransactionsMode
import com.orderfast.ToolkitProvider

class ToolkitSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "ToolkitSyncWorker"
        const val WORK_NAME = "toolkit_periodic_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting toolkit sync...")

            val toolkit = ToolkitProvider.getToolkit()

            if (toolkit == null) {
                Log.w(TAG, "Toolkit not initialized, retrying later")
                return Result.retry()
            }

            if (!toolkit.instance().isDeviceConfigured()) {
                Log.w(TAG, "Device not configured, retrying later")
                return Result.retry()
            }

            // Ejecuta la sincronización
            toolkit.instance().sync(
                false,
                SyncTransactionsMode.DEVICE,
                true
            )

            Log.i(TAG, "Sync completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)

            // Retry si es un error transitorio y no hemos intentado muchas veces
            if (runAttemptCount < 3) {
                Log.i(TAG, "Retrying sync (attempt ${runAttemptCount + 1})")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, failing")
                Result.failure()
            }
        }
    }
}