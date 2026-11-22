package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talhastudio.callerid.core.*
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.HistoryViewModel

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val query by vm.query.collectAsState()
    val items by vm.items.collectAsState()
    var confirm by remember { mutableStateOf(false) }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, containerColor = Pearl,
        title = { Text("Clear call history?") },
        text = { Text("This removes all calls stored on this phone.") },
        confirmButton = { TextButton(onClick = { vm.clear(); confirm = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
    )

    ScreenFrame("Call History", onBack) {
        OutlinedTextField(query, { vm.query.value = it }, label = { Text("Search by number") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), colors = glassFieldColors())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) GlassCard(Modifier.fillMaxWidth()) {
                Text(if (query.isBlank()) "No calls yet" else "No matches", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Calls you make show their number, caller ID, time, duration and result here.", color = SilverText)
            }
            items.forEach { c ->
                GlassCard(Modifier.fillMaxWidth(), corner = 20) {
                    Text(c.toNumber, style = MaterialTheme.typography.titleMedium)
                    Text("From ${c.callerId}", color = SilverText)
                    Text("${formatDate(c.startedAt)} · ${formatDuration(c.durationSec)}", color = SilverText)
                    Text(
                        (if (c.status == "FAILED") "Failed" else "Completed") + if (c.testMode) " (test mode)" else "",
                        color = if (c.status == "FAILED") Ink else Champagne
                    )
                }
            }
        }
        TextButton(onClick = { confirm = true }, enabled = items.isNotEmpty() || query.isNotBlank()) { Text("Clear history") }
    }
}
