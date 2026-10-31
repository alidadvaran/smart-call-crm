package ir.smartcallcrm.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.ui.RangeChips
import ir.smartcallcrm.ui.StatTile
import ir.smartcallcrm.ui.SectionTitle

@Composable
fun DashboardScreen(vm: ir.smartcallcrm.ui.DashboardViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RangeChips(selected = s.rangeDays) { vm.setRange(it) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("کل تماس‌ها", TimeUtils.toPersianDigits(s.total.toString()), Modifier.weight(1f))
            StatTile("مجموع مکالمه", TimeUtils.formatDuration(s.talkSeconds), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("ورودی", TimeUtils.toPersianDigits(s.incoming.toString()), Modifier.weight(1f))
            StatTile("خروجی", TimeUtils.toPersianDigits(s.outgoing.toString()), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("ازدست‌رفته", TimeUtils.toPersianDigits(s.missed.toString()), Modifier.weight(1f))
            StatTile("مشتریان جدید", TimeUtils.toPersianDigits(s.newCustomers.toString()), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("پیگیری معوق", TimeUtils.toPersianDigits(s.pendingFollowUps.toString()), Modifier.weight(1f))
            StatTile("در صف ارسال", TimeUtils.toPersianDigits(s.pendingSync.toString()), Modifier.weight(1f))
        }

        SectionTitle("پرتکرارترین شماره‌ها")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                if (s.topNumbers.isEmpty()) {
                    Text("داده‌ای نیست", color = MaterialTheme.colorScheme.outline)
                }
                s.topNumbers.forEachIndexed { i, n ->
                    if (i > 0) HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = n.name?.takeIf { it.isNotBlank() }
                                ?: TimeUtils.toPersianDigits(n.number.ifBlank { "ناشناس" }),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${TimeUtils.toPersianDigits(n.count.toString())} تماس",
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
