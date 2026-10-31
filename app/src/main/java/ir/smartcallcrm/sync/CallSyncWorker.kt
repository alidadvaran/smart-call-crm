package ir.smartcallcrm.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.smartcallcrm.SmartCallApp
import kotlinx.coroutines.flow.first

/**
 * The heart of background capture: scans the Call Log for new calls, stores
 * them offline, then tries to sync. Runs periodically and on demand. It is
 * resilient — if the permission is missing or the network is down it still
 * does whatever part it can and succeeds, so WorkManager keeps the schedule.
 */
class CallSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SmartCallApp
        val container = app.container

        val hasCallLog = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        // 1) Capture new calls (offline-safe).
        if (hasCallLog) {
            try {
                val deviceUuid = container.settings.deviceUuid()
                container.callRepository.scanNewCalls(applicationContext, deviceUuid)
            } catch (_: Exception) {
                // A capture error must not abort the whole job; sync can still run.
            }
        }

        // 2) Sync if enabled (errors just leave data queued for next run).
        val autoSync = container.settings.autoSyncFlow.first()
        if (autoSync || inputData.getBoolean(KEY_FORCE_SYNC, false)) {
            runCatching { container.syncRepository.syncAll() }
                .getOrNull()
                ?.let { if (!it.success && isRetryRun()) return Result.retry() }
        }

        // Check for and apply an app update (silent if device owner).
        runCatching { UpdateManager.checkAndMaybeUpdate(applicationContext) }

        return Result.success()
    }

    private fun isRetryRun() = runAttemptCount < 4

    companion object {
        const val KEY_FORCE_SYNC = "force_sync"
    }
}
