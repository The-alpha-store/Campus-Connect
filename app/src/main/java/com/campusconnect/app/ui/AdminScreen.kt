package com.campusconnect.app.ui

import com.campusconnect.app.tr
import com.campusconnect.app.trf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.campusconnect.app.EAT
import com.campusconnect.app.data.Profile
import com.campusconnect.app.data.Programme
import java.time.LocalDate

@Composable
fun AdminScreen(vm: AdminVm, lookups: LookupsVm, onBack: (() -> Unit)? = null) {
    var tab by remember { mutableIntStateOf(0) }
    val pending by vm.pending.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Administration", onBack)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(trf("Approvals (%d)", pending.items.size)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Setup")) })
        }
        if (tab == 0) ApprovalsTab(vm, lookups) else SetupTab(vm, lookups)
    }
}

@Composable
private fun ApprovalsTab(vm: AdminVm, lookups: LookupsVm) {
    val pending by vm.pending.state.collectAsState()
    val programmes by lookups.programmes.state.collectAsState()
    val busy by vm.busy.collectAsState()
    RefreshList(
        state = pending, onRefresh = { vm.pending.refresh() },
        emptyTitle = "No pending accounts", emptyText = "New sign-ups waiting for approval will appear here.",
    ) { list ->
        items(list, key = { it.id }) { p -> PendingCard(p, programmes.items, busy, vm) }
    }
}

@Composable
private fun PendingCard(p: Profile, programmes: List<Programme>, busy: Boolean, vm: AdminVm) {
    var role by remember(p.id) { mutableStateOf("student") }
    var programme by remember(p.id) { mutableStateOf<Programme?>(null) }
    AppCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(p.fullName, style = MaterialTheme.typography.titleSmall)
            Text(p.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(role == "student", { role = "student" }, { Text(tr("Student")) })
                FilterChip(role == "lecturer", { role = "lecturer" }, { Text(tr("Lecturer")) })
                FilterChip(role == "admin", { role = "admin" }, { Text(tr("Admin")) })
            }
            if (role == "student") {
                PickerDropdown("Programme", programmes, programme, { "${it.code} - ${it.name}" }, { programme = it }, allowNone = true, noneLabel = "Not set")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.reject(p) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(tr("Reject")) }
                Button(onClick = { vm.approve(p, role, if (role == "student") programme?.id else null) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(tr("Approve"))
                }
            }
        }
    }
}

@Composable
private fun SetupTab(vm: AdminVm, lookups: LookupsVm) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val done = { dialog = null }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                tr("Set these up in order: programme, term, course, class offering, then enrol students."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SetupCard("New programme", "e.g. BSc Information Technology", Icons.Default.School) { dialog = "programme" } }
        item { SetupCard("New academic term", "Semester dates; mark the current one", Icons.Default.Event) { dialog = "term" } }
        item { SetupCard("New course", "Course code and title", Icons.Default.MenuBook) { dialog = "course" } }
        item { SetupCard("New class offering", "A course taught in a term by a lecturer", Icons.Default.Groups) { dialog = "offering" } }
        item { SetupCard("Enrol a student", "Add a student to a class offering", Icons.Default.PersonAdd) { dialog = "enroll" } }
    }
    when (dialog) {
        "programme" -> ProgrammeDialog(vm, done)
        "term" -> TermDialog(vm, done)
        "course" -> CourseDialog(vm, lookups, done)
        "offering" -> OfferingDialog(vm, lookups, done)
        "enroll" -> EnrollDialog(vm, lookups, done)
    }
}

@Composable
private fun SetupCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tr(title), style = MaterialTheme.typography.titleSmall)
                Text(tr(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun ProgrammeDialog(vm: AdminVm, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsState()
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    FormDialog("New programme", code.isNotBlank() && name.isNotBlank(), busy, onDismiss, { vm.createProgramme(code, name, onDismiss) }) {
        OutlinedTextField(code, { code = it }, label = { Text(tr("Code (e.g. BSC-IT)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(name, { name = it }, label = { Text(tr("Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TermDialog(vm: AdminVm, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsState()
    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(LocalDate.now(EAT)) }
    var end by remember { mutableStateOf(LocalDate.now(EAT).plusMonths(4)) }
    var current by remember { mutableStateOf(true) }
    FormDialog("New academic term", name.isNotBlank() && end.isAfter(start), busy, onDismiss, { vm.createTerm(name, start.toString(), end.toString(), current, onDismiss) }) {
        OutlinedTextField(name, { name = it }, label = { Text(tr("Name (e.g. 2026/27 Semester 1)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField("Start", start) { start = it }
        DateField("End", end) { end = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(current, { current = it })
            Text(tr("Current term"))
        }
    }
}

@Composable
private fun CourseDialog(vm: AdminVm, lookups: LookupsVm, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val programmes by lookups.programmes.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var programme by remember { mutableStateOf<Programme?>(null) }
    FormDialog("New course", code.isNotBlank() && title.isNotBlank(), busy, onDismiss, { vm.createCourse(code, title, programme?.id, onDismiss) }) {
        OutlinedTextField(code, { code = it }, label = { Text(tr("Code (e.g. IT101)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(title, { title = it }, label = { Text(tr("Title")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        PickerDropdown("Programme", programmes.items, programme, { "${it.code} - ${it.name}" }, { programme = it }, allowNone = true, noneLabel = "Not set")
    }
}

@Composable
private fun OfferingDialog(vm: AdminVm, lookups: LookupsVm, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val courses by lookups.courses.state.collectAsState()
    val terms by lookups.terms.state.collectAsState()
    val lecturers by lookups.lecturers.state.collectAsState()
    var course by remember { mutableStateOf<com.campusconnect.app.data.Course?>(null) }
    var term by remember { mutableStateOf<com.campusconnect.app.data.Term?>(null) }
    var lecturer by remember { mutableStateOf<Profile?>(null) }
    var cohort by remember { mutableStateOf("main") }
    val chosenTerm = term ?: terms.items.firstOrNull { it.isCurrent } ?: terms.items.firstOrNull()
    FormDialog("New class offering", course != null && chosenTerm != null, busy, onDismiss, {
        vm.createOffering(course!!.id, chosenTerm!!.id, lecturer?.id, cohort, onDismiss)
    }) {
        PickerDropdown("Course", courses.items, course, { "${it.code} - ${it.title}" }, { course = it })
        PickerDropdown("Term", terms.items, chosenTerm, { it.name }, { term = it })
        PickerDropdown("Lecturer", lecturers.items, lecturer, { it.fullName }, { lecturer = it }, allowNone = true, noneLabel = "Not assigned")
        OutlinedTextField(cohort, { cohort = it }, label = { Text(tr("Cohort / group")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EnrollDialog(vm: AdminVm, lookups: LookupsVm, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val offerings by lookups.offerings.state.collectAsState()
    val students by lookups.students.state.collectAsState()
    var offering by remember { mutableStateOf<com.campusconnect.app.data.Offering?>(null) }
    var student by remember { mutableStateOf<Profile?>(null) }
    FormDialog("Enrol a student", offering != null && student != null, busy, onDismiss, { vm.enroll(offering!!.id, student!!.id, onDismiss) }, confirmText = "Enrol") {
        PickerDropdown("Class offering", offerings.items, offering, { it.label }, { offering = it })
        SearchPick("Student", students.items, student, { "${it.fullName} (${it.email.orEmpty()})" }, { student = it })
    }
}
