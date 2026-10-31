package ir.smartcallcrm.data.repo

import android.content.Context
import ir.smartcallcrm.core.PhoneUtils
import ir.smartcallcrm.data.calllog.CallLogReader
import ir.smartcallcrm.data.local.dao.CallDao
import ir.smartcallcrm.data.local.dao.CustomerDao
import ir.smartcallcrm.data.local.dao.NumberStat
import ir.smartcallcrm.data.local.dao.TypeCount
import ir.smartcallcrm.data.local.entity.CallEntity
import ir.smartcallcrm.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Owns capturing calls from the system log into Room and keeping the derived
 * customer profiles in step. Reading the system log requires READ_CALL_LOG.
 */
class CallRepository(
    private val callDao: CallDao,
    private val customerDao: CustomerDao
) {

    /**
     * Scan the device Call Log for rows newer than what we already stored and
     * persist them. Returns the number of genuinely new calls inserted.
     */
    suspend fun scanNewCalls(context: Context, deviceUuid: String): Int {
        val since = callDao.maxSourceCallId(deviceUuid)
        val raw = CallLogReader.read(context, sinceSourceId = since)
        var inserted = 0
        for (r in raw) {
            val normalized = PhoneUtils.normalize(r.number)
            val entity = CallEntity(
                clientUuid = UUID.randomUUID().toString(),
                sourceCallId = r.sourceId,
                deviceUuid = deviceUuid,
                rawNumber = r.number,
                numberNormalized = normalized,
                contactName = r.name,
                type = r.type,
                startedAt = r.dateMillis,
                durationSec = r.durationSec,
                phoneAccountId = r.phoneAccountId
            )
            val id = callDao.insertIgnore(entity)
            if (id != -1L) {
                inserted++
                if (normalized.isNotBlank()) {
                    linkCustomer(normalized, r.name, r.number, r.dateMillis)
                }
            }
        }
        return inserted
    }

    private suspend fun linkCustomer(number: String, name: String?, raw: String, callAt: Long) {
        val display = name?.takeIf { it.isNotBlank() } ?: PhoneUtils.display(raw)
        customerDao.insertIgnore(
            CustomerEntity(
                numberNormalized = number,
                displayName = display,
                lastCallAt = callAt,
                callCount = 0
            )
        )
        customerDao.registerCall(number, callAt)
    }

    fun observeFiltered(device: String?, since: Long, type: Int, limit: Int = 500): Flow<List<CallEntity>> =
        callDao.observeFiltered(device, since, type, limit)

    fun observePendingSync(): Flow<Int> = callDao.observePendingCount()

    fun observeTypeCounts(device: String?, since: Long): Flow<List<TypeCount>> =
        callDao.observeTypeCounts(device, since)

    fun observeTotalTalk(device: String?, since: Long): Flow<Long> =
        callDao.observeTotalTalk(device, since)

    fun observeTotal(device: String?, since: Long): Flow<Int> =
        callDao.observeTotal(device, since)

    fun observeTopNumbers(device: String?, since: Long, limit: Int = 8): Flow<List<NumberStat>> =
        callDao.observeTopNumbers(device, since, limit)

    suspend fun setCategory(id: Long, category: String) = callDao.setCategory(id, category)

    suspend fun deleteAll() = callDao.deleteAll()
}
