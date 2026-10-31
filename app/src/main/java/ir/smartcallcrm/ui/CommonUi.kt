package ir.smartcallcrm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Four selectable date ranges used across Dashboard and Calls. */
val dateRanges = listOf(
    1 to "امروز",
    7 to "۷ روز",
    30 to "۳۰ روز",
    0 to "همه"
)

@Composable
fun RangeChips(selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dateRanges.size) { i ->
            val (value, label) = dateRanges[i]
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 13.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.outline)
    }
}
