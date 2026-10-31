package ir.smartcallcrm.ui.followups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.ui.FollowUpsViewModel

@Composable
fun FollowUpsScreen(vm: FollowUpsViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("" to "همه", "PENDING" to "معوق", "DONE" to "انجام‌شده").forEach { (v, l) ->
                FilterChip(selected = filter == v, onClick = { vm.setFilter(v) }, label = { Text(l) })
            }
        }
        if (items.isEmpty()) {
            Text("پیگیری‌ای ثبت نشده.", Modifier.padding(16.dp))
        } else {
            LazyColumn(
                Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { f ->
                    val overdue = f.status == "PENDING" && f.dueAt < now
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(f.customerName, fontWeight = FontWeight.SemiBold)
                                Text(f.note, fontSize = 13.sp)
                                Text(
                                    "سررسید: ${TimeUtils.formatJalaliDate(f.dueAt)}" +
                                        if (overdue) "  (عقب‌افتاده)" else "",
                                    fontSize = 12.sp,
                                    color = if (overdue) Color(0xFFC62828) else MaterialTheme.colorScheme.outline
                                )
                            }
                            if (f.status == "PENDING") {
                                TextButton(onClick = { vm.complete(f.id) }) { Text("انجام شد") }
                            } else {
                                Text("✔", color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}
