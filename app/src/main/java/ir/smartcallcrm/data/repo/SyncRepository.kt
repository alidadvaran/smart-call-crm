package ir.smartcallcrm.data.repo

import ir.smartcallcrm.core.SecureStore
import ir.smartcallcrm.core.SettingsStore
import ir.smartcallcrm.data.local.dao.CallDao
import ir.smartcallcrm.data.local.dao.CustomerDao
import ir.smartcallcrm.data.remote.ApiClientFactory
import ir.smartcallcrm.data.remote.dto.CallDto
import ir.smartcallcrm.data.remote.dto.CustomerDto
import ir.smartcallcrm.data.remote.dto.RegisterDeviceRequest
import ir.smartcallcrm.data.remote.dto.ReportRequest
import ir.smartcallcrm.data.remote.dto.UploadCallsRequest
import ir.smartcallcrm.data.remote.dto.UploadCustomersRequest

data class SyncResult(
    val success: Boolean,
    val pushedCalls: Int = 0,
    val duplicates: Int = 0,
    val error: String? = null
)

/**
 * Pushes locally-captured data to the central backend. Idempotent: the backend
 * dedups on clientUuid, and only rows still marked unsynced are sent, so a
 * retried or repeated sync never double-counts.
 */
class SyncRepository(
    private val settings: SettingsStore,
    private val secure: SecureStore,
    private val callDao: CallDao,
    private val customerDao: CustomerDao
) {
    private val batch = 200

    private suspend fun service() =
        ApiClientFactory.create(settings.serverUrl()) { secure.authToken }

    /** Register this device with the backend if it has no token yet. */
    suspend fun ensureRegistered(): Boolean {
        if (secure.isRegistered) return true
        return try {
            val resp = service().registerDevice(
                RegisterDeviceRequest(
                    deviceUuid = settings.deviceUuid(),
                    name = settings.deviceName(),
                    role = settings.role()
                )
            )
            secure.authToken = resp.token
            secure.serverDeviceId = resp.deviceId
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncAll(): SyncResult {
        if (!ensureRegistered()) {
            return SyncResult(false, error = "ثبت دستگاه ناموفق بود")
        }
        val deviceUuid = settings.deviceUuid()
        var pushed = 0
        var dups = 0
        try {
            val api = service()

            // Calls
            while (true) {
                val chunk = callDao.unsynced(batch)
                if (chunk.isEmpty()) break
                val resp = api.uploadCalls(
                    UploadCallsRequest(
                        deviceUuid = deviceUuid,
                        calls = chunk.map { it.toDto() }
                    )
                )
                callDao.markSynced(chunk.map { it.clientUuid })
                pushed += resp.accepted
                dups += resp.duplicates
                if (chunk.size < batch) break
            }

            // Customers
            while (true) {
                val chunk = customerDao.unsynced(batch)
                if (chunk.isEmpty()) break
                api.uploadCustomers(UploadCustomersRequest(chunk.map { it.toDto() }))
                customerDao.markSynced(chunk.map { it.id })
                if (chunk.size < batch) break
            }

            settings.setLastSync(System.currentTimeMillis())
            return SyncResult(true, pushedCalls = pushed, duplicates = dups)
        } catch (e: Exception) {
            return SyncResult(false, pushedCalls = pushed, duplicates = dups, error = e.message)
        }
    }

    /**
     * Ask the backend to generate and email a report. The email is sent from
     * the server (SMTP credentials live there, never in the app). We sync
     * first so the report reflects the latest data.
     */
    suspend fun sendReport(type: String): Boolean {
        if (!ensureRegistered()) return false
        return try {
            syncAll()
            service().sendReport(ReportRequest(type = type, recipient = settings.recipient())).ok
        } catch (e: Exception) {
            false
        }
    }
}

private fun ir.smartcallcrm.data.local.entity.CallEntity.toDto() = CallDto(
    clientUuid = clientUuid,
    sourceId = sourceCallId,
    number = rawNumber,
    numberNormalized = numberNormalized,
    contactName = contactName,
    type = type,
    startedAt = startedAt,
    durationSec = durationSec,
    phoneAccountId = phoneAccountId,
    category = category
)

private fun ir.smartcallcrm.data.local.entity.CustomerEntity.toDto() = CustomerDto(
    numberNormalized = numberNormalized,
    displayName = displayName,
    company = company,
    status = status,
    notes = notes,
    lastCallAt = lastCallAt,
    callCount = callCount
)
