package ir.smartcallcrm.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.smartcallcrm.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "smartcall_settings")

/**
 * Non-secret app state: device identity (uuid/name/role), server URL, sync
 * preferences, last successful sync time, and the report recipient. The auth
 * token lives in [SecureStore], not here.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val UUID = stringPreferencesKey("device_uuid")
        val NAME = stringPreferencesKey("device_name")
        val ROLE = stringPreferencesKey("device_role")
        val SERVER = stringPreferencesKey("server_url")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val SPLIT_WORK = booleanPreferencesKey("split_work_personal")
        val RECIPIENT = stringPreferencesKey("report_recipient")
        val LAST_SYNC = longPreferencesKey("last_sync_success")
    }

    companion object {
        const val ROLE_PRIMARY = "PRIMARY"
        const val ROLE_SECONDARY = "SECONDARY"
        const val DEFAULT_RECIPIENT = "a.dadvaran@gmail.com"
    }

    val nameFlow: Flow<String> = context.dataStore.data.map { it[Keys.NAME] ?: "گوشی من" }
    val roleFlow: Flow<String> = context.dataStore.data.map { it[Keys.ROLE] ?: ROLE_PRIMARY }
    val serverUrlFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.SERVER] ?: BuildConfig.DEFAULT_SERVER_URL }
    val wifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: false }
    val autoSyncFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SYNC] ?: true }
    val splitWorkFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.SPLIT_WORK] ?: false }
    val recipientFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.RECIPIENT] ?: DEFAULT_RECIPIENT }
    val lastSyncFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    /** Create the device UUID on first access and keep it stable thereafter. */
    suspend fun deviceUuid(): String {
        context.dataStore.data.first()[Keys.UUID]?.let { return it }
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.UUID] = id }
        return id
    }

    suspend fun deviceName(): String = context.dataStore.data.first()[Keys.NAME] ?: "گوشی من"
    suspend fun serverUrl(): String =
        context.dataStore.data.first()[Keys.SERVER] ?: BuildConfig.DEFAULT_SERVER_URL
    suspend fun role(): String = context.dataStore.data.first()[Keys.ROLE] ?: ROLE_PRIMARY
    suspend fun wifiOnly(): Boolean = context.dataStore.data.first()[Keys.WIFI_ONLY] ?: false
    suspend fun recipient(): String =
        context.dataStore.data.first()[Keys.RECIPIENT] ?: DEFAULT_RECIPIENT

    suspend fun setName(v: String) = context.dataStore.edit { it[Keys.NAME] = v }.let {}
    suspend fun setRole(v: String) = context.dataStore.edit { it[Keys.ROLE] = v }.let {}
    suspend fun setServerUrl(v: String) = context.dataStore.edit { it[Keys.SERVER] = v }.let {}
    suspend fun setWifiOnly(v: Boolean) = context.dataStore.edit { it[Keys.WIFI_ONLY] = v }.let {}
    suspend fun setAutoSync(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_SYNC] = v }.let {}
    suspend fun setSplitWork(v: Boolean) = context.dataStore.edit { it[Keys.SPLIT_WORK] = v }.let {}
    suspend fun setRecipient(v: String) = context.dataStore.edit { it[Keys.RECIPIENT] = v }.let {}
    suspend fun setLastSync(ts: Long) = context.dataStore.edit { it[Keys.LAST_SYNC] = ts }.let {}
}
