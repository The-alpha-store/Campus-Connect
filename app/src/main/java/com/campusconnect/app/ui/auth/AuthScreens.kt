package com.campusconnect.app.ui.auth

import com.campusconnect.app.tr
import com.campusconnect.app.trf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusconnect.app.data.AppContainer
import com.campusconnect.app.data.Profile
import com.campusconnect.app.ui.AuthVm
import com.campusconnect.app.ui.MessageEffect
import com.campusconnect.app.ui.vm

@Composable
private fun Brand(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.School, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("CampusConnect"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(tr(subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String = "Password") {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(tr(label)) }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { show = !show }) {
                Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle password")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun AuthFlow(c: AppContainer) {
    val vm = vm("auth") { AuthVm(c) }
    val busy by vm.busy.collectAsState()
    val host = remember { SnackbarHostState() }
    MessageEffect(vm, host)

    var screen by rememberSaveable { mutableStateOf("login") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val register = screen == "register"

    val emailOk = email.contains("@") && email.contains(".")
    val valid = emailOk && pass.length >= 8 && (!register || (name.isNotBlank() && pass == confirm))

    Scaffold(snackbarHost = { SnackbarHost(host) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Brand("Your timetable, announcements and deadlines in one place.")
            Spacer(Modifier.height(16.dp))
            Text(if (register) tr("Create your account") else tr("Welcome back"), style = MaterialTheme.typography.titleMedium)

            if (register) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text(tr("Full name")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text(tr("Email")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(pass, { pass = it })
            if (register) {
                PasswordField(confirm, { confirm = it }, "Confirm password")
                if (pass.isNotEmpty() && pass.length < 8) {
                    Text(tr("Use at least 8 characters."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else if (confirm.isNotEmpty() && pass != confirm) {
                    Text(tr("Passwords do not match."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    tr("New accounts are reviewed by a campus administrator before you can see course information."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    if (register) vm.signUp(name, email, pass) { screen = "login"; confirm = "" }
                    else vm.signIn(email, pass)
                },
                enabled = valid && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (register) tr("Create account") else tr("Sign in"))
            }
            TextButton(onClick = { screen = if (register) "login" else "register" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (register) tr("I already have an account") else tr("New here? Create an account"))
            }
        }
    }
}

@Composable
fun PendingScreen(me: Profile, refreshing: Boolean, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Brand("Almost there")
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Default.HourglassTop, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(12.dp))
        Text(tr("Awaiting approval"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            trf("Hi %s, your account is waiting for a campus administrator to approve it. You will see your courses as soon as it is approved.", me.fullName.substringBefore(' ')),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(tr("Check again"))
        }
        TextButton(onClick = onSignOut) { Text(tr("Sign out")) }
    }
}

@Composable
fun BlockedScreen(onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Block, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(tr("Account not active"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            tr("This account has been disabled or was not approved. Contact your campus administrator."),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onSignOut) { Text(tr("Sign out")) }
    }
}
