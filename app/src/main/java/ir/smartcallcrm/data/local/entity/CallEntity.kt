package ir.smartcallcrm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single call captured on this device.
 *
 * De-duplication happens on two levels:
 *  - [clientUuid] is a stable id generated once per captured call; it is what
 *    the backend uses to reject duplicate uploads (idempotent sync).
 *  - the unique index on (deviceUuid, sourceCallId) stops the periodic
 *    re-scan from inserting the same system Call Log row twice.
 */
@Entity(
    tableName = "calls",
    indices = [
        Index(value = ["clientUuid"], unique = true),
        Index(value = ["deviceUuid", "sourceCallId"], unique = true),
        Index(value = ["numberNormalized"]),
        Index(value = ["synced"]),
        Index(value = ["startedAt"])
    ]
)
data class CallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientUuid: String,
    /** CallLog.Calls._ID on the source device (may repeat across devices). */
    val sourceCallId: Long,
    val deviceUuid: String,
    val rawNumber: String,
    val numberNormalized: String,
    val contactName: String?,
    /** android.provider.CallLog.Calls.TYPE */
    val type: Int,
    val startedAt: Long,
    val durationSec: Long,
    val phoneAccountId: String?,
    /** WORK | PERSONAL | UNSET */
    val category: String = "UNSET",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
