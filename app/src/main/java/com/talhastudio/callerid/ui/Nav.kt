package com.talhastudio.callerid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.talhastudio.callerid.Graph
import com.talhastudio.callerid.ui.screens.*

@Composable
fun AppNav() {
    val nav: NavHostController = rememberNavController()
    val repo = Graph.repo
    val start = if (repo.configured && repo.session.token == null) "login" else "home"
    val back = { nav.popBackStack(); Unit }
    NavHost(nav, startDestination = start) {
        composable("login") {
            LoginScreen { nav.navigate("home") { popUpTo("login") { inclusive = true } } }
        }
        composable("home") { HomeScreen(nav) }
        composable("numbers") { VerifiedNumbersScreen(back) }
        composable("name") { CallerNameScreen(back) }
        composable("history") { HistoryScreen(back) }
        composable("settings") { SettingsScreen(nav, back) }
        composable("account") {
            AccountScreen(back) { nav.navigate("login") { popUpTo(0) } }
        }
        composable("provider") { ProviderScreen(back) }
        composable("info/{key}") { entry -> InfoScreen(entry.arguments?.getString("key") ?: "", back) }
    }
}
