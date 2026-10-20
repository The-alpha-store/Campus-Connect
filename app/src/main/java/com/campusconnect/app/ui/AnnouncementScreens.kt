package com.campusconnect.app.ui

import com.campusconnect.app.tr
import com.campusconnect.app.trf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusconnect.app.data.*
import com.campusconnect.app.dateEat
import com.campusconnect.app.timeEat
import com.campusconnect.app.ui.theme.Amber

@Composable
fun AnnouncementsScreen(
    isStaff: Boolean,
    state: ListState<Announcement>,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = if (query.isBlank()) state.items else state.items.filter {
        it.title.contains(query, true) || it.body.contains(query, true)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Announcements", onBack)
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(tr("Search announcements")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            RefreshList(
                state = state.copy(items = filtered), onRefresh = onRefresh, modifier = Modifier.weight(1f),
                emptyTitle = if (query.isBlank()) "No announcements yet" else "No matches",
                emptyText = if (query.isBlank()) "Official updates will appear here." else "Try a different word.",
            ) { list ->
                items(list, key = { it.id }) { a -> AnnouncementCard(a) { onOpen(a.id) } }
            }
        }
        if (isStaff) {
            ExtendedFloatingActionButton(
                onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text(tr("Post")) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
fun AnnouncementDetailScreen(
    id: String,
    me: Profile,
    announcements: ListState<Announcement>,
    offerings: ListState<Offering>,
    programmes: ListState<Programme>,
    vm: AnnouncementsVm,
    onBack: () -> Unit,
) {
    val a = announcements.items.firstOrNull { it.id == id }
    val busy by vm.busy.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Announcement", onBack)
        if (a == null) {
            if (announcements.loading) SkeletonList(2) else EmptyState("Announcement not found", "It may have been removed.")
            return@Column
        }
        val audience = when (a.audience) {
            "campus" -> tr("Whole campus")
            "programme" -> programmes.items.firstOrNull { it.id == a.programmeId }?.name ?: tr("Programme")
            else -> offerings.items.firstOrNull { it.id == a.offeringId }?.label ?: tr("Course")
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(audience, MaterialTheme.colorScheme.primary)
                if (a.status != "published") StatusBadge(a.status.replaceFirstChar { it.uppercase() }, Amber)
            }
            Text(a.title, style = MaterialTheme.typography.titleLarge)
            val stamp = a.publishedAt ?: a.createdAt
            Text(
                (a.profiles?.fullName?.let { "$it  |  " }.orEmpty()) + stamp.dateEat() + ", " + stamp.timeEat(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Text(a.body, style = MaterialTheme.typography.bodyLarge)
            if (a.status == "draft" && a.authorId == me.id) {
                Button(onClick = { vm.publishDraft(a.id) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(tr("Publish now"))
                }
            }
        }
    }
}

@Composable
fun ComposeAnnouncementScreen(
    me: Profile,
    offerings: ListState<Offering>,
    programmes: ListState<Programme>,
    vm: AnnouncementsVm,
    onBack: () -> Unit,
) {
    val isAdmin = me.role == "admin"
    val myOfferings = offerings.items.filter { isAdmin || it.lecturerId == me.id }
    val busy by vm.busy.collectAsState()
    var audience by rememberSaveable { mutableStateOf("offering") }
    var offering by remember { mutableStateOf<Offering?>(null) }
    var programme by remember { mutableStateOf<Programme?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    val target = when (audience) {
        "offering" -> offering ?: myOfferings.firstOrNull()
        "programme" -> programme ?: programmes.items.firstOrNull()
        else -> true
    }
    val ready = title.isNotBlank() && body.isNotBlank() && title.length <= 160 && body.length <= 5000 && target != null

    fun submit(status: String) {
        val a = NewAnnouncement(
            authorId = me.id, audience = audience,
            programmeId = if (audience == "programme") (programme ?: programmes.items.firstOrNull())?.id else null,
            offeringId = if (audience == "offering") (offering ?: myOfferings.firstOrNull())?.id else null,
            title = title.trim(), body = body.trim(), status = status,
        )
        vm.publish(a, onBack)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("New announcement", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isAdmin) {
                Text(tr("Send to"), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(audience == "campus", { audience = "campus" }, { Text(tr("Whole campus")) })
                    FilterChip(audience == "programme", { audience = "programme" }, { Text(tr("Programme")) })
                    FilterChip(audience == "offering", { audience = "offering" }, { Text(tr("Course")) })
                }
            }
            when (audience) {
                "offering" -> if (myOfferings.isEmpty()) {
                    Text(
                        tr("You have no assigned courses. Ask an administrator to assign you to a class first."),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else PickerDropdown("Course", myOfferings, offering ?: myOfferings.first(), { it.label }, { offering = it })
                "programme" -> PickerDropdown(
                    "Programme", programmes.items, programme ?: programmes.items.firstOrNull(), { "${it.code} - ${it.name}" }, { programme = it },
                )
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text(tr("Title")) }, singleLine = true,
                supportingText = { Text("${title.length}/160") }, isError = title.length > 160, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body, onValueChange = { body = it }, label = { Text(tr("Message")) }, minLines = 6,
                supportingText = { Text("${body.length}/5000") }, isError = body.length > 5000, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                tr("Publishing notifies every student in the selected audience."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { submit("published") }, enabled = ready && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(tr("Publish"))
            }
            OutlinedButton(onClick = { submit("draft") }, enabled = ready && !busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(tr("Save as draft"))
            }
        }
    }
}
