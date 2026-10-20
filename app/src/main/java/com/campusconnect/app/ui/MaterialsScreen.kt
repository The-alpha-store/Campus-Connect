package com.campusconnect.app.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.campusconnect.app.*
import com.campusconnect.app.data.ListState
import com.campusconnect.app.data.Material
import com.campusconnect.app.data.Offering
import com.campusconnect.app.data.Profile
import com.campusconnect.app.ui.theme.Amber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_BYTES = 10L * 1024 * 1024

fun formatSize(bytes: Long?): String = when {
    bytes == null -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1048576.0)
}

@Composable
fun MaterialsScreen(
    me: Profile,
    state: ListState<Material>,
    offerings: ListState<Offering>,
    vm: MaterialsVm,
    onView: (String) -> Unit,
) {
    val staff = me.role != "student"
    var query by rememberSaveable { mutableStateOf("") }
    var courseFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val downloading by vm.downloading.collectAsState()
    val savedTick by vm.savedTick.collectAsState()
    val busy by vm.busy.collectAsState()
    var uploading by remember { mutableStateOf(false) }
    var deleteFor by remember { mutableStateOf<Material?>(null) }

    val courses = remember(state.items) {
        state.items.map { it.offeringId to (it.offering?.courses?.code ?: "?") }.distinct()
    }
    val filtered = state.items.filter {
        (courseFilter == null || it.offeringId == courseFilter) &&
            (query.isBlank() || it.title.contains(query, true)) &&
            (staff || it.status == "approved")
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Study materials")
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text(tr("Search materials")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            if (courses.size > 1) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(courseFilter == null, { courseFilter = null }, { Text(tr("All")) }) }
                    items(courses, key = { it.first }) { (id, code) ->
                        FilterChip(courseFilter == id, { courseFilter = if (courseFilter == id) null else id }, { Text(code) })
                    }
                }
            }
            RefreshList(
                state = state.copy(items = filtered), onRefresh = { vm.list.refresh() }, modifier = Modifier.weight(1f),
                emptyTitle = if (query.isBlank()) "No study materials yet" else "No matches",
                emptyText = if (staff) "Tap Upload to share a PDF with your class." else "Notes and past papers from your lecturers will appear here.",
            ) { list ->
                items(list, key = { it.id }) { m ->
                    val saved = remember(m.id, savedTick) { vm.isSaved(m) }
                    val canDelete = me.role == "admin" || (staff && m.uploaderId == me.id)
                    AppCard(onClick = { vm.open(m) { onView(m.id) } }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(m.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                Text(
                                    listOf(m.offering?.courses?.code, formatSize(m.sizeBytes).ifBlank { null }, m.createdAt.dateEat())
                                        .filterNotNull().joinToString("  |  "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (m.status != "approved") StatusBadge(tr(m.status.replaceFirstChar { it.uppercase() }), Amber)
                            }
                            when {
                                downloading == m.id -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                saved -> Icon(Icons.Default.DownloadDone, tr("Saved offline"), tint = MaterialTheme.colorScheme.primary)
                            }
                            if (canDelete) IconButton(onClick = { deleteFor = m }) {
                                Icon(Icons.Default.DeleteOutline, tr("Delete"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        if (staff) {
            ExtendedFloatingActionButton(
                onClick = { uploading = true },
                icon = { Icon(Icons.Default.UploadFile, null) },
                text = { Text(tr("Upload")) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    deleteFor?.let { m ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(tr("Delete this material?")) },
            text = { Text(m.title) },
            confirmButton = { TextButton(onClick = { vm.delete(m); deleteFor = null }) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text(tr("Cancel")) } },
        )
    }
    if (uploading) {
        UploadDialog(
            offerings = offerings.items.filter { me.role == "admin" || it.lecturerId == me.id },
            busy = busy, vm = vm, onDismiss = { uploading = false },
        )
    }
}

@Composable
private fun UploadDialog(offerings: List<Offering>, busy: Boolean, vm: MaterialsVm, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var picked by remember { mutableStateOf<Offering?>(null) }
    val offering = picked ?: offerings.firstOrNull()
    var title by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf<Long?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            uri = u
            ctx.contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) fileName = c.getString(ni).orEmpty()
                    if (si >= 0 && !c.isNull(si)) fileSize = c.getLong(si)
                }
            }
            if (title.isBlank()) title = fileName.removeSuffix(".pdf").removeSuffix(".PDF")
        }
    }
    val tooBig = (fileSize ?: 0L) > MAX_BYTES

    FormDialog(
        title = "Upload study material",
        enabled = offering != null && uri != null && title.isNotBlank() && !tooBig,
        busy = busy, confirmText = "Upload", onDismiss = onDismiss,
        onConfirm = { vm.upload(offering!!.id, title, uri!!, onDismiss) },
    ) {
        if (offerings.isEmpty()) {
            Text(tr("You have no assigned courses yet. Ask an administrator to assign you to a class."), color = MaterialTheme.colorScheme.error)
        } else {
            PickerDropdown("Course", offerings, offering, { it.label }, { picked = it })
        }
        OutlinedButton(onClick = { launcher.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (uri == null) tr("Choose PDF") else fileName.ifBlank { tr("PDF selected") })
        }
        if (tooBig) Text(tr("PDF is larger than 10 MB."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        else if (fileSize != null) Text(formatSize(fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(title, { title = it }, label = { Text(tr("Title")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(
            tr("Only PDF files up to 10 MB. Students in the course are notified."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ----------------------------------------------------------------------------- PDF viewer

/** Renders pages with the built-in PdfRenderer. No external app, no extra permissions, works offline. */
private class PdfDoc(file: File) : AutoCloseable {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    val pageCount: Int get() = renderer.pageCount

    suspend fun render(index: Int, widthPx: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.Default) {
            renderer.openPage(index).use { page ->
                val h = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(AndroidColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }

    override fun close() {
        try { renderer.close(); pfd.close() } catch (_: Exception) {}
    }
}

@Composable
fun PdfViewerScreen(id: String, state: ListState<Material>, vm: MaterialsVm, onBack: () -> Unit) {
    val m = state.items.firstOrNull { it.id == id }
    val file = m?.let { vm.localFile(it) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(m?.title ?: tr("Document"), onBack)
        if (file == null || !file.exists()) {
            EmptyState("Document not available", "Go back and open it again while online.")
        } else {
            val doc = remember(file.path) { try { PdfDoc(file) } catch (e: Exception) { null } }
            DisposableEffect(doc) { onDispose { doc?.close() } }
            if (doc == null || doc.pageCount == 0) {
                EmptyState("This PDF could not be opened", "The file may be damaged. Ask your lecturer to upload it again.")
            } else {
                PdfPages(doc)
            }
        }
    }
}

@Composable
private fun PdfPages(doc: PdfDoc) {
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(count = doc.pageCount, key = { it }) { index ->
                val bitmap by produceState<Bitmap?>(null, doc, index, widthPx) { value = doc.render(index, widthPx) }
                val b = bitmap
                if (b != null) {
                    Image(b.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                } else {
                    Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) {
            Text(
                "${listState.firstVisibleItemIndex + 1} / ${doc.pageCount}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
