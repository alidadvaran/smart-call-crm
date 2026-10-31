package ir.smartcallcrm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.smartcallcrm.core.AdminAuth
import ir.smartcallcrm.core.SecureStore

/**
 * In-memory unlock flag. Resets when the app process is killed, so the
 * password is required again on a fresh launch.
 */
object AdminSession {
    var unlocked by mutableStateOf(false)
}

/** Shows [content] only after the admin password has been entered correctly. */
@Composable
fun AdminGate(secure: SecureStore, content: @Composable () -> Unit) {
    if (AdminSession.unlocked) {
        content()
        return
    }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            "این بخش قفل است",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            "برای تغییر تنظیمات، خاموش‌کردن همگام‌سازی یا حذف اطلاعات، رمز عبور را وارد کنید.",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = false },
            label = { Text("رمز عبور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = error,
            modifier = Modifier.fillMaxWidth()
        )
        if (error) {
            Text("رمز نادرست است", color = Color(0xFFC62828), modifier = Modifier.padding(top = 6.dp))
        }
        Button(
            onClick = {
                if (AdminAuth.verify(secure, input)) AdminSession.unlocked = true else error = true
            },
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("ورود") }
    }
}
