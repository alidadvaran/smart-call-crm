package ir.smartcallcrm.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms scanning after a reboot or an app update so new calls are not missed
 * while the UI has never been opened.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> SyncScheduler.onBoot(context.applicationContext)
        }
    }
}
