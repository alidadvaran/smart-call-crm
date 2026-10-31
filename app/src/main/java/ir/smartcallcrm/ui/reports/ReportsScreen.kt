package ir.smartcallcrm.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.ui.ReportsViewModel
import ir.smartcallcrm.ui.SectionTitle

@Composable
fun ReportsScreen(vm: ReportsViewModel) {
    val recipient by vm.recipient.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("ایمیل گیرنده گزارش")
        OutlinedTextField(
            value = recipient,
            onValueChange = { vm.setRecipient(it) },
            label = { Text("ایمیل گیرنده") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "ایمیل از سمت سرور امن ارسال می‌شود؛ رمز/کلید ایمیل هرگز در برنامه ذخیره نمی‌شود.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )

        SectionTitle("ارسال گزارش")
        Button(onClick = { vm.send("DAILY") }, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
            Text("گزارش روزانه")
        }
        Button(onClick = { vm.send("WEEKLY") }, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
            Text("گزارش هفتگی")
        }
        Button(onClick = { vm.send("MANUAL") }, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
            Text("گزارش دستی (اکنون)")
        }

        if (sending) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("در حال همگام‌سازی و ارسال…", Modifier.padding(top = 8.dp))
                }
            }
        }
        message?.let {
            Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(16.dp)) }
        }

        Text(
            "آخرین همگام‌سازی موفق: " +
                if (lastSync > 0) TimeUtils.formatJalaliDateTime(lastSync) else "هرگز",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
