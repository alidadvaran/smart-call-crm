package ir.smartcallcrm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ir.smartcallcrm.core.SecureStore
import ir.smartcallcrm.core.SettingsStore
import ir.smartcallcrm.data.local.AppDatabase
import ir.smartcallcrm.data.repo.CallRepository
import ir.smartcallcrm.data.repo.CustomerRepository
import ir.smartcallcrm.data.repo.SyncRepository
import ir.smartcallcrm.sync.SyncScheduler

/** Lightweight manual DI container — no framework needed for an app this size. */
class AppContainer(app: Application) {
    val database = AppDatabase.get(app)
    val settings = SettingsStore(app)
    val secure: SecureStore by lazy { SecureStore(app) }

    val callRepository = CallRepository(database.callDao(), database.customerDao())
    val customerRepository = CustomerRepository(database.customerDao(), database.followUpDao())
    val syncRepository =
        SyncRepository(settings, secure, database.callDao(), database.customerDao())
}

class SmartCallApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        // Keep the periodic scan+sync scheduled (idempotent — KEEP policy).
        SyncScheduler.schedulePeriodic(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SYNC,
                "همگام‌سازی",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "وضعیت ثبت و ارسال تماس‌ها" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SYNC = "sync_status"
    }
}
