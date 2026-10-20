package com.campusconnect.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusconnect.app.*
import com.campusconnect.app.data.Announcement
import com.campusconnect.app.data.ClassSession
import com.campusconnect.app.data.ListState
import com.campusconnect.app.ui.theme.Amber
import com.campusconnect.app.ui.theme.Danger
import com.campusconnect.app.ui.theme.Success
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = content)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text, color = color, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack == null) 16.dp else 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(tr(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionTitle(title: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) TextButton(onClick = onAction) { Text(tr(actionText)) }
    }
}

@Composable
fun SessionCard(s: ClassSession, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cancelled = s.status == "cancelled"
    val accent = when (s.status) {
        "cancelled" -> Danger
        "changed" -> Amber
        else -> MaterialTheme.colorScheme.primary
    }
    AppCard(modifier, onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp)) {
                Text(s.startsAt.timeEat(), style = MaterialTheme.typography.titleMedium)
                Text(s.endsAt.timeEat(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.offering?.courses?.code ?: tr("Class"),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (cancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    s.offering?.courses?.title.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        s.venue ?: tr("Venue to be announced"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (s.status) {
                "cancelled" -> StatusBadge(tr("Cancelled"), Danger)
                "changed" -> StatusBadge(tr("Changed"), Amber)
            }
        }
    }
}

@Composable
fun AnnouncementCard(a: Announcement, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (a.status != "published") StatusBadge(tr(a.status.replaceFirstChar { it.uppercase() }), Amber)
            }
            Text(
                a.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val who = a.profiles?.fullName?.let { "$it  |  " }.orEmpty()
            Text(
                who + (a.publishedAt ?: a.createdAt).relativeTime(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun EmptyState(title: String, text: String = "", icon: ImageVector = Icons.Default.Inbox) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
        Text(tr(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (text.isNotBlank()) Text(tr(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorRetry(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(tr("Try again")) }
    }
}

@Composable
fun OfflineBanner() {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(tr("Offline. Showing saved data."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun SkeletonList(count: Int = 5) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val alpha by t.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Box(
                Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
            )
        }
    }
}

/** Pull-to-refresh list with skeleton, error/retry, offline banner and empty state built in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> RefreshList(
    state: ListState<T>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    emptyTitle: String,
    emptyText: String = "",
    content: LazyListScope.(List<T>) -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.loading && state.items.isNotEmpty(), onRefresh = onRefresh, modifier = modifier) {
        when {
            state.loading && state.items.isEmpty() -> SkeletonList()
            state.error != null && state.items.isEmpty() -> ErrorRetry(state.error, onRefresh)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.offline) item { OfflineBanner() }
                if (state.items.isEmpty()) item { EmptyState(emptyTitle, emptyText) } else content(state.items)
            }
        }
    }
}

@Composable
fun MessageEffect(vm: BaseVm, host: SnackbarHostState) {
    val msg by vm.message.collectAsState()
    LaunchedEffect(msg) { msg?.let { host.showSnackbar(it); vm.clearMessage() } }
}

// ------------------------------------------------------------------ forms

@Composable
fun FormDialog(
    title: String,
    enabled: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Save",
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = enabled && !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(tr(confirmText))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerDropdown(
    label: String,
    items: List<T>,
    selected: T?,
    labelOf: (T) -> String,
    onSelect: (T?) -> Unit,
    allowNone: Boolean = false,
    noneLabel: String = "None",
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = if (selected != null) labelOf(selected) else if (allowNone) tr(noneLabel) else "",
            onValueChange = {}, readOnly = true, label = { Text(tr(label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowNone) DropdownMenuItem(text = { Text(tr(noneLabel)) }, onClick = { onSelect(null); open = false })
            items.forEach { item -> DropdownMenuItem(text = { Text(labelOf(item)) }, onClick = { onSelect(item); open = false }) }
        }
    }
}

@Composable
fun <T> SearchPick(label: String, items: List<T>, selected: T?, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    var q by remember { mutableStateOf("") }
    val matches = remember(q, items) {
        if (q.isBlank()) emptyList() else items.filter { labelOf(it).contains(q, ignoreCase = true) }.take(5)
    }
    Column {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, label = { Text(tr(label)) }, singleLine = true,
            supportingText = { Text(selected?.let { tr("Selected") + ": " + labelOf(it) } ?: tr("Type a name to search")) },
            modifier = Modifier.fillMaxWidth(),
        )
        matches.forEach { item -> TextButton(onClick = { onSelect(item); q = "" }) { Text(labelOf(item)) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Event, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr(label) + ": " + value.format(DateFmt))
    }
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(tr("OK")) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(tr("Cancel")) } },
        ) { DatePicker(state = st) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var pickedDate by remember { mutableStateOf(value.toLocalDate()) }
    OutlinedButton(onClick = { pickedDate = value.toLocalDate(); step = 1 }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Event, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr(label) + ": " + value.format(DayFmt) + ", " + value.format(TimeFmt))
    }
    if (step == 1) {
        val st = rememberDatePickerState(initialSelectedDateMillis = value.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    step = 2
                }) { Text(tr("Next")) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(tr("Cancel")) } },
        ) { DatePicker(state = st) }
    }
    if (step == 2) {
        val ts = rememberTimePickerState(initialHour = value.hour, initialMinute = value.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = { onChange(LocalDateTime.of(pickedDate, LocalTime.of(ts.hour, ts.minute))); step = 0 }) { Text(tr("OK")) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(tr("Cancel")) } },
            text = { TimePicker(state = ts) },
        )
    }
}

@Composable
fun roleColor(role: String): Color = when (role) {
    "admin" -> Danger
    "lecturer" -> Success
    else -> MaterialTheme.colorScheme.primary
}

fun roleLabel(role: String): String = when (role) {
    "admin" -> tr("Administrator")
    "lecturer" -> tr("Lecturer")
    else -> tr("Student")
}
