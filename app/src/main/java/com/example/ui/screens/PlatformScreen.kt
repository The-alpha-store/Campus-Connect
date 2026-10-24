package com.example.ui.screens

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
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(viewModel.t("App manager", "Msimamizi wa programu"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          Text(user.fullName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { viewModel.signOut() }, modifier = Modifier.testTag("platform_sign_out")) {
          Text(viewModel.t("Sign out", "Toka"))
        }
      }
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
        items(campuses, key = { it.university.id }) { campus ->
          Box(modifier = Modifier.animateItem()) {
            CampusCard(campus, viewModel) { managing = campus.university.id }
          }
        }
        if (logs.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.t("Recent actions", "Hatua za karibuni"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          items(logs.take(8), key = { it.id }) { log ->
            Text(
              "${log.timestamp}  \u2022  ${log.action.lowercase().replace('_', ' ')}  \u2022  ${log.details}",
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
}

@Composable
private fun CampusCard(campus: CampusSummary, viewModel: CampusViewModel, onManage: () -> Unit) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onManage() }.testTag("campus_${campus.university.id}")
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text("${campus.university.name} (${campus.university.shortName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          viewModel.t(
            "${campus.people} people \u2022 ${campus.pending} waiting \u2022 ${campus.admins} admin",
            "Watu ${campus.people} \u2022 ${campus.pending} wanasubiri \u2022 msimamizi ${campus.admins}"
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          viewModel.t(
            "${campus.courses} courses \u2022 ${campus.sessions} classes \u2022 ${campus.announcements} notices \u2022 ${campus.materials} files",
            "Kozi ${campus.courses} \u2022 vipindi ${campus.sessions} \u2022 matangazo ${campus.announcements} \u2022 mafaili ${campus.materials}"
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCampusSheet(viewModel: CampusViewModel, onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var code by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var adminName by remember { mutableStateOf("") }
  var adminEmail by remember { mutableStateOf("") }
  var adminNumber by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
    ) {
      Text(viewModel.t("New campus", "Chuo kipya"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        viewModel.t(
          "Creates the campus and its first administrator. The administrator then adds the programmes, courses and rooms and approves their people.",
          "Huunda chuo na msimamizi wake wa kwanza. Msimamizi huyo huongeza programu, kozi na vyumba na kuidhinisha watu wake."
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(14.dp))
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
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = {
          error = viewModel.createCampus(code, name, adminName, adminEmail, adminNumber, adminPassword)
          if (error == null) onDismiss()
        },
        enabled = listOf(code, name, adminName, adminEmail, adminNumber, adminPassword).all { it.isNotBlank() },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("campus_create_btn")
      ) { Text(viewModel.t("Create campus", "Unda chuo"), fontWeight = FontWeight.SemiBold) }
    }
  }
}

private data class DeleteChoice(val label: String, val detail: String, val scope: WipeScope?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCampusSheet(campus: CampusSummary, viewModel: CampusViewModel, onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
    ) {
      Text(campus.university.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(
        viewModel.t(
          "${campus.people} people \u2022 ${campus.pending} waiting for approval",
          "Watu ${campus.people} \u2022 ${campus.pending} wanasubiri idhini"
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
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
  }

  confirm?.let { choice ->
    val matches = typed.trim().equals(campus.university.shortName, ignoreCase = true)
    AlertDialog(
      onDismissRequest = { confirm = null },
      title = { Text(viewModel.t("Delete ${choice.label.lowercase()}?", "Futa ${choice.label.lowercase()}?")) },
      text = {
        Column {
          Text(
            viewModel.t(
              "This removes it from ${campus.university.name} for everyone. Type ${campus.university.shortName} to confirm.",
              "Hii huiondoa kwa kila mtu wa ${campus.university.name}. Andika ${campus.university.shortName} kuthibitisha."
            )
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("wipe_confirm_text")
          )
        }
      },
      confirmButton = {
        TextButton(
          enabled = matches,
          onClick = {
            val scope = choice.scope
            if (scope == null) viewModel.deleteCampus(campus.university.id) else viewModel.wipeCampus(campus.university.id, scope)
            confirm = null
            onDismiss()
          },
          modifier = Modifier.testTag("wipe_confirm_btn")
        ) { Text(viewModel.t("Delete", "Futa"), color = if (matches) DangerRed else Color.Gray) }
      },
      dismissButton = { TextButton(onClick = { confirm = null }) { Text(viewModel.t("Cancel", "Ghairi")) } }
    )
  }
}
