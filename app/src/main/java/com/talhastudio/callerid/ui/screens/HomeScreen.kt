package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.talhastudio.callerid.core.*
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.HomeViewModel

fun stateLabel(s: CallState) = when (s) {
    CallState.CALLING -> "Calling…"
    CallState.RINGING -> "Ringing…"
    CallState.CONNECTED -> "Connected"
    CallState.ENDED -> "Call ended"
    CallState.FAILED -> "Call failed"
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val vm: HomeViewModel = viewModel()
    val digits by vm.digits.collectAsState()
    val caller by vm.callerId.collectAsState()
    val active by vm.active.collectAsState()
    val recent by vm.recent.collectAsState()
    val snack = remember { SnackbarHostState() }
    ShowMessages(vm.messages, snack)
    LaunchedEffect(Unit) { vm.refresh() }

    active?.let { a ->
        val st = a.session.state
        AlertDialog(
            onDismissRequest = {},
            containerColor = Pearl,
            title = { Text(stateLabel(st)) },
            text = {
                Column {
                    Text(a.to, style = MaterialTheme.typography.titleMedium)
                    Text("From ${a.from}", color = SilverText)
                    if (st == CallState.CONNECTED || st == CallState.ENDED) Text(formatDuration(a.session.durationSec))
                    if (a.session.testMode) {
                        Spacer(Modifier.height(8.dp))
                        Text("TEST MODE: this is a simulation. No real call is placed.", color = Champagne)
                    }
                }
            },
            confirmButton = {
                if (st.terminal) TextButton(onClick = vm::dismiss) { Text("Close") }
                else TextButton(onClick = vm::hangUp) { Text("End call") }
            }
        )
    }

    PearlBackground {
        Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snack) }) { pad ->
            Column(
                Modifier.padding(pad).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TALHA CALLER ID", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 2.sp)
                        Text("TALHA STUDIO", color = Champagne, fontSize = 11.sp, letterSpacing = 3.sp)
                    }
                    IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                }
                Text("Welcome. Enter a Pakistani mobile number to call.", color = SilverText, modifier = Modifier.fillMaxWidth())

                GlassCard(Modifier.fillMaxWidth(), onClick = { nav.navigate("numbers") }) {
                    Text("Outgoing caller ID", color = SilverText, fontSize = 12.sp)
                    Text(caller?.e164 ?: "None verified yet", style = MaterialTheme.typography.titleMedium)
                    Text(caller?.callerName?.let { "Caller name: $it" } ?: "Tap to manage your verified numbers",
                        color = Champagne, fontSize = 12.sp)
                }

                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+92", fontWeight = FontWeight.Medium, fontSize = 22.sp, color = Champagne)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (digits.isEmpty()) "3XX XXXXXXX" else digits,
                            fontSize = 28.sp, fontWeight = FontWeight.Light,
                            color = if (digits.isEmpty()) Silver else Ink,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = vm::backspace) { Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete digit") }
                    }
                    val op = PakistaniNumber.normalize(digits)?.let { PakistaniNumber.operator(it) }
                    if (op != null && op != PakistaniNumber.Operator.UNKNOWN) Text("Likely ${op.label}", color = SilverText, fontSize = 12.sp)
                }

                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) { r.forEach { d -> GlassKey(d) { vm.press(d) } } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Spacer(Modifier.size(68.dp)); GlassKey("0") { vm.press("0") }; Spacer(Modifier.size(68.dp))
                }

                CallButton(onClick = vm::call)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(Modifier.weight(1f), onClick = { nav.navigate("numbers") }) {
                        Text("Verified numbers", style = MaterialTheme.typography.titleMedium)
                    }
                    GlassCard(Modifier.weight(1f), onClick = { nav.navigate("history") }) {
                        Text("Call history", style = MaterialTheme.typography.titleMedium)
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Recent calls", style = MaterialTheme.typography.titleMedium)
                    if (recent.isEmpty()) Text("No calls yet", color = SilverText, fontSize = 12.sp)
                    recent.forEach { c ->
                        Spacer(Modifier.height(6.dp))
                        Text(c.toNumber)
                        Text("${formatDate(c.startedAt)} · ${c.status.lowercase()}", color = SilverText, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
