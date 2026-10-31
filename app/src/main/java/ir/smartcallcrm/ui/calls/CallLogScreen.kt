package ir.smartcallcrm.ui.calls

import android.provider.CallLog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.PhoneUtils
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.data.local.entity.CallEntity
import ir.smartcallcrm.ui.CallsViewModel
import ir.smartcallcrm.ui.Labels
import ir.smartcallcrm.ui.RangeChips

private val typeFilters = listOf(
    -1 to "همه",
    CallLog.Calls.INCOMING_TYPE to "ورودی",
    CallLog.Calls.OUTGOING_TYPE to "خروجی",
    CallLog.Calls.MISSED_TYPE to "ازدست‌رفته"
)

@Composable
fun CallLogScreen(vm: CallsViewModel) {
    val calls by vm.calls.collectAsStateWithLifecycle()
    val range by vm.rangeDays.collectAsStateWithLifecycle()
    val type by vm.typeFilter.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        RangeChips(selected = range) { vm.setRange(it) }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            typeFilters.forEach { (value, label) ->
                FilterChip(
                    selected = type == value,
                    onClick = { vm.setType(value) },
                    label = { Text(label) }
                )
            }
        }
        if (calls.isEmpty()) {
            Text("تماسی در این بازه نیست.", Modifier.padding(16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(calls, key = { it.id }) { call ->
                    CallRow(call) { next -> vm.setCategory(call.id, next) }
                }
            }
        }
    }
}

@Composable
private fun CallRow(call: CallEntity, onCategory: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    call.contactName?.takeIf { it.isNotBlank() }
                        ?: PhoneUtils.display(call.rawNumber),
                    fontWeight = FontWeight.SemiBold
                )
                Text(TimeUtils.toPersianDigits(call.rawNumber), fontSize = 13.sp)
                Text(TimeUtils.formatJalaliDateTime(call.startedAt), fontSize = 12.sp)
                call.phoneAccountId?.let {
                    Text("سیم‌کارت: ${TimeUtils.toPersianDigits(it)}", fontSize = 11.sp)
                }
                CategoryChip(call.category) { onCategory(it) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(Labels.callTypeRes(call.type)),
                    color = Labels.callTypeColor(call.type),
                    fontWeight = FontWeight.SemiBold
                )
                Text("مدت: ${TimeUtils.formatDuration(call.durationSec)}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryChip(current: String, onNext: (String) -> Unit) {
    val (label, color) = when (current) {
        "WORK" -> "کاری" to Color(0xFF1565C0)
        "PERSONAL" -> "شخصی" to Color(0xFF6A1B9A)
        else -> "بدون دسته" to Color(0xFF9E9E9E)
    }
    AssistChip(
        onClick = {
            onNext(when (current) {
                "UNSET" -> "WORK"
                "WORK" -> "PERSONAL"
                else -> "UNSET"
            })
        },
        label = { Text(label, color = color, fontSize = 12.sp) }
    )
}
