package ir.smartcallcrm.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for the device's auth token and server-assigned device id.
 * Backed by Android Keystore via EncryptedSharedPreferences, so the token is
 * never written to disk in clear text and is excluded from backups.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var serverDeviceId: String?
        get() = prefs.getString(KEY_SERVER_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_SERVER_DEVICE_ID, value).apply()

    /** SHA-256 hash of the admin password that gates sensitive actions. */
    var adminPasswordHash: String?
        get() = prefs.getString(KEY_ADMIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_ADMIN_HASH, value).apply()

    val isRegistered: Boolean get() = !authToken.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_SERVER_DEVICE_ID = "server_device_id"
        const val KEY_ADMIN_HASH = "admin_pw_hash"
    }
}
