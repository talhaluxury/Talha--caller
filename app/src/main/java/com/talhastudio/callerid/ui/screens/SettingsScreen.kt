package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.talhastudio.callerid.ui.components.*

@Composable
fun SettingsScreen(nav: NavHostController, onBack: () -> Unit) {
    val items = listOf(
        "Account" to "account",
        "Verified numbers" to "numbers",
        "Caller name" to "name",
        "Calling provider" to "provider",
        "Call history" to "history",
        "Privacy policy" to "info/privacy",
        "Terms and conditions" to "info/terms",
        "Help and support" to "info/help",
        "About TALHA STUDIO" to "info/about",
    )
    ScreenFrame("Settings", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (label, route) ->
                GlassCard(Modifier.fillMaxWidth(), corner = 20, onClick = { nav.navigate(route) }) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
