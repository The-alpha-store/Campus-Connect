@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.material.icons.filled.PersonAdd

import com.example.ui.components.AddUserSheet

import androidx.compose.material3.IconButton

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.border

import androidx.compose.material3.Surface

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.DropdownMenu

import androidx.compose.material.icons.filled.DeleteOutline

import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.filled.PlayCircle

import androidx.compose.material.icons.filled.PauseCircle

import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.filled.School

import androidx.compose.material.icons.filled.Logout

import com.example.ui.theme.SuccessGreen

import com.example.ui.theme.WarmAmber

import com.example.ui.components.SheetButtons

import com.example.ui.components.EmptyState

import com.example.ui.components.SoftActionButton

import com.example.util.DeadlineTime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.CampusSummary
import com.example.data.repository.WipeScope
import com.example.ui.components.AppBottomSheet
import com.example.ui.components.ConfirmSheet
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.viewmodel.CampusViewModel

/**
 * The app manager's console. It lists the campuses with totals only, creates new campuses with their
 * first administrator, and can delete data from any campus. It never shows what a campus wrote:
 * campus administrators run their own campuses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformScreen(viewModel: CampusViewModel) {
  val campuses by viewModel.campuses.collectAsState()
  val logs by viewModel.auditLogs.collectAsState()
  val message by viewModel.message.collectAsState()
  val user by viewModel.currentUser.collectAsState()
  val snackbar = remember { SnackbarHostState() }
  var creating by remember { mutableStateOf(false) }
  var managing by remember { mutableStateOf<String?>(null) }
  var pausing by remember { mutableStateOf<String?>(null) }
  var editing by remember { mutableStateOf<String?>(null) }
  var addingAdmin by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(message) {
    message?.let {
      snackbar.showSnackbar(it)
      viewModel.messageShown()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbar) },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.systemBarsPadding().testTag("platform_screen")
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(viewModel.t("App manager", "Msimamizi wa programu"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          Text(user.fullName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SoftActionButton(
          label = viewModel.t("Sign out", "Toka"), icon = Icons.Default.Logout,
          onClick = { viewModel.signOut() }, tag = "platform_sign_out"
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(viewModel.t("Campuses", "Vyuo"), campuses.size.toString(), Modifier.weight(1f))
        StatTile(viewModel.t("People", "Watu"), campuses.sumOf { it.people }.toString(), Modifier.weight(1f))
        StatTile(viewModel.t("Offline", "Nje ya mtandao"), campuses.count { it.university.maintenance }.toString(), Modifier.weight(1f))
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        viewModel.t(
          "You manage every campus. You see totals, never what a campus wrote. Campus administrators run their own campuses.",
          "Unasimamia kila chuo. Unaona jumla tu, si kilichoandikwa na chuo. Wasimamizi wa vyuo huendesha vyuo vyao."
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(14.dp))

      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (campuses.isEmpty()) {
          item {
            EmptyState(
              icon = Icons.Default.School,
              title = viewModel.t("No campuses yet", "Hakuna vyuo bado"),
              body = viewModel.t(
                "Create the first campus and its administrator. Their people then sign up themselves.",
                "Unda chuo cha kwanza na msimamizi wake. Watu wake hujisajili wenyewe."
              ),
              action = {
                SoftActionButton(
                  label = viewModel.t("New campus", "Chuo kipya"), icon = Icons.Default.Add,
                  onClick = { creating = true }, filled = true, tag = "platform_empty_new_campus_btn"
                )
              }
            )
          }
        }
        items(campuses, key = { it.university.id }) { campus ->
          Box(modifier = Modifier.animateItem()) {
            CampusCard(
              campus = campus, viewModel = viewModel,
              onManage = { managing = campus.university.id },
              onPause = { pausing = campus.university.id },
              onEdit = { editing = campus.university.id },
              onAddAdmin = { addingAdmin = campus.university.id }
            )
          }
        }
        if (logs.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.t("Recent actions", "Hatua za karibuni"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          items(logs.take(8), key = { it.id }) { log ->
            Text(
              "${DeadlineTime.friendly(log.timestamp)}  \u2022  ${log.action.lowercase().replace('_', ' ')}  \u2022  ${log.details}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
      }

      Button(
        onClick = { creating = true },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 0.dp).testTag("platform_new_campus_btn")
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(viewModel.t("New campus", "Chuo kipya"), fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  if (creating) CreateCampusSheet(viewModel = viewModel, onDismiss = { creating = false })
  managing?.let { id ->
    campuses.firstOrNull { it.university.id == id }?.let { campus ->
      ManageCampusSheet(campus = campus, viewModel = viewModel, onDismiss = { managing = null })
    }
  }
  pausing?.let { id ->
    campuses.firstOrNull { it.university.id == id }?.let { campus ->
      PauseCampusSheet(campus = campus, viewModel = viewModel, onDismiss = { pausing = null })
    }
  }
  editing?.let { id ->
    campuses.firstOrNull { it.university.id == id }?.let { campus ->
      EditCampusSheet(campus = campus, viewModel = viewModel, onDismiss = { editing = null })
    }
  }
  addingAdmin?.let { id ->
    AddUserSheet(viewModel = viewModel, onDismiss = { addingAdmin = null }, adminOfCampus = id)
  }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = modifier) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
  }
}

@Composable
private fun CampusCard(
  campus: CampusSummary,
  viewModel: CampusViewModel,
  onManage: () -> Unit,
  onPause: () -> Unit,
  onEdit: () -> Unit,
  onAddAdmin: () -> Unit
) {
  val uni = campus.university
  var menuOpen by remember { mutableStateOf(false) }
  val paused = uni.maintenance
  val statusColor = if (paused) WarmAmber else SuccessGreen
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).testTag("campus_${uni.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(DeepBlue.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Text(uni.shortName.take(4), color = DeepBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(uni.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
          Text(uni.shortName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
          if (paused) viewModel.t("Offline", "Nje ya mtandao") else viewModel.t("Online", "Mtandaoni"),
          color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
          modifier = Modifier.clip(RoundedCornerShape(50)).background(statusColor.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Fact(campus.people.toString(), viewModel.t("people", "watu"))
        Fact(campus.pending.toString(), viewModel.t("waiting", "wanasubiri"), highlight = campus.pending > 0)
        Fact(campus.courses.toString(), viewModel.t("courses", "kozi"))
        Fact(campus.sessions.toString(), viewModel.t("classes", "vipindi"))
      }
      if (paused) {
        Spacer(Modifier.height(10.dp))
        Text(
          buildString {
            append(viewModel.t("Offline since ", "Nje ya mtandao tangu "))
            append(DeadlineTime.friendly(uni.maintenanceSince, swahili = viewModel.t("a", "b") == "b").lowercase())
            if (uni.maintenanceNote.isNotBlank()) append(" \u2022 ").append(uni.maintenanceNote)
          },
          style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F),
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(WarmAmber.copy(alpha = 0.16f)).padding(10.dp)
        )
      }
      Spacer(Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        SoftActionButton(
          label = viewModel.t("Manage", "Simamia"), icon = Icons.Default.Settings, onClick = onManage,
          modifier = Modifier.weight(1f), tag = "manage_campus_${uni.id}"
        )
        SoftActionButton(
          label = if (paused) viewModel.t("Bring online", "Rudisha mtandaoni") else viewModel.t("Put offline", "Weka nje ya mtandao"),
          icon = if (paused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
          onClick = onPause, color = if (paused) SuccessGreen else WarmAmber,
          modifier = Modifier.weight(1f), tag = "maintenance_toggle_${uni.id}"
        )
        Box {
          IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp).testTag("campus_more_${uni.id}")) {
            Icon(Icons.Default.MoreVert, contentDescription = viewModel.t("More", "Zaidi"))
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text(viewModel.t("Edit details", "Hariri maelezo")) },
              leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
              onClick = { menuOpen = false; onEdit() }
            )
            DropdownMenuItem(
              text = { Text(viewModel.t("Add administrator", "Ongeza msimamizi")) },
              leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
              onClick = { menuOpen = false; onAddAdmin() },
              modifier = Modifier.testTag("add_admin_${uni.id}")
            )
            DropdownMenuItem(
              text = { Text(viewModel.t("Clear data or delete\u2026", "Futa data au chuo\u2026"), color = DangerRed) },
              leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = DangerRed) },
              onClick = { menuOpen = false; onManage() }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Fact(value: String, label: String, highlight: Boolean = false) {
  Column {
    Text(value, fontWeight = FontWeight.Bold, color = if (highlight) WarmAmber else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PauseCampusSheet(campus: CampusSummary, viewModel: CampusViewModel, onDismiss: () -> Unit) {
  val uni = campus.university
  if (uni.maintenance) {
    ConfirmSheet(
      title = viewModel.t("Bring ${uni.shortName} back online?", "Rudisha ${uni.shortName} mtandaoni?"),
      message = viewModel.t(
        "${campus.people} people can use the app again, and they will be told it is back.",
        "Watu ${campus.people} wataweza kutumia programu tena, na wataambiwa imerudi."
      ),
      confirmLabel = viewModel.t("Bring online", "Rudisha mtandaoni"),
      dismissLabel = viewModel.t("Cancel", "Ghairi"),
      destructive = false,
      onDismiss = onDismiss,
      onConfirm = { viewModel.setCampusMaintenance(uni.id, false); onDismiss() },
      confirmTag = "confirm_reopen_campus_btn"
    )
  } else {
    var note by remember { mutableStateOf("") }
    AppBottomSheet(
      onDismissRequest = onDismiss,
      title = viewModel.t("Put ${uni.shortName} offline", "Weka ${uni.shortName} nje ya mtandao"),
      subtitle = viewModel.t(
        "Everyone at this campus is paused: they see your message and nothing else works for them until you reopen it.",
        "Kila mtu wa chuo hiki anasimamishwa: wanaona ujumbe wako na hakuna kinachofanya kazi hadi ufungue."
      ),
      tag = "pause_campus_sheet",
      footer = {
        SheetButtons(
          dismissLabel = viewModel.t("Cancel", "Ghairi"), onDismiss = onDismiss,
          confirmLabel = viewModel.t("Put campus offline", "Weka chuo nje ya mtandao"),
          onConfirm = { viewModel.setCampusMaintenance(uni.id, true, note); onDismiss() },
          confirmColor = WarmAmber, confirmIcon = Icons.Default.PauseCircle, confirmTag = "confirm_pause_campus_btn"
        )
      }
    ) {
      Spacer(Modifier.height(4.dp))
      OutlinedTextField(
        value = note, onValueChange = { note = it.take(300) },
        label = { Text(viewModel.t("Message for the campus (optional)", "Ujumbe kwa chuo (si lazima)")) },
        placeholder = { Text(viewModel.t("e.g. Back at 08:00", "mf. Tunarudi saa 02:00 asubuhi")) },
        minLines = 2, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("pause_note_input")
      )
      Text("${note.length}/300", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
      Spacer(Modifier.height(8.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCampusSheet(campus: CampusSummary, viewModel: CampusViewModel, onDismiss: () -> Unit) {
  val uni = campus.university
  var name by remember(uni.id) { mutableStateOf(uni.name) }
  var error by remember { mutableStateOf<String?>(null) }
  AppBottomSheet(
    onDismissRequest = onDismiss,
    title = viewModel.t("Edit campus details", "Hariri maelezo ya chuo"),
    subtitle = viewModel.t("The short code ${uni.shortName} never changes.", "Msimbo mfupi ${uni.shortName} haubadiliki."),
    tag = "edit_campus_sheet",
    footer = {
      SheetButtons(
        dismissLabel = viewModel.t("Cancel", "Ghairi"), onDismiss = onDismiss,
        confirmLabel = viewModel.t("Save", "Hifadhi"),
        onConfirm = { val e = viewModel.renameCampus(uni.id, name); if (e == null) onDismiss() else error = e },
        enabled = name.isNotBlank(), confirmTag = "save_campus_details_btn"
      )
    }
  ) {
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
      value = name, onValueChange = { name = it.take(120); error = null },
      label = { Text(viewModel.t("Campus name", "Jina la chuo")) }, singleLine = true,
      shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("campus_name_input")
    )
    error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
    Spacer(Modifier.height(8.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCampusSheet(viewModel: CampusViewModel, onDismiss: () -> Unit) {
  var code by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var adminName by remember { mutableStateOf("") }
  var adminEmail by remember { mutableStateOf("") }
  var adminNumber by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AppBottomSheet(
    onDismissRequest = onDismiss,
    title = viewModel.t("New campus", "Chuo kipya"),
    subtitle = viewModel.t(
      "Creates the campus and its first administrator. The administrator then adds the programmes, courses and rooms and approves their people.",
      "Huunda chuo na msimamizi wake wa kwanza. Msimamizi huyo huongeza programu, kozi na vyumba na kuidhinisha watu wake."
    ),
    footer = {
      Button(
        onClick = {
          error = viewModel.createCampus(code, name, adminName, adminEmail, adminNumber, adminPassword)
          if (error == null) onDismiss()
        },
        enabled = listOf(code, name, adminName, adminEmail, adminNumber, adminPassword).all { it.isNotBlank() },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
        modifier = Modifier.weight(1f).height(52.dp).testTag("campus_create_btn")
      ) { Text(viewModel.t("Create campus", "Unda chuo"), fontWeight = FontWeight.SemiBold) }
    }
  ) {
    Spacer(modifier = Modifier.height(8.dp))
    SheetField(code, { code = it; error = null }, viewModel.t("Short name (for example UDOM)", "Jina fupi (mfano UDOM)"), "campus_code")
    Spacer(modifier = Modifier.height(10.dp))
    SheetField(name, { name = it; error = null }, viewModel.t("Full name of the university", "Jina kamili la chuo"), "campus_name")
    Spacer(modifier = Modifier.height(18.dp))
    Text(viewModel.t("First administrator", "Msimamizi wa kwanza"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    SheetField(adminName, { adminName = it; error = null }, viewModel.t("Full name", "Jina kamili"), "campus_admin_name")
    Spacer(modifier = Modifier.height(10.dp))
    SheetField(adminEmail, { adminEmail = it; error = null }, viewModel.t("Email", "Barua pepe"), "campus_admin_email")
    Spacer(modifier = Modifier.height(10.dp))
    SheetField(adminNumber, { adminNumber = it; error = null }, viewModel.t("Staff number", "Namba ya mfanyakazi"), "campus_admin_number")
    Spacer(modifier = Modifier.height(10.dp))
    SheetField(adminPassword, { adminPassword = it; error = null }, viewModel.t("Starting password", "Nenosiri la kuanzia"), "campus_admin_password", password = true)

    if (error != null) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(error ?: "", color = DangerRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("campus_error"))
    }
  }
}

private data class DeleteChoice(val label: String, val detail: String, val scope: WipeScope?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCampusSheet(campus: CampusSummary, viewModel: CampusViewModel, onDismiss: () -> Unit) {
  var confirm by remember { mutableStateOf<DeleteChoice?>(null) }
  var typed by remember { mutableStateOf("") }

  val choices = listOf(
    DeleteChoice(viewModel.t("Notices", "Matangazo"), "${campus.announcements}", WipeScope.ANNOUNCEMENTS),
    DeleteChoice(viewModel.t("Timetable", "Ratiba"), "${campus.sessions}", WipeScope.TIMETABLE),
    DeleteChoice(viewModel.t("Study materials", "Vifaa vya masomo"), "${campus.materials}", WipeScope.MATERIALS),
    DeleteChoice(viewModel.t("Deadlines and hand-ins", "Tarehe za mwisho na kazi"), "${campus.works}", WipeScope.WORKS),
    DeleteChoice(viewModel.t("Notifications", "Arifa"), "", WipeScope.NOTIFICATIONS),
    DeleteChoice(viewModel.t("Programmes, courses and rooms", "Programu, kozi na vyumba"), "${campus.programmes + campus.courses + campus.rooms}", WipeScope.CATALOG),
    DeleteChoice(viewModel.t("Students and lecturers", "Wanafunzi na wahadhiri"), "${campus.people - campus.admins}", WipeScope.PEOPLE),
    DeleteChoice(viewModel.t("Everything, including administrators", "Kila kitu, pamoja na wasimamizi"), "", WipeScope.EVERYTHING),
    DeleteChoice(viewModel.t("Delete this campus completely", "Futa chuo hiki kabisa"), "", null)
  )

  AppBottomSheet(
    onDismissRequest = onDismiss,
    title = campus.university.name,
    subtitle = viewModel.t(
      "${campus.people} people \u2022 ${campus.pending} waiting for approval",
      "Watu ${campus.people} \u2022 ${campus.pending} wanasubiri idhini"
    ),
    tag = "manage_campus_sheet"
  ) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(viewModel.t("Delete data", "Futa data"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(
      viewModel.t("Deleting cannot be undone.", "Kufuta hakurudishwi."),
      style = MaterialTheme.typography.bodySmall,
      color = DangerRed
    )
    Spacer(modifier = Modifier.height(6.dp))
    choices.forEach { choice ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { typed = ""; confirm = choice }
          .padding(horizontal = 8.dp, vertical = 14.dp)
          .testTag("wipe_${choice.scope?.name ?: "CAMPUS"}"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          choice.label,
          style = MaterialTheme.typography.bodyLarge,
          color = if (choice.scope == null || choice.scope == WipeScope.EVERYTHING) DangerRed else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
        if (choice.detail.isNotBlank()) {
          Text(choice.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }

  confirm?.let { choice ->
    val matches = typed.trim().equals(campus.university.shortName, ignoreCase = true)
    ConfirmSheet(
      title = viewModel.t("Delete ${choice.label.lowercase()}?", "Futa ${choice.label.lowercase()}?"),
      message = viewModel.t(
        "This removes it from ${campus.university.name} for everyone and cannot be undone. Type ${campus.university.shortName} to confirm.",
        "Hii huiondoa kwa kila mtu wa ${campus.university.name} na haiwezi kurudishwa. Andika ${campus.university.shortName} kuthibitisha."
      ),
      confirmLabel = viewModel.t("Delete", "Futa"),
      dismissLabel = viewModel.t("Cancel", "Ghairi"),
      confirmEnabled = matches,
      onDismiss = { confirm = null },
      onConfirm = {
        val scope = choice.scope
        if (scope == null) viewModel.deleteCampus(campus.university.id) else viewModel.wipeCampus(campus.university.id, scope)
        confirm = null
        onDismiss()
      },
      confirmTag = "wipe_confirm_btn",
      extra = {
        OutlinedTextField(
          value = typed,
          onValueChange = { typed = it },
          singleLine = true,
          placeholder = { Text(campus.university.shortName) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("wipe_confirm_text")
        )
      }
    )
  }
}
