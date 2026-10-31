package ir.smartcallcrm.sync

import android.content.Context
import ir.smartcallcrm.BuildConfig
import ir.smartcallcrm.core.SecureStore
import ir.smartcallcrm.core.SettingsStore
import ir.smartcallcrm.data.remote.ApiClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Checks the backend for a newer APK and, if found, downloads and installs it.
 * Install is silent when the app is device owner, otherwise it shows one
 * system confirmation (see [Installer]).
 */
object UpdateManager {

    suspend fun checkAndMaybeUpdate(context: Context): Boolean = withContext(Dispatchers.IO) {
        val settings = SettingsStore(context)
        val secure = SecureStore(context)
        if (!secure.isRegistered) return@withContext false

        val api = ApiClientFactory.create(settings.serverUrl()) { secure.authToken }
        val latest = try {
            api.latestRelease()
        } catch (_: Exception) {
            return@withContext false
        }

        val url = latest.url
        if (latest.versionCode <= BuildConfig.VERSION_CODE || url.isNullOrBlank()) {
            return@withContext false
        }

        val apkFile = File(context.cacheDir, "update-${latest.versionCode}.apk")
        try {
            val body = api.downloadApk(url)
            body.byteStream().use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (_: Exception) {
            apkFile.delete()
            return@withContext false
        }

        try {
            Installer.install(context, apkFile)
            true
        } catch (_: Exception) {
            false
        }
    }
}
