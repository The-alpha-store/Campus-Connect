package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProcessedDocumentEntity
import com.example.data.local.ProcessedDocumentRepository
import com.example.ui.components.AppBottomSheet
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProcessedDocumentsScreen(
  onAnalyzeDocument: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val repository = remember(context) { ProcessedDocumentRepository(context) }
  val documents by repository.observeDocuments().collectAsState(initial = emptyList())
  var query by remember { mutableStateOf("") }
  var selectedCourse by remember { mutableStateOf("ALL") }
  var selectedCategory by remember { mutableStateOf("All Types") }
  var selectedDocument by remember { mutableStateOf<ProcessedDocumentEntity?>(null) }
  var confirmDelete by remember { mutableStateOf<ProcessedDocumentEntity?>(null) }
  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current
  val courses = listOf("ALL", "CS210", "IS201", "NW302")
  val categories = listOf("All Types", "Notes", "Syllabus", "Assignments", "Exam Revision", "Reference")
  val filtered = documents.filter { doc ->
    val textMatch = query.isBlank() || listOf(doc.title, doc.fileName, doc.extractedSummary, doc.keyTopics, doc.courseCode, doc.courseTitle, doc.tags)
      .any { it.contains(query.trim(), ignoreCase = true) }
    textMatch && (selectedCourse == "ALL" || doc.courseCode.equals(selectedCourse, true)) &&
      (selectedCategory == "All Types" || doc.category.equals(selectedCategory, true))
  }

  Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("AI Summaries", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${documents.size} documents stored on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      IconButton(onClick = onAnalyzeDocument) {
        Icon(Icons.Default.Article, contentDescription = "Add a document", tint = MaterialTheme.colorScheme.primary)
      }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
      singleLine = true, shape = RoundedCornerShape(16.dp),
      placeholder = { Text("Search summaries, topics or course codes") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Text("×") } }
    )
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(courses) { course ->
        FilterChip(selected = selectedCourse == course, onClick = { selectedCourse = course }, label = { Text(course) })
      }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categories) { category ->
        FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
      }
    }
    Spacer(Modifier.height(8.dp))
    if (filtered.isEmpty()) {
      Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(if (documents.isEmpty()) "Your processed summaries will appear here" else "No summaries match these filters", fontWeight = FontWeight.SemiBold)
        Text("Add a course document to your library. AI extraction integration is required to generate summaries.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAnalyzeDocument) { Text("Add document") }
      }
    } else {
      LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filtered, key = { it.id }) { document ->
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                  Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    if (document.mimeType.contains("pdf", true)) Icons.Default.Description else Icons.Default.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                  Text(document.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                  Text("${document.courseCode} • ${document.courseTitle}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { confirmDelete = document }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete summary", tint = MaterialTheme.colorScheme.error) }
              }
              Spacer(Modifier.height(10.dp))
              Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)) {
                Text("“ SUMMARY PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(document.extractedSummary, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
              }
              Spacer(Modifier.height(8.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                document.keyTopics.split(',').map(String::trim).filter(String::isNotBlank).take(5).forEach { topic ->
                  AssistChip(onClick = { query = topic }, label = { Text(topic, style = MaterialTheme.typography.labelSmall) })
                }
              }
              Spacer(Modifier.height(6.dp))
              Text("${formatSize(document.fileSize)}  •  ${document.pageCount} pages  •  ${document.wordCount} words  •  ${document.readingTimeMinutes} min read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(document.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedDocument = document }) { Text("View full analysis") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                  val uri = document.sourceUri
                  if (uri.isNullOrBlank()) Toast.makeText(context, "Original file is not available on this device.", Toast.LENGTH_SHORT).show()
                  else runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), document.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                  }.onFailure { Toast.makeText(context, "No compatible document viewer found.", Toast.LENGTH_SHORT).show() }
                }) { Icon(Icons.Default.MenuBook, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Read") }
              }
            }
          }
        }
      }
    }
  }

  selectedDocument?.let { doc ->
    AppBottomSheet(onDismissRequest = { selectedDocument = null }) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
        Text(doc.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${doc.courseCode} • ${doc.courseTitle}", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Executive summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(doc.extractedSummary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text("Syllabus concepts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(doc.keyTopics.split(',').joinToString("  •  ") { it.trim() })
        Spacer(Modifier.height(12.dp))
        Text("Learning objectives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        doc.learningObjectives.lines().filter(String::isNotBlank).forEach { Text("✓  $it", modifier = Modifier.padding(top = 4.dp)) }
        Spacer(Modifier.height(12.dp))
        Text("Study tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        doc.studyTips.lines().filter(String::isNotBlank).forEach { Text("•  $it", modifier = Modifier.padding(top = 4.dp)) }
        Spacer(Modifier.height(12.dp))
        Text("Saved on this device • Room SQLite", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(onClick = {
            clipboard.setText(AnnotatedString("${doc.title}\n\n${doc.extractedSummary}\n\nKey topics: ${doc.keyTopics}\n\nLearning objectives:\n${doc.learningObjectives}\n\nStudy tips:\n${doc.studyTips}"))
            Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
          }) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Copy") }
          TextButton(onClick = {
            val text = "${doc.title}\n\n${doc.extractedSummary}\n\nKey topics: ${doc.keyTopics}\n\nLearning objectives:\n${doc.learningObjectives}\n\nStudy tips:\n${doc.studyTips}"
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, doc.title).putExtra(Intent.EXTRA_TEXT, text), "Share analysis"))
          }) { Icon(Icons.Default.IosShare, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Share") }
        }
        Spacer(Modifier.height(12.dp))
      }
    }
  }

  confirmDelete?.let { doc ->
    AppBottomSheet(onDismissRequest = { confirmDelete = null }) {
      Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Delete saved summary?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${doc.title} will be removed from this device's Room database. The original file will not be deleted.")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
          TextButton(onClick = { scope.launch { repository.deleteLocal(doc.id); confirmDelete = null; Toast.makeText(context, "Summary deleted", Toast.LENGTH_SHORT).show() } }) { Text("Delete") }
        }
      }
    }
  }
}

private fun formatSize(bytes: Long): String = when {
  bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
  bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
  else -> "$bytes B"
}