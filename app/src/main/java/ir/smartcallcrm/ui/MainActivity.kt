package ir.smartcallcrm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.smartcallcrm.SmartCallApp
import ir.smartcallcrm.sync.SyncScheduler
import ir.smartcallcrm.sync.UpdateManager
import ir.smartcallcrm.ui.calls.CallLogScreen
import ir.smartcallcrm.ui.customers.CustomersScreen
import ir.smartcallcrm.ui.dashboard.DashboardScreen
import ir.smartcallcrm.ui.devices.DevicesScreen
import ir.smartcallcrm.ui.followups.FollowUpsScreen
import ir.smartcallcrm.ui.reports.ReportsScreen
import ir.smartcallcrm.ui.settings.SettingsScreen
import ir.smartcallcrm.ui.theme.SmartCallTheme

private enum class Dest(val route: String, val title: String, val icon: ImageVector, val inBar: Boolean) {
    DASHBOARD("dashboard", "داشبورد", Icons.Filled.Dashboard, true),
    CALLS("calls", "تماس‌ها", Icons.Filled.Call, true),
    CUSTOMERS("customers", "مشتریان", Icons.Filled.People, true),
    FOLLOWUPS("followups", "پیگیری‌ها", Icons.Filled.Checklist, true),
    REPORTS("reports", "گزارش‌ها", Icons.Filled.Assessment, true),
    DEVICES("devices", "دستگاه‌ها", Icons.Filled.Devices, false),
    SETTINGS("settings", "تنظیمات", Icons.Filled.Settings, false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartCallTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as SmartCallApp
    val factory = remember { AppViewModelFactory(app) }

    // Check for an app update when the UI opens.
    LaunchedEffect(Unit) {
        runCatching { UpdateManager.checkAndMaybeUpdate(context) }
    }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_CALL_LOG] == true) {
            granted = true
            SyncScheduler.scanNow(context)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!granted) {
            PermissionGate {
                val perms = mutableListOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_STATE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms += Manifest.permission.POST_NOTIFICATIONS
                }
                launcher.launch(perms.toTypedArray())
            }
        } else {
            MainScaffold(factory, app)
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("دسترسی به گزارش تماس", style = MaterialTheme.typography.headlineSmall)
        Text(
            "این برنامه برای ثبت سوابق تماس شما به مجوز «گزارش تماس» و «مخاطبین» نیاز دارد. " +
                "داده‌ها فقط روی دستگاه و سرور شخصی شما ذخیره می‌شوند و صدای مکالمه ضبط نمی‌شود.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        androidx.compose.material3.Button(onClick = onRequest) {
            Text("اعطای مجوز و شروع")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(factory: AppViewModelFactory, app: SmartCallApp) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val currentDest = Dest.entries.firstOrNull { d ->
        current?.hierarchy?.any { it.route == d.route } == true
    } ?: Dest.DASHBOARD

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDest.title) },
                actions = {
                    IconButton(onClick = { navController.navigateSingleTop(Dest.DEVICES.route) }) {
                        Icon(Icons.Filled.Devices, contentDescription = "دستگاه‌ها")
                    }
                    IconButton(onClick = { navController.navigateSingleTop(Dest.SETTINGS.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Dest.entries.filter { it.inBar }.forEach { d ->
                    NavigationBarItem(
                        selected = currentDest == d,
                        onClick = { navController.navigateSingleTop(d.route) },
                        icon = { Icon(d.icon, contentDescription = d.title) },
                        label = { Text(d.title) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Dest.DASHBOARD.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Dest.DASHBOARD.route) { DashboardScreen(viewModel(factory = factory)) }
            composable(Dest.CALLS.route) { CallLogScreen(viewModel(factory = factory)) }
            composable(Dest.CUSTOMERS.route) { CustomersScreen(viewModel(factory = factory)) }
            composable(Dest.FOLLOWUPS.route) { FollowUpsScreen(viewModel(factory = factory)) }
            composable(Dest.REPORTS.route) { ReportsScreen(viewModel(factory = factory)) }
            composable(Dest.DEVICES.route) { DevicesScreen(viewModel(factory = factory)) }
            composable(Dest.SETTINGS.route) {
                AdminGate(app.container.secure) { SettingsScreen(viewModel(factory = factory)) }
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
