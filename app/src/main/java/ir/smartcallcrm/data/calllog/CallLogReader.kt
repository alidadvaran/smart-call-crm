package ir.smartcallcrm.data.calllog

import android.content.Context
import android.provider.CallLog

/** Raw row straight from the system Call Log provider. */
data class RawCall(
    val sourceId: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val dateMillis: Long,
    val durationSec: Long,
    val phoneAccountId: String?
)

/**
 * Reads the system Call Log. Supports an incremental scan: pass the highest
 * sourceId already stored and only newer rows are returned, which keeps the
 * periodic background scan cheap.
 */
object CallLogReader {

    fun read(context: Context, sinceSourceId: Long = -1, limit: Int = 2000): List<RawCall> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val selection = if (sinceSourceId >= 0) "${CallLog.Calls._ID} > ?" else null
        val args = if (sinceSourceId >= 0) arrayOf(sinceSourceId.toString()) else null

        val out = ArrayList<RawCall>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args,
            "${CallLog.Calls.DATE} DESC"
        ) ?: return out

        cursor.use { c ->
            val iId = c.getColumnIndex(CallLog.Calls._ID)
            val iNum = c.getColumnIndex(CallLog.Calls.NUMBER)
            val iName = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val iType = c.getColumnIndex(CallLog.Calls.TYPE)
            val iDate = c.getColumnIndex(CallLog.Calls.DATE)
            val iDur = c.getColumnIndex(CallLog.Calls.DURATION)
            val iAcc = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
            var n = 0
            while (c.moveToNext() && n < limit) {
                out.add(
                    RawCall(
                        sourceId = if (iId >= 0) c.getLong(iId) else 0L,
                        number = (if (iNum >= 0) c.getString(iNum) else null) ?: "",
                        name = if (iName >= 0) c.getString(iName)?.ifBlank { null } else null,
                        type = if (iType >= 0) c.getInt(iType) else 0,
                        dateMillis = if (iDate >= 0) c.getLong(iDate) else 0L,
                        durationSec = if (iDur >= 0) c.getLong(iDur) else 0L,
                        phoneAccountId = if (iAcc >= 0) c.getString(iAcc)?.ifBlank { null } else null
                    )
                )
                n++
            }
        }
        return out
    }
}
