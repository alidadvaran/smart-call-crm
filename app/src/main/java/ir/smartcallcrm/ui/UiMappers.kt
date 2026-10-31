package ir.smartcallcrm.ui

import android.provider.CallLog
import androidx.compose.ui.graphics.Color
import ir.smartcallcrm.R

/** Maps domain codes to Persian labels (string resources) and accent colors. */
object Labels {

    fun callTypeRes(type: Int): Int = when (type) {
        CallLog.Calls.INCOMING_TYPE -> R.string.call_incoming
        CallLog.Calls.OUTGOING_TYPE -> R.string.call_outgoing
        CallLog.Calls.MISSED_TYPE -> R.string.call_missed
        CallLog.Calls.REJECTED_TYPE -> R.string.call_rejected
        CallLog.Calls.BLOCKED_TYPE -> R.string.call_blocked
        CallLog.Calls.VOICEMAIL_TYPE -> R.string.call_voicemail
        else -> R.string.call_unknown
    }

    fun callTypeColor(type: Int): Color = when (type) {
        CallLog.Calls.INCOMING_TYPE -> Color(0xFF2E7D32)
        CallLog.Calls.OUTGOING_TYPE -> Color(0xFF1565C0)
        CallLog.Calls.MISSED_TYPE -> Color(0xFFC62828)
        CallLog.Calls.REJECTED_TYPE -> Color(0xFFAD1457)
        else -> Color(0xFF616161)
    }

    val statuses = listOf("NEW", "CONTACTED", "QUALIFIED", "FOLLOWUP", "WON", "LOST")

    fun statusRes(status: String): Int = when (status) {
        "NEW" -> R.string.cust_status_new
        "CONTACTED" -> R.string.cust_status_contacted
        "QUALIFIED" -> R.string.cust_status_qualified
        "FOLLOWUP" -> R.string.cust_status_followup
        "WON" -> R.string.cust_status_won
        "LOST" -> R.string.cust_status_lost
        else -> R.string.cust_status_new
    }

    fun statusColor(status: String): Color = when (status) {
        "NEW" -> Color(0xFF616161)
        "CONTACTED" -> Color(0xFF1565C0)
        "QUALIFIED" -> Color(0xFF6A1B9A)
        "FOLLOWUP" -> Color(0xFFE8A13A)
        "WON" -> Color(0xFF2E7D32)
        "LOST" -> Color(0xFFC62828)
        else -> Color(0xFF616161)
    }
}
