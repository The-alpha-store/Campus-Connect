package com.campusconnect.app.ui

import com.campusconnect.app.tr
import com.campusconnect.app.trf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusconnect.app.EAT
import com.campusconnect.app.LongDayFmt
import com.campusconnect.app.data.Announcement
import com.campusconnect.app.data.Assignment
import com.campusconnect.app.data.ClassSession
import com.campusconnect.app.data.ListState
import com.campusconnect.app.data.Profile
import com.campusconnect.app.localDateEat
import com.campusconnect.app.toInstantOrNull
import java.time.LocalDate

@Composable
fun HomeScreen(
    me: Profile,
    sessions: ListState<ClassSession>,
    announcements: ListState<Announcement>,
    assignments: ListState<Assignment>,
    unread: Int,
    pendingApprovals: Int,
    onBell: () -> Unit,
    onOpenNotice: (String) -> Unit,
    onOpenNotices: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenCalendar: () -> Unit,
    onAddNotice: () -> Unit,
    onAddSession: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val today = LocalDate.now(EAT)
    val todays = sessions.items.filter { it.startsAt.localDateEat() == today }.sortedBy { it.startsAt }
    val latest = announcements.items.firstOrNull { it.status == "published" }
    val first = me.fullName.trim().substringBefore(' ')
    val nextDeadlines = assignments.items
        .filter { (it.dueAt.toInstantOrNull() ?: java.time.Instant.EPOCH).isAfter(java.time.Instant.now()) }
        .take(2)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(trf("Hello, %s!", first), style = MaterialTheme.typography.titleLarge)
                    Text(today.format(LongDayFmt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onBell) {
                    BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
                        Icon(Icons.Default.Notifications, "Notifications")
                    }
                }
            }
        }
        if (sessions.offline || announcements.offline) item { OfflineBanner() }

        if (me.role != "student") item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onAddNotice, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Campaign, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Announce"))
                }
                FilledTonalButton(onClick = onAddSession, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Add class"))
                }
            }
        }

        item { SectionTitle("Today's classes", "Timetable", onOpenTimetable) }
        when {
            sessions.loading && sessions.items.isEmpty() -> item { SkeletonList(2) }
            todays.isEmpty() -> item {
                AppCard { EmptyState("No classes today", "Enjoy the free time or check your timetable.", Icons.Default.EventAvailable) }
            }
            else -> items(todays, key = { it.id }) { SessionCard(it) }
        }

        if (nextDeadlines.isNotEmpty()) {
            item { SectionTitle("Upcoming deadlines", "Calendar", onOpenCalendar) }
            items(nextDeadlines, key = { "dl-" + it.id }) { DeadlineCard(it, onClick = onOpenCalendar) }
        }

        item { SectionTitle("Latest announcement", "See all", onOpenNotices) }
        item {
            if (latest != null) AnnouncementCard(latest) { onOpenNotice(latest.id) }
            else AppCard { EmptyState("No announcements yet", "Official updates from your lecturers will appear here.", Icons.Default.Campaign) }
        }

        if (me.role == "admin" && pendingApprovals > 0) item {
            AppCard(onClick = onOpenAdmin) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(trf("%d account(s) waiting", pendingApprovals), style = MaterialTheme.typography.titleSmall)
                        Text(tr("Review and approve new users"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}
