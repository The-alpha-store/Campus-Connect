package com.campusconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campusconnect.app.*
import com.campusconnect.app.data.Assignment
import com.campusconnect.app.data.ClassSession
import com.campusconnect.app.data.ListState
import com.campusconnect.app.data.Offering
import com.campusconnect.app.data.Profile
import com.campusconnect.app.ui.theme.Amber
import com.campusconnect.app.ui.theme.Danger
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DeadlineCard(a: Assignment, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val due = a.dueAt.toInstantOrNull()
    val now = Instant.now()
    val (label, color) = when {
        due == null -> "" to MaterialTheme.colorScheme.primary
        due.isBefore(now) -> tr("Overdue") to Danger
        Duration.between(now, due).toHours() < 24 -> tr("Due soon") to Amber
        else -> trf("In %d days", Duration.between(now, due).toDays()) to MaterialTheme.colorScheme.primary
    }
    AppCard(modifier, onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.14f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AssignmentLate, null, tint = color) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    (a.offering?.courses?.code?.let { "$it  |  " }.orEmpty()) + a.dueAt.dateEat() + ", " + a.dueAt.timeEat(),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (label.isNotBlank()) StatusBadge(label, color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    me: Profile,
    assignments: ListState<Assignment>,
    sessions: ListState<ClassSession>,
    offerings: ListState<Offering>,
    vm: AssignmentsVm,
) {
    val staff = me.role != "student"
    val today = LocalDate.now(EAT)
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val busy by vm.busy.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Assignment?>(null) }

    val deadlineDays = remember(assignments.items) { assignments.items.mapNotNull { it.dueAt.localDateEat() }.toSet() }
    val classDays = remember(sessions.items) {
        sessions.items.filter { it.status != "cancelled" }.mapNotNull { it.startsAt.localDateEat() }.toSet()
    }
    val dayDeadlines = assignments.items.filter { it.dueAt.localDateEat() == selected }.sortedBy { it.dueAt }
    val daySessions = sessions.items.filter { it.startsAt.localDateEat() == selected }.sortedBy { it.startsAt }
    val upcoming = assignments.items.filter { (it.dueAt.toInstantOrNull() ?: Instant.EPOCH).isAfter(Instant.now()) }.take(5)

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = assignments.loading && assignments.items.isNotEmpty(),
            onRefresh = { vm.list.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { ScreenHeader("Calendar") }
                if (assignments.offline) item { Box(Modifier.padding(horizontal = 16.dp)) { OfflineBanner() } }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { monthIndex-- }) { Icon(Icons.Default.ChevronLeft, tr("Previous month")) }
                            Text(
                                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Lang.locale)).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            IconButton(onClick = { monthIndex++ }) { Icon(Icons.Default.ChevronRight, tr("Next month")) }
                            TextButton(onClick = {
                                monthIndex = today.year * 12 + today.monthValue - 1; selectedEpoch = today.toEpochDay()
                            }) { Text(tr("Today")) }
                        }
                        MonthGrid(month, selected, today, deadlineDays, classDays) { selectedEpoch = it.toEpochDay() }
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Legend(Amber, tr("Deadline"))
                            Legend(MaterialTheme.colorScheme.primary, tr("Class"))
                        }
                    }
                }
                item { Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle(selected.format(LongDayFmt)) } }
                if (dayDeadlines.isEmpty() && daySessions.isEmpty()) {
                    item { Box(Modifier.padding(horizontal = 16.dp)) { AppCard { EmptyState("Nothing scheduled", "No classes or deadlines on this day.", Icons.Default.EventAvailable) } } }
                }
                items(dayDeadlines, key = { "d-" + it.id }) { Box(Modifier.padding(horizontal = 16.dp)) { DeadlineCard(it) { detail = it } } }
                items(daySessions, key = { "s-" + it.id }) { Box(Modifier.padding(horizontal = 16.dp)) { SessionCard(it) } }
                if (upcoming.isNotEmpty()) {
                    item { Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Upcoming deadlines") } }
                    items(upcoming, key = { "u-" + it.id }) { Box(Modifier.padding(horizontal = 16.dp)) { DeadlineCard(it) { detail = it } } }
                }
            }
        }
        if (staff) {
            ExtendedFloatingActionButton(
                onClick = { adding = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(tr("Add deadline")) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    detail?.let { a ->
        val canDelete = me.role == "admin" || a.createdBy == me.id
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(a.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.offering?.label.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tr("Due") + ": " + a.dueAt.dateEat() + ", " + a.dueAt.timeEat(), style = MaterialTheme.typography.titleSmall)
                    Text(a.instructions?.ifBlank { null } ?: tr("No extra instructions."), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text(tr("OK")) } },
            dismissButton = {
                if (canDelete) TextButton(onClick = { vm.delete(a.id); detail = null }) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }
            },
        )
    }

    if (adding) {
        val mine = offerings.items.filter { me.role == "admin" || it.lecturerId == me.id }
        var picked by remember { mutableStateOf<Offering?>(null) }
        val offering = picked ?: mine.firstOrNull()
        var title by remember { mutableStateOf("") }
        var instructions by remember { mutableStateOf("") }
        var due by remember { mutableStateOf(LocalDateTime.of(today.plusDays(7), LocalTime.of(17, 0))) }
        FormDialog(
            title = "Add deadline",
            enabled = offering != null && title.isNotBlank(),
            busy = busy, onDismiss = { adding = false },
            onConfirm = { vm.create(offering!!.id, title, instructions, due) { adding = false } },
        ) {
            if (mine.isEmpty()) {
                Text(tr("You have no assigned courses yet. Ask an administrator to assign you to a class."), color = MaterialTheme.colorScheme.error)
            } else {
                PickerDropdown("Course", mine, offering, { it.label }, { picked = it })
            }
            OutlinedTextField(title, { title = it }, label = { Text(tr("Title")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(instructions, { instructions = it }, label = { Text(tr("Instructions (optional)")) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            DateTimeField("Due", due) { due = it }
            Text(tr("Students in the course are notified now and again 24 hours before the deadline."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    deadlineDays: Set<LocalDate>,
    classDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val lead = month.atDay(1).dayOfWeek.value - 1  // Monday first
    val cells = lead + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        DayOfWeek.values().forEach { d ->
            Text(
                d.getDisplayName(TextStyle.SHORT, Lang.locale).take(3),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    repeat(rows) { r ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { c ->
                val dayNum = r * 7 + c - lead + 1
                if (dayNum < 1 || dayNum > month.lengthOfMonth()) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val date = month.atDay(dayNum)
                    val isSel = date == selected
                    val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(if (isSel) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                .then(if (date == today && !isSel) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable { onSelect(date) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("$dayNum", style = MaterialTheme.typography.bodyMedium, color = fg)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
                                if (date in deadlineDays) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) fg else Amber))
                                if (date in classDays) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) fg else MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}
