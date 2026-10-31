package ir.smartcallcrm.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.SettingsStore
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.ui.DevicesViewModel
import ir.smartcallcrm.ui.SectionTitle

@Composable
fun DevicesScreen(vm: DevicesViewModel) {
    val name by vm.name.collectAsStateWithLifecycle()
    val role by vm.role.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val registered by vm.registered.collectAsStateWithLifecycle()
    var editName by remember(name) { mutableStateOf(name) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("این دستگاه")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("نام دستگاه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("نقش دستگاه:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == SettingsStore.ROLE_PRIMARY,
                        onClick = { vm.setRole(SettingsStore.ROLE_PRIMARY) },
                        label = { Text("گوشی اصلی") }
                    )
                    FilterChip(
                        selected = role == SettingsStore.ROLE_SECONDARY,
                        onClick = { vm.setRole(SettingsStore.ROLE_SECONDARY) },
                        label = { Text("گوشی دوم") }
                    )
                }
                Button(onClick = { vm.setName(editName) }) { Text("ذخیره نام") }
                Text(
                    "وضعیت ثبت در سرور: " + if (registered) "ثبت‌شده" else "ثبت‌نشده",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "آخرین همگام‌سازی: " +
                        if (lastSync > 0) TimeUtils.formatJalaliDateTime(lastSync) else "هرگز",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Text(
                "هر گوشی به‌صورت مستقل تماس‌های خود را ثبت و ذخیره می‌کند. گزارش تجمیعی هر دو گوشی در داشبورد مرکزی (وب) قابل مشاهده است؛ شماره‌ی مشترک بین دو گوشی یک پروفایل مشتری واحد می‌سازد ولی تماس‌ها جداگانه می‌مانند.",
                Modifier.padding(16.dp),
                fontSize = 13.sp
            )
        }
    }
}
