package ir.smartcallcrm.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val PERIODIC = "smartcall_periodic_scan_sync"
    private const val ONE_SHOT = "smartcall_manual_sync"

    /**
     * Periodic scan+sync. 15 minutes is the Android minimum for periodic work.
     * No network constraint, so call capture happens even offline; the worker
     * itself decides whether it can sync.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<CallSyncWorker>(15, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** User-triggered immediate sync; requires connectivity. */
    fun triggerNow(context: Context, wifiOnly: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<CallSyncWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putBoolean(CallSyncWorker.KEY_FORCE_SYNC, true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_SHOT, ExistingWorkPolicy.REPLACE, request)
    }

    /** Immediate capture with no network requirement (e.g. right after the
     *  user grants the call-log permission). */
    fun scanNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CallSyncWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("smartcall_scan_now", ExistingWorkPolicy.REPLACE, request)
    }

    /** Fired right after boot: capture immediately and keep the schedule alive. */
    fun onBoot(context: Context) {
        schedulePeriodic(context)
        val request = OneTimeWorkRequestBuilder<CallSyncWorker>()
            .setInitialDelay(20, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("smartcall_boot_scan", ExistingWorkPolicy.KEEP, request)
    }
}
