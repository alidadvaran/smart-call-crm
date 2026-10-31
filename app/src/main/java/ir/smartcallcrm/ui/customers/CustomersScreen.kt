package ir.smartcallcrm.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.smartcallcrm.core.TimeUtils
import ir.smartcallcrm.data.local.entity.CustomerEntity
import ir.smartcallcrm.ui.CustomersViewModel
import ir.smartcallcrm.ui.Labels

@Composable
fun CustomersScreen(vm: CustomersViewModel) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CustomerEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            label = { Text("جستجوی نام یا شماره…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(
            Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == "",
                    onClick = { vm.setStatusFilter("") },
                    label = { Text("همه") }
                )
            }
            items(Labels.statuses) { st ->
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { vm.setStatusFilter(st) },
                    label = { Text(stringResource(Labels.statusRes(st))) }
                )
            }
        }
        if (customers.isEmpty()) {
            Text("مشتری‌ای یافت نشد.", Modifier.padding(16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(customers, key = { it.id }) { c ->
                    CustomerRow(c) { selected = c }
                }
            }
        }
    }

    selected?.let { c ->
        CustomerDialog(
            customer = c,
            onDismiss = { selected = null },
            onStatus = { vm.setStatus(c.id, it) },
            onSave = { name, company, notes ->
                vm.save(c.copy(displayName = name, company = company, notes = notes))
            },
            onAddFollowUp = { days, note ->
                vm.addFollowUp(c.id, TimeUtils.startOfDayMillis(0) + days * 86_400_000L, note)
            }
        )
    }
}

@Composable
private fun CustomerRow(c: CustomerEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(c.displayName, fontWeight = FontWeight.SemiBold)
                Text(TimeUtils.toPersianDigits(c.numberNormalized), fontSize = 13.sp)
                c.company?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp) }
                Text(
                    "تعداد تماس: ${TimeUtils.toPersianDigits(c.callCount.toString())} • آخرین: ${TimeUtils.formatJalaliDate(c.lastCallAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(stringResource(Labels.statusRes(c.status)), color = Labels.statusColor(c.status), fontSize = 12.sp)
                }
            )
        }
    }
}

@Composable
private fun CustomerDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onStatus: (String) -> Unit,
    onSave: (String, String?, String?) -> Unit,
    onAddFollowUp: (Int, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.displayName) }
    var company by remember { mutableStateOf(customer.company ?: "") }
    var notes by remember { mutableStateOf(customer.notes ?: "") }
    var fuNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(name, company.ifBlank { null }, notes.ifBlank { null })
                onDismiss()
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
        title = { Text(TimeUtils.toPersianDigits(customer.numberNormalized)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true)
                OutlinedTextField(company, { company = it }, label = { Text("شرکت") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("یادداشت") })

                Text("وضعیت پیگیری:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Labels.statuses) { st ->
                        FilterChip(
                            selected = customer.status == st,
                            onClick = { onStatus(st) },
                            label = { Text(stringResource(Labels.statusRes(st)), fontSize = 12.sp) }
                        )
                    }
                }

                Text("افزودن پیگیری:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(fuNote, { fuNote = it }, label = { Text("یادداشت پیگیری") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 7).forEach { d ->
                        SuggestionChip(
                            onClick = {
                                if (fuNote.isNotBlank()) { onAddFollowUp(d, fuNote); fuNote = "" }
                            },
                            label = { Text("+${TimeUtils.toPersianDigits(d.toString())} روز") }
                        )
                    }
                }
            }
        }
    )
}
