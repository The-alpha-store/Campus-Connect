package com.campusconnect.app.ui

import com.campusconnect.app.tr
import com.campusconnect.app.trf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusconnect.app.BuildConfig
import com.campusconnect.app.data.Profile
import com.campusconnect.app.data.Programme

@Composable
fun ProfileScreen(
    me: Profile,
    programmes: List<Programme>,
    lang: String,
    onLang: (String) -> Unit,
    pendingApprovals: Int,
    onOpenAdmin: () -> Unit,
    onOpenNotices: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val programme = programmes.firstOrNull { it.id == me.programmeId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Profile")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                me.fullName.trim().take(1).uppercase(), style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Text(me.fullName, style = MaterialTheme.typography.titleLarge)
                    me.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    StatusBadge(roleLabel(me.role), roleColor(me.role))
                }
            }
            AppCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(Icons.Default.Badge, "Registration / staff number", me.studentNumber ?: me.staffNumber ?: tr("Not set"))
                    if (me.role == "student") InfoRow(Icons.Default.School, "Programme", programme?.let { "${it.code} - ${it.name}" } ?: tr("Not set"))
                    InfoRow(Icons.Default.Language, "Time zone", tr("East Africa Time (Africa/Dar_es_Salaam)"))
                }
            }
            AppCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Language"), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(lang == "en", { onLang("en") }, { Text(tr("English")) })
                        FilterChip(lang == "sw", { onLang("sw") }, { Text(tr("Kiswahili")) })
                    }
                }
            }
            AppCard(onClick = onOpenNotices) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Announcements"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            if (me.role == "admin") {
                AppCard(onClick = onOpenAdmin) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(tr("Administration"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (pendingApprovals > 0) Badge { Text("$pendingApprovals") }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Refresh all data"))
            }
            Button(
                onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Sign out"))
            }
            Text(
                "CampusConnect ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Sign out?")) },
            text = { Text(tr("Saved offline data on this phone will be removed.")) },
            confirmButton = { TextButton(onClick = { confirm = false; onSignOut() }) { Text(tr("Sign out")) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(tr(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
