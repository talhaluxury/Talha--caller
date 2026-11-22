package com.talhastudio.callerid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talhastudio.callerid.ui.components.*
import com.talhastudio.callerid.ui.theme.*
import com.talhastudio.callerid.ui.vm.LoginViewModel

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val vm: LoginViewModel = viewModel()
    val busy by vm.busy.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    ShowMessages(vm.messages, snack)

    PearlBackground {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, snackbarHost = { SnackbarHost(snack) }) { pad ->
            Column(
                Modifier.padding(pad).padding(24.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TALHA CALLER ID", fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 2.sp)
                Text("TALHA STUDIO", color = Champagne, fontSize = 12.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(28.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(), colors = glassFieldColors())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(password, { password = it }, label = { Text("Password (8+ characters)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), colors = glassFieldColors())
                }
                Spacer(Modifier.height(16.dp))
                InkButton(if (create) "Create account" else "Sign in", enabled = !busy && email.isNotBlank() && password.length >= 8) {
                    vm.submit(email, password, create, onSignedIn)
                }
                TextButton(onClick = { create = !create }) {
                    Text(if (create) "I already have an account" else "Create a new account")
                }
            }
        }
    }
}
