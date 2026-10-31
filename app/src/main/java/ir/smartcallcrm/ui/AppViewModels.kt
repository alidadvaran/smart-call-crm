package ir.smartcallcrm.ui

import android.content.Context
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.smartcallcrm.SmartCallApp
import ir.smartcallcrm.core.SecureStore
import ir.smartcallcrm.core.SettingsStore
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.data.local.dao.FollowUpRow
import ir.smartcallcrm.data.local.dao.NumberStat
import ir.smartcallcrm.data.local.entity.CallEntity
import ir.smartcallcrm.data.local.entity.CustomerEntity
import ir.smartcallcrm.data.repo.CallRepository
import ir.smartcallcrm.data.repo.CustomerRepository
import ir.smartcallcrm.data.repo.SyncRepository
import ir.smartcallcrm.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** since-epoch for a range expressed in days; 0 (or less) means "all time". */
private fun sinceOf(days: Int): Long =
    if (days <= 0) 0L else TimeUtils.startOfDayMillis(days - 1)

// ---------------------------------------------------------------- Dashboard

data class DashboardUiState(
    val rangeDays: Int = 7,
    val total: Int = 0,
    val incoming: Int = 0,
    val outgoing: Int = 0,
    val missed: Int = 0,
    val talkSeconds: Long = 0,
    val newCustomers: Int = 0,
    val pendingFollowUps: Int = 0,
    val pendingSync: Int = 0,
    val topNumbers: List<NumberStat> = emptyList()
)

class DashboardViewModel(
    private val callRepo: CallRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val rangeDays = MutableStateFlow(7)
    fun setRange(days: Int) { rangeDays.value = days }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardUiState> = rangeDays.flatMapLatest { days ->
        val since = sinceOf(days)
        val base = combine(
            callRepo.observeTotal(null, since),
            callRepo.observeTypeCounts(null, since),
            callRepo.observeTotalTalk(null, since),
            customerRepo.observeNewCount(since),
            customerRepo.observePendingFollowUps()
        ) { total, types, talk, newCust, pendingFu ->
            val inc = types.firstOrNull { it.type == CallLog.Calls.INCOMING_TYPE }?.count ?: 0
            val out = types.firstOrNull { it.type == CallLog.Calls.OUTGOING_TYPE }?.count ?: 0
            val miss = types.firstOrNull { it.type == CallLog.Calls.MISSED_TYPE }?.count ?: 0
            DashboardUiState(
                rangeDays = days, total = total, incoming = inc, outgoing = out,
                missed = miss, talkSeconds = talk, newCustomers = newCust,
                pendingFollowUps = pendingFu
            )
        }
        combine(base, callRepo.observePendingSync(), callRepo.observeTopNumbers(null, since)) { s, sync, top ->
            s.copy(pendingSync = sync, topNumbers = top)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}

// -------------------------------------------------------------------- Calls

class CallsViewModel(private val callRepo: CallRepository) : ViewModel() {
    val rangeDays = MutableStateFlow(7)
    val typeFilter = MutableStateFlow(-1)

    fun setRange(days: Int) { rangeDays.value = days }
    fun setType(type: Int) { typeFilter.value = type }
    fun setCategory(id: Long, category: String) = viewModelScope.launch {
        callRepo.setCategory(id, category)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val calls: StateFlow<List<CallEntity>> =
        combine(rangeDays, typeFilter) { d, t -> d to t }
            .flatMapLatest { (d, t) -> callRepo.observeFiltered(null, sinceOf(d), t, 500) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// ---------------------------------------------------------------- Customers

class CustomersViewModel(private val customerRepo: CustomerRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val statusFilter = MutableStateFlow("")

    fun setQuery(q: String) { query.value = q }
    fun setStatusFilter(s: String) { statusFilter.value = s }
    fun setStatus(id: Long, status: String) = viewModelScope.launch {
        customerRepo.setStatus(id, status)
    }
    fun setNotes(id: Long, notes: String) = viewModelScope.launch {
        customerRepo.setNotes(id, notes)
    }
    fun save(customer: CustomerEntity) = viewModelScope.launch {
        customerRepo.updateProfile(customer)
    }
    fun addFollowUp(customerId: Long, dueAt: Long, note: String) = viewModelScope.launch {
        customerRepo.addFollowUp(customerId, dueAt, note)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<CustomerEntity>> =
        combine(query, statusFilter) { q, s -> q to s }
            .flatMapLatest { (q, s) -> customerRepo.observeCustomers(q, s) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// --------------------------------------------------------------- Follow-ups

class FollowUpsViewModel(private val customerRepo: CustomerRepository) : ViewModel() {
    val filter = MutableStateFlow("")
    fun setFilter(s: String) { filter.value = s }
    fun complete(id: Long) = viewModelScope.launch { customerRepo.completeFollowUp(id) }
    fun add(customerId: Long, dueAt: Long, note: String) = viewModelScope.launch {
        customerRepo.addFollowUp(customerId, dueAt, note)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<FollowUpRow>> =
        filter.flatMapLatest { customerRepo.observeFollowUps(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// ------------------------------------------------------------------ Reports

class ReportsViewModel(
    private val settings: SettingsStore,
    private val syncRepo: SyncRepository
) : ViewModel() {
    val recipient: StateFlow<String> =
        settings.recipientFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.DEFAULT_RECIPIENT)
    val lastSync: StateFlow<Long> =
        settings.lastSyncFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val sending = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    fun setRecipient(v: String) = viewModelScope.launch { settings.setRecipient(v) }

    fun send(type: String) = viewModelScope.launch {
        sending.value = true
        message.value = null
        val ok = syncRepo.sendReport(type)
        message.value = if (ok) "درخواست گزارش ارسال شد." else "ارسال ناموفق بود؛ اتصال و آدرس سرور را بررسی کنید."
        sending.value = false
    }
}

// ------------------------------------------------------------------ Devices

class DevicesViewModel(
    private val settings: SettingsStore,
    secure: SecureStore
) : ViewModel() {
    val name: StateFlow<String> =
        settings.nameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val role: StateFlow<String> =
        settings.roleFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.ROLE_PRIMARY)
    val lastSync: StateFlow<Long> =
        settings.lastSyncFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val registered = MutableStateFlow(secure.isRegistered)

    fun setName(v: String) = viewModelScope.launch { settings.setName(v) }
    fun setRole(v: String) = viewModelScope.launch { settings.setRole(v) }
}

// ----------------------------------------------------------------- Settings

class SettingsViewModel(
    private val settings: SettingsStore,
    private val secure: SecureStore,
    private val callRepo: CallRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {
    val serverUrl = settings.serverUrlFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val wifiOnly = settings.wifiOnlyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoSync = settings.autoSyncFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val splitWork = settings.splitWorkFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setServerUrl(v: String) = viewModelScope.launch { settings.setServerUrl(v) }
    fun setWifiOnly(v: Boolean) = viewModelScope.launch { settings.setWifiOnly(v) }
    fun setAutoSync(v: Boolean) = viewModelScope.launch { settings.setAutoSync(v) }
    fun setSplitWork(v: Boolean) = viewModelScope.launch { settings.setSplitWork(v) }

    fun syncNow(context: Context) = viewModelScope.launch {
        SyncScheduler.triggerNow(context, settings.wifiOnly())
    }

    fun deleteAllData() = viewModelScope.launch {
        callRepo.deleteAll()
        customerRepo.deleteAll()
        secure.clear()
    }
}

// ------------------------------------------------------------------ Factory

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(private val app: SmartCallApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val c = app.container
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(c.callRepository, c.customerRepository)
            modelClass.isAssignableFrom(CallsViewModel::class.java) ->
                CallsViewModel(c.callRepository)
            modelClass.isAssignableFrom(CustomersViewModel::class.java) ->
                CustomersViewModel(c.customerRepository)
            modelClass.isAssignableFrom(FollowUpsViewModel::class.java) ->
                FollowUpsViewModel(c.customerRepository)
            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(c.settings, c.syncRepository)
            modelClass.isAssignableFrom(DevicesViewModel::class.java) ->
                DevicesViewModel(c.settings, c.secure)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(c.settings, c.secure, c.callRepository, c.customerRepository)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
