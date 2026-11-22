package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talhastudio.callerid.core.VerifiedNumber
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.VerifiedNumbersViewModel

@Composable
fun VerifiedNumbersScreen(onBack: () -> Unit) {
    val vm: VerifiedNumbersViewModel = viewModel()
    val list by vm.numbers.collectAsState()
    val busy by vm.busy.collectAsState()
    val testMode by vm.testMode.collectAsState()
    val snack = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var verifyTarget by remember { mutableStateOf<VerifiedNumber?>(null) }
    var input by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    ShowMessages(vm.messages, snack)
    LaunchedEffect(Unit) { vm.refresh() }

    if (showAdd) AlertDialog(
        onDismissRequest = { showAdd = false }, containerColor = Pearl,
        title = { Text("Add a number") },
        text = {
            Column {
                Text("Use a number you own or one issued to you by your provider. We'll ask the provider to verify it.", color = SilverText)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(input, { input = it }, label = { Text("Phone number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = glassFieldColors())
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && input.isNotBlank(), onClick = {
                vm.add(input) { n -> showAdd = false; input = ""; code = ""; verifyTarget = n }
            }) { Text("Send code") }
        },
        dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
    )

    verifyTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { verifyTarget = null }, containerColor = Pearl,
            title = { Text("Verify ${t.e164}") },
            text = {
                Column {
                    Text("Enter the code from your provider.", color = SilverText)
                    if (testMode) Text("Test mode: use code 123456.", color = Champagne)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(code, { code = it.take(8) }, label = { Text("Code") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = glassFieldColors())
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && code.isNotBlank(), onClick = {
                    vm.verify(t.id, code) { verifyTarget = null; code = "" }
                }) { Text("Verify") }
            },
            dismissButton = { TextButton(onClick = { verifyTarget = null }) { Text("Later") } }
        )
    }

    ScreenFrame("My Verified Numbers", onBack, snack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (list.isEmpty()) GlassCard(Modifier.fillMaxWidth()) {
                Text("No numbers yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Only numbers your provider confirms are authorized can be used as caller ID.", color = SilverText)
            }
            list.forEach { n ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(n.e164, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (n.status) { "verified" -> "Verified"; "pending" -> "Waiting for verification"; else -> "Rejected" },
                        color = if (n.isVerified) Champagne else SilverText
                    )
                    n.callerName?.let { Text("Caller name: $it", color = SilverText) }
                    Row {
                        if (n.status == "pending") TextButton(onClick = { code = ""; verifyTarget = n }) { Text("Enter code") }
                        if (n.isVerified) {
                            if (n.isDefault) TextButton(onClick = {}, enabled = false) { Text("Default") }
                            else TextButton(onClick = { vm.makeDefault(n.id) }) { Text("Make default") }
                        }
                        TextButton(onClick = { vm.remove(n.id) }) { Text("Remove") }
                    }
                }
            }
        }
        InkButton("Add a number") { showAdd = true }
        Spacer(Modifier.height(12.dp))
    }
}
