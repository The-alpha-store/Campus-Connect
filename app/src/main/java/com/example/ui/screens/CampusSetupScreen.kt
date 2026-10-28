@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.Alignment

import androidx.compose.material.icons.filled.Edit

import com.example.ui.components.SheetButtons

import com.example.ui.components.SoftActionButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppBottomSheet
import com.example.ui.components.AssignLecturersSheet
import com.example.ui.components.PickerField
import com.example.ui.components.PickerItem
import com.example.ui.components.PickerSheet
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.CampusViewModel

private enum class SetupKind { PROGRAMME, COURSE, IMPORT, ROOM }

/**
 * The campus administrator's own set-up: programmes, courses and rooms. Students and lecturers of
 * this campus choose from what is added here, and nobody else can add or see it.
 */
@Composable
fun CampusSetupTab(viewModel: CampusViewModel, modifier: Modifier = Modifier) {
  val user by viewModel.currentUser.collectAsState()
  val allProgrammes by viewModel.catalogProgrammes.collectAsState()
  val allCourses by viewModel.catalogCourses.collectAsState()
  val allRooms by viewModel.rooms.collectAsState()
  var open by remember { mutableStateOf<SetupKind?>(null) }
  // Reading the accounts makes this screen refresh as soon as a lecturer is added or removed.
  val accounts by viewModel.accounts.collectAsState()
  var assigningFor by remember { mutableStateOf<String?>(null) }
  var assigning by remember { mutableStateOf(false) }
  var editingDetails by remember { mutableStateOf(false) }
  val universities by viewModel.universities.collectAsState()
  val campus = universities.firstOrNull { it.id == user.universityId }

  val programmes = allProgrammes[user.universityId].orEmpty().keys.sorted()
  val courses = allCourses[user.universityId].orEmpty().toList().sortedBy { it.first }
  val rooms = allRooms[user.universityId].orEmpty()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        viewModel.t(
          "Everyone at your campus chooses from what you add here. Other campuses never see it.",
          "Kila mtu wa chuo chako huchagua kutoka unachoongeza hapa. Vyuo vingine havioni."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    item {
      // The campus's own details, editable by its administrator.
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("setup_campus_details")
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(viewModel.t("Campus details", "Maelezo ya chuo"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(campus?.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(campus?.shortName ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
          SoftActionButton(
            label = viewModel.t("Edit", "Hariri"), icon = Icons.Default.Edit,
            onClick = { editingDetails = true }, tag = "edit_campus_details_btn"
          )
        }
      }
    }
    item {
      SetupCard(
        title = viewModel.t("Programmes", "Programu"),
        count = programmes.size,
        preview = programmes.take(3).joinToString(", "),
        empty = viewModel.t("None yet. Students will type theirs when they sign up.", "Hakuna bado. Wanafunzi wataandika zao wakijisajili."),
        primary = viewModel.t("Add programme", "Ongeza programu") to { open = SetupKind.PROGRAMME },
        tag = "setup_programmes"
      )
    }
    item {
      SetupCard(
        title = viewModel.t("Courses", "Kozi"),
        count = courses.size,
        preview = courses.take(3).joinToString(", ") { it.first },
        empty = viewModel.t("None yet. Add them one by one or paste a list.", "Hakuna bado. Ziongeze moja moja au bandika orodha."),
        primary = viewModel.t("Add course", "Ongeza kozi") to { open = SetupKind.COURSE },
        secondary = viewModel.t("Import a list", "Leta orodha") to { open = SetupKind.IMPORT },
        tag = "setup_courses"
      )
    }
    item {
      val uncovered = courses.filter { accounts.isNotEmpty() && viewModel.lecturersFor(it.first).isEmpty() }
      SetupCard(
        title = viewModel.t("Course lecturers", "Wahadhiri wa kozi"),
        count = courses.size - uncovered.size,
        preview = if (courses.isEmpty()) "" else if (uncovered.isEmpty())
          viewModel.t("Every course has a lecturer.", "Kila kozi ina mhadhiri.")
        else viewModel.t(
          "Needs a lecturer: ${uncovered.take(3).joinToString(", ") { it.first }}" + if (uncovered.size > 3) " \u2026" else "",
          "Zinahitaji mhadhiri: ${uncovered.take(3).joinToString(", ") { it.first }}" + if (uncovered.size > 3) " \u2026" else ""
        ),
        empty = viewModel.t("Add courses first, then assign the lecturers who teach them.", "Ongeza kozi kwanza, kisha weka wahadhiri."),
        primary = viewModel.t("Assign lecturers", "Weka wahadhiri") to { assigningFor = null; assigning = true },
        tag = "setup_lecturers",
        showMore = false
      )
    }
    item {
      SetupCard(
        title = viewModel.t("Rooms", "Vyumba"),
        count = rooms.size,
        preview = rooms.take(3).joinToString(", "),
        empty = viewModel.t("None yet. Lecturers pick a room when they add a class.", "Hakuna bado. Wahadhiri huchagua chumba wakiongeza kipindi."),
        primary = viewModel.t("Add room", "Ongeza chumba") to { open = SetupKind.ROOM },
        tag = "setup_rooms"
      )
    }
    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  open?.let { kind ->
    SetupSheet(kind = kind, viewModel = viewModel, programmes = programmes, onDismiss = { open = null })
  }

  if (editingDetails && campus != null) {
    var name by remember(campus.id) { mutableStateOf(campus.name) }
    var error by remember { mutableStateOf<String?>(null) }
    AppBottomSheet(
      onDismissRequest = { editingDetails = false },
      title = viewModel.t("Edit campus details", "Hariri maelezo ya chuo"),
      subtitle = viewModel.t("The short code ${campus.shortName} never changes.", "Msimbo mfupi ${campus.shortName} haubadiliki."),
      tag = "edit_own_campus_sheet",
      footer = {
        SheetButtons(
          dismissLabel = viewModel.t("Cancel", "Ghairi"), onDismiss = { editingDetails = false },
          confirmLabel = viewModel.t("Save", "Hifadhi"),
          onConfirm = { val e = viewModel.updateCampusDetails(name); if (e == null) editingDetails = false else error = e },
          enabled = name.isNotBlank(), confirmTag = "save_own_campus_details_btn"
        )
      }
    ) {
      Spacer(Modifier.height(4.dp))
      OutlinedTextField(
        value = name, onValueChange = { name = it.take(120); error = null },
        label = { Text(viewModel.t("Campus name", "Jina la chuo")) }, singleLine = true,
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("own_campus_name_input")
      )
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
      Spacer(Modifier.height(8.dp))
    }
  }

  if (assigning) {
    AssignLecturersSheet(
      courses = courses,
      lecturers = viewModel.campusLecturers(),
      assignedFor = { code -> viewModel.lecturersFor(code).map { it.id }.toSet() },
      initialCourse = assigningFor,
      onSave = { code, ids -> viewModel.setCourseLecturers(code, ids) },
      onDismiss = { assigning = false },
      t = { en, sw -> viewModel.t(en, sw) }
    )
  }
}

@Composable
private fun SetupCard(
  title: String,
  count: Int,
  preview: String,
  empty: String,
  primary: Pair<String, () -> Unit>,
  tag: String,
  secondary: Pair<String, () -> Unit>? = null,
  showMore: Boolean = true
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().testTag(tag)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$count", style = MaterialTheme.typography.titleMedium, color = DeepBlue, fontWeight = FontWeight.Bold)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (count == 0 && preview.isBlank()) empty else preview + if (showMore && count > 3) " \u2026" else "",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = primary.second,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
          modifier = Modifier.testTag("${tag}_add")
        ) { Text(primary.first) }
        if (secondary != null) {
          OutlinedButton(onClick = secondary.second, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("${tag}_import")) {
            Text(secondary.first)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupSheet(kind: SetupKind, viewModel: CampusViewModel, programmes: List<String>, onDismiss: () -> Unit) {
  var first by remember { mutableStateOf("") }
  var second by remember { mutableStateOf("") }
  var programme by remember { mutableStateOf<String?>(null) }
  var list by remember { mutableStateOf("") }
  var feedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
  var picking by remember { mutableStateOf(false) }

  val title = when (kind) {
    SetupKind.PROGRAMME -> viewModel.t("Add a programme", "Ongeza programu")
    SetupKind.COURSE -> viewModel.t("Add a course", "Ongeza kozi")
    SetupKind.IMPORT -> viewModel.t("Import courses", "Leta kozi")
    SetupKind.ROOM -> viewModel.t("Add a room", "Ongeza chumba")
  }

  fun finish(ok: Boolean, done: String) {
    feedback = if (ok) true to done else false to viewModel.t("Could not save. Check what you typed.", "Imeshindikana kuhifadhi. Angalia ulichoandika.")
    if (ok) { first = ""; second = ""; list = "" }
  }

  AppBottomSheet(
    onDismissRequest = onDismiss,
    title = title,
    tag = "setup_sheet",
    footer = {
        Button(
          onClick = {
            when (kind) {
              SetupKind.PROGRAMME -> finish(viewModel.addProgramme(first), viewModel.t("Added. Add another or close.", "Imeongezwa. Ongeza nyingine au funga."))
              SetupKind.ROOM -> finish(viewModel.addRoom(first), viewModel.t("Added. Add another or close.", "Imeongezwa. Ongeza nyingine au funga."))
              SetupKind.COURSE -> finish(viewModel.addCourse(first, second, programme), viewModel.t("Saved. Add another or close.", "Imehifadhiwa. Ongeza nyingine au funga."))
              SetupKind.IMPORT -> finish(viewModel.importCourses(list), viewModel.t("Imported.", "Imeletwa."))
            }
          },
          enabled = when (kind) {
            SetupKind.COURSE -> first.isNotBlank() && second.isNotBlank()
            SetupKind.IMPORT -> list.isNotBlank()
            else -> first.isNotBlank()
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
          modifier = Modifier.weight(1f).height(52.dp).testTag("setup_save_btn")
        ) { Text(viewModel.t("Save", "Hifadhi"), fontWeight = FontWeight.SemiBold) }
    }
  ) {
    Spacer(modifier = Modifier.height(6.dp))
    when (kind) {
      SetupKind.PROGRAMME -> SheetField(first, { first = it; feedback = null }, viewModel.t("Programme name", "Jina la programu"), "setup_programme_name")
      SetupKind.ROOM -> SheetField(first, { first = it; feedback = null }, viewModel.t("Room name", "Jina la chumba"), "setup_room_name")
      SetupKind.COURSE -> {
        SheetField(first, { first = it; feedback = null }, viewModel.t("Course code", "Msimbo wa kozi"), "setup_course_code")
        Spacer(modifier = Modifier.height(10.dp))
        SheetField(second, { second = it; feedback = null }, viewModel.t("Course name", "Jina la kozi"), "setup_course_title")
        Spacer(modifier = Modifier.height(10.dp))
        PickerField(
          label = viewModel.t("Programme (optional)", "Programu (hiari)"),
          value = programme,
          placeholder = viewModel.t("Not in a programme", "Haiko kwenye programu"),
          onClick = { picking = true },
          tag = "setup_course_programme"
        )
      }
      SetupKind.IMPORT -> {
        Text(
          viewModel.t(
            "One course per line: code, name, programme (the programme is optional).",
            "Kozi moja kwa mstari: msimbo, jina, programu (programu ni hiari)."
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = list,
          onValueChange = { list = it; feedback = null },
          placeholder = { Text("CS101, Introduction to Programming, BSc Computer Science\nMT101, Calculus") },
          minLines = 6,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("setup_import_text")
        )
      }
    }

    feedback?.let { (ok, text) ->
      Spacer(modifier = Modifier.height(10.dp))
      Text(text, color = if (ok) SuccessGreen else DangerRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("setup_feedback"))
    }
  }

  if (picking) {
    PickerSheet(
      title = viewModel.t("Programme", "Programu"),
      options = listOf(PickerItem("", viewModel.t("Not in a programme", "Haiko kwenye programu"))) + programmes.map { PickerItem(it, it) },
      selectedId = programme ?: "",
      onSelect = { programme = it.ifBlank { null } },
      onDismiss = { picking = false }
    )
  }
}

@Composable
internal fun SheetField(value: String, onChange: (String) -> Unit, label: String, tag: String, password: Boolean = false) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
    else androidx.compose.ui.text.input.VisualTransformation.None,
    modifier = Modifier.fillMaxWidth().testTag(tag)
  )
}
