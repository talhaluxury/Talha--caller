package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.CallerNameViewModel

@Composable
fun CallerNameScreen(onBack: () -> Unit) {
    val vm: CallerNameViewModel = viewModel()
    val current by vm.current.collectAsState()
    val busy by vm.busy.collectAsState()
    var name by remember { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }
    ShowMessages(vm.messages, snack)
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenFrame("Caller Name", onBack, snack) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Status", color = SilverText)
            Text(
                when (current.status) {
                    "approved" -> "Approved: ${current.name}"
                    "pending" -> "Waiting for approval: ${current.name}"
                    "rejected" -> "Rejected: ${current.name}. Try a different name."
                    else -> "No name submitted"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            OutlinedTextField(name, { name = it.take(30) }, label = { Text("Business display name") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassFieldColors())
        }
        InkButton("Submit for approval", enabled = !busy && name.isNotBlank()) { vm.submit(name) }
        Text(
            "Your provider must approve the name. The person you call may still see only a number, " +
                "because name display depends on their network and phone.",
            color = SilverText
        )
    }
}
