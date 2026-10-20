package com.campusconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusconnect.app.*
import com.campusconnect.app.data.ClassSession
import com.campusconnect.app.data.Conflict
import com.campusconnect.app.data.ListState
import com.campusconnect.app.data.Offering
import com.campusconnect.app.data.Profile
import com.campusconnect.app.ui.theme.Amber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    me: Profile,
    state: ListState<ClassSession>,
    offerings: ListState<Offering>,
    vm: SessionsVm,
    openEditor: Boolean,
) {
    val staff = me.role != "student"
    val today = LocalDate.now(EAT)
    val days = remember(today) { (0L..13L).map { today.plusDays(it) } }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    var weekMode by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(openEditor) }
    var editing by remember { mutableStateOf<ClassSession?>(null) }
    var actionFor by remember { mutableStateOf<ClassSession?>(null) }
    val busy by vm.busy.collectAsState()

    val daysWithClasses = remember(state.items) { state.items.mapNotNull { it.startsAt.localDateEat() }.toSet() }
    val visible = state.items.filter {
        val d = it.startsAt.localDateEat()
        d != null && if (weekMode) !d.isBefore(selected) && d.isBefore(selected.plusDays(7)) else d == selected
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Timetable")
            SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                SegmentedButton(selected = !weekMode, onClick = { weekMode = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(tr("Day")) }
                SegmentedButton(selected = weekMode, onClick = { weekMode = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(tr("Week")) }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(days, key = { it.toEpochDay() }) { d ->
                    val sel = d == selected
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { selectedEpoch = d.toEpochDay() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val fg = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(d.format(DateTimeFormatter.ofPattern("EEE", Lang.locale)), style = MaterialTheme.typography.labelSmall, color = fg)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = fg)
                        Box(
                            Modifier.padding(top = 2.dp).size(6.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (d in daysWithClasses) (if (sel) fg else Amber) else androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            }
            RefreshList(
                state = state.copy(items = visible),
                onRefresh = { vm.list.refresh() },
                modifier = Modifier.weight(1f),
                emptyTitle = if (weekMode) "No classes this week" else "No classes this day",
                emptyText = if (staff) "Tap Add class to schedule one." else "Pull down to refresh.",
            ) { list ->
                if (weekMode) {
                    val grouped = list.groupBy { it.startsAt.localDateEat()!! }.toSortedMap()
                    grouped.forEach { (date, sessions) ->
                        item(key = "h-$date") {
                            Text(date.format(DayFmt), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        items(sessions.sortedBy { it.startsAt }, key = { it.id }) { s ->
                            SessionCard(s, onClick = if (staff) ({ actionFor = s }) else null)
                        }
                    }
                } else {
                    items(list.sortedBy { it.startsAt }, key = { it.id }) { s ->
                        SessionCard(s, onClick = if (staff) ({ actionFor = s }) else null)
                    }
                }
            }
        }
        if (staff) {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(tr("Add class")) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    actionFor?.let { s ->
        AlertDialog(
            onDismissRequest = { actionFor = null },
            title = { Text(s.offering?.courses?.code ?: "Class") },
            text = { Text("${s.startsAt.dateEat()}  ${s.startsAt.timeEat()} - ${s.endsAt.timeEat()}\n${s.venue ?: tr("Venue to be announced")}") },
            confirmButton = {
                TextButton(onClick = { editing = s; actionFor = null }) { Text(tr("Edit")) }
            },
            dismissButton = {
                if (s.status == "cancelled") TextButton(onClick = { vm.restore(s.id); actionFor = null }) { Text(tr("Restore class")) }
                else TextButton(onClick = { vm.cancel(s.id); actionFor = null }) { Text(tr("Cancel class"), color = MaterialTheme.colorScheme.error) }
            },
        )
    }

    if (creating || editing != null) {
        SessionEditorDialog(
            existing = editing,
            offerings = offerings.items.filter { me.role == "admin" || it.lecturerId == me.id },
            busy = busy,
            vm = vm,
            onDismiss = { creating = false; editing = null },
        )
    }
}

@Composable
private fun SessionEditorDialog(
    existing: ClassSession?,
    offerings: List<Offering>,
    busy: Boolean,
    vm: SessionsVm,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf<Offering?>(null) }
    val offering = picked ?: offerings.firstOrNull { it.id == existing?.offeringId } ?: offerings.firstOrNull()
    var venue by remember { mutableStateOf(existing?.venue.orEmpty()) }
    var start by remember {
        mutableStateOf(
            existing?.startsAt?.toInstantOrNull()?.eat()?.toLocalDateTime()
                ?: LocalDateTime.of(LocalDate.now(EAT), LocalTime.of(8, 0)),
        )
    }
    var end by remember {
        mutableStateOf(existing?.endsAt?.toInstantOrNull()?.eat()?.toLocalDateTime() ?: start.plusHours(2))
    }
    var conflicts by remember { mutableStateOf<List<Conflict>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = if (existing == null) "Add class" else "Edit class",
        enabled = offering != null,
        busy = busy,
        confirmText = if (conflicts.isNotEmpty()) "Save anyway" else "Save",
        onDismiss = onDismiss,
        onConfirm = {
            error = null
            vm.save(
                existingId = existing?.id, offeringId = offering!!.id, venue = venue, start = start, end = end,
                force = conflicts.isNotEmpty(),
                onConflicts = { conflicts = it }, onError = { error = it }, onDone = onDismiss,
            )
        },
    ) {
        if (offerings.isEmpty()) {
            Text(
                tr("You have no assigned courses yet. Ask an administrator to assign you to a class."),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
            )
        } else if (existing == null) {
            PickerDropdown("Course", offerings, offering, { it.label }, { picked = it })
        } else {
            Text(offering?.label.orEmpty(), style = MaterialTheme.typography.titleSmall)
        }
        OutlinedTextField(
            value = venue, onValueChange = { venue = it; conflicts = emptyList() }, label = { Text(tr("Venue / room")) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        DateTimeField("Starts", start) {
            val d = Duration.between(start, end)
            start = it; end = it.plus(d); conflicts = emptyList()
        }
        DateTimeField("Ends", end) { end = it; conflicts = emptyList() }
        if (conflicts.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Possible clash"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    conflicts.forEach {
                        val what = if (it.kind == "venue") tr("Venue already used") else tr("You already teach another class")
                        Text("$what, ${it.startsAt.timeEat()} - ${it.endsAt.timeEat()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start) }
    }
}
