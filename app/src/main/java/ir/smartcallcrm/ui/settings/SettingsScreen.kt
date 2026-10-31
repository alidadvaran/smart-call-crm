package ir.smartcallcrm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.ui.SectionTitle
import ir.smartcallcrm.ui.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val context = LocalContext.current
    val serverUrl by vm.serverUrl.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val autoSync by vm.autoSync.collectAsStateWithLifecycle()
    val splitWork by vm.splitWork.collectAsStateWithLifecycle()
    var urlDraft by remember(serverUrl) { mutableStateOf(serverUrl) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var passwordMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("سرور")
        OutlinedTextField(
            value = urlDraft,
            onValueChange = { urlDraft = it },
            label = { Text("آدرس سرور (https://...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { vm.setServerUrl(urlDraft.trim()) }) { Text("ذخیره آدرس") }

        SectionTitle("همگام‌سازی")
        ToggleRow("همگام‌سازی خودکار", autoSync) { vm.setAutoSync(it) }
        ToggleRow("فقط با وای‌فای", wifiOnly) { vm.setWifiOnly(it) }
        ToggleRow("تفکیک تماس‌های کاری و شخصی", splitWork) { vm.setSplitWork(it) }
        Button(onClick = { vm.syncNow(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("همگام‌سازی دستی")
        }

        SectionTitle("رمز عبور مدیریت")
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; passwordMsg = null },
            label = { Text("رمز عبور جدید") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (newPassword.length < 4) {
                    passwordMsg = "رمز باید حداقل ۴ کاراکتر باشد"
                } else {
                    vm.changeAdminPassword(newPassword)
                    newPassword = ""
                    passwordMsg = "رمز عبور تغییر کرد"
                }
            }
        ) { Text("تغییر رمز عبور") }
        passwordMsg?.let { Text(it, color = MaterialTheme.colorScheme.outline) }

        SectionTitle("حریم خصوصی")
        Card(Modifier.fillMaxWidth()) {
            Text(
                "صدای مکالمات ضبط نمی‌شود. داده‌ها فقط روی دستگاه و سرور شخصی شما ذخیره می‌شوند. برنامه به Root نیاز ندارد.",
                Modifier.padding(16.dp)
            )
        }
        OutlinedButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
        ) { Text("حذف کامل اطلاعات محلی") }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = { vm.deleteAllData(); confirmDelete = false }) {
                    Text("حذف", color = Color(0xFFC62828))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
            title = { Text("حذف اطلاعات") },
            text = { Text("همه داده‌های محلی (تماس‌ها، مشتریان، توکن) حذف شوند؟ این عمل بازگشت‌ناپذیر است.") }
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
