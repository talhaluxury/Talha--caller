package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talhastudio.callerid.Graph
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.ProviderViewModel

@Composable
fun AccountScreen(onBack: () -> Unit, onSignedOut: () -> Unit) {
    val repo = Graph.repo
    ScreenFrame("Account", onBack) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Signed in as", color = SilverText)
            Text(repo.session.email ?: if (repo.configured) "Unknown" else "Not signed in", style = MaterialTheme.typography.titleMedium)
        }
        if (repo.configured) InkButton("Sign out") { repo.signOut(); onSignedOut() }
        else Text("Sign-in is available once the calling service is configured.", color = SilverText)
    }
}

@Composable
fun ProviderScreen(onBack: () -> Unit) {
    val vm: ProviderViewModel = viewModel()
    val mode by vm.mode.collectAsState()
    val balance by vm.balance.collectAsState()
    val snack = remember { SnackbarHostState() }
    ShowMessages(vm.messages, snack)
    LaunchedEffect(Unit) { vm.refresh() }
    ScreenFrame("Calling Provider", onBack, snack) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Status", color = SilverText)
            Text(
                when {
                    !Graph.repo.configured -> "Not configured"
                    mode == "mock" -> "Test mode (simulated, no real calls)"
                    mode == "none" -> "Backend reachable, no provider connected"
                    mode == null -> "Checking…"
                    else -> "Connected: $mode"
                },
                style = MaterialTheme.typography.titleMedium
            )
            balance?.let { Spacer(Modifier.height(6.dp)); Text("Balance: $it") }
        }
        if (Graph.repo.configured) InkButton("Check balance") { vm.loadBalance() }
        Text(
            "Calls are internet calls billed by your provider. They don't use Jazz, Zong, Ufone or Telenor SIM balance.",
            color = SilverText
        )
    }
}

private val infoText = mapOf(
    "privacy" to ("Privacy policy" to
        "What we keep: your email, the caller ID numbers you add, and your caller name request. Our server also keeps a short record of each call (time and the last 4 digits of the number) so it can check that only you can see it.\n\n" +
        "Your full call history is stored only on this phone. You can clear it in Call History.\n\n" +
        "We don't read your contacts, messages or SIM. Provider keys never leave our server.\n\n" +
        "Your telecom provider processes the calls you make and has its own privacy terms."),
    "terms" to ("Terms and conditions" to
        "Use TALHA CALLER ID only with numbers you own or that your provider has issued and verified for you. Impersonating another person or using a number you aren't authorized to use is not allowed and will get your account suspended.\n\n" +
        "Calls are billed by your provider. Caller name display depends on the receiving network and phone and isn't guaranteed.\n\n" +
        "You are responsible for following Pakistani telecom rules and your provider's terms."),
    "help" to ("Help and support" to
        "Can't verify a number? Check that the code came from your provider and try again. After 5 wrong codes, remove the number and add it again.\n\n" +
        "Call failed? Check the number, your internet connection and your provider balance.\n\n" +
        "Need more help? Contact TALHA STUDIO support."),
    "about" to ("About TALHA STUDIO" to
        "TALHA CALLER ID is a product of TALHA STUDIO. It places internet calls to Pakistani mobile numbers through an authorized telecom provider, using only caller IDs that have been verified for your account.\n\nVersion 1.0.0")
)

@Composable
fun InfoScreen(key: String, onBack: () -> Unit) {
    val (title, body) = infoText[key] ?: ("Info" to "")
    ScreenFrame(title, onBack) {
        GlassCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { Text(body) }
    }
}
