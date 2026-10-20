package com.campusconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusconnect.app.data.AppNotification
import com.campusconnect.app.data.ListState
import com.campusconnect.app.relativeTime
import com.campusconnect.app.tr

@Composable
fun NotificationsScreen(
    state: ListState<AppNotification>,
    vm: NotificationsVm,
    onBack: () -> Unit,
    onOpenNotice: (String) -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMaterials: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Notifications", onBack) {
            if (state.items.any { it.readAt == null }) TextButton(onClick = { vm.markAllRead() }) { Text(tr("Mark all read")) }
        }
        RefreshList(
            state = state, onRefresh = { vm.list.refresh() }, modifier = Modifier.weight(1f),
            emptyTitle = "You are all caught up", emptyText = "New announcements and timetable changes will show here.",
        ) { list ->
            items(list, key = { it.id }) { n ->
                val unread = n.readAt == null
                AppCard(onClick = {
                    if (unread) vm.markRead(n.id)
                    when (n.kind) {
                        "announcement" -> if (n.announcementId != null) onOpenNotice(n.announcementId)
                        "deadline" -> onOpenCalendar()
                        "material" -> onOpenMaterials()
                        else -> onOpenTimetable()
                    }
                }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            when (n.kind) {
                                "timetable" -> Icons.Default.Schedule
                                "deadline" -> Icons.Default.AssignmentLate
                                "material" -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.Campaign
                            },
                            null, tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (n.kind == "announcement") n.title else tr(n.title), style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                            )
                            n.body?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(n.createdAt.relativeTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (unread) Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }
}
