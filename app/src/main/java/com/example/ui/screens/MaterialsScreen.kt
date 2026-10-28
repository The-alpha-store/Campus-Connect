package com.example.ui.screens

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.DropdownMenu

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.filled.BookmarkBorder

import androidx.compose.material.icons.filled.BookmarkAdded

import com.example.ui.components.ConfirmSheet

import com.example.ui.components.SoftActionButton

import com.example.ui.components.FileKindBadge

import com.example.data.docs.DocumentKind

import com.example.util.DeadlineTime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttachmentFile
import com.example.data.model.ContentApprovalStatus
import com.example.data.model.Role
import com.example.data.model.StudyMaterial
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberLight
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
  viewModel: CampusViewModel,
  onOpenUploadDialog: () -> Unit,
  onOpenProcessedDocuments: () -> Unit,
  onPreviewDocument: (AttachmentFile, authorName: String, courseCode: String) -> Unit,
  onAnalyseDocument: (AttachmentFile, courseCode: String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  var deleteTarget by remember { mutableStateOf<StudyMaterial?>(null) }
  val user by viewModel.currentUser.collectAsState()
  val materials by viewModel.materials.collectAsState()
  var query by remember { mutableStateOf("") }
  var collapsed by remember { mutableStateOf(setOf<String>()) }
  val shown = materials.filter {
    query.isBlank() || it.title.contains(query, true) || it.courseCode.contains(query, true) ||
      it.courseTitle.contains(query, true) || it.description.contains(query, true)
  }
  // One section per subject, so everything for a module is in one place.
  val grouped = shown.groupBy { it.courseCode }.toSortedMap()

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = viewModel.getString("tab_materials"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Offline course notes, slides & handouts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Card(
        onClick = onOpenProcessedDocuments,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text("AI Summaries (Room DB)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Browse saved analyses offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
          Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Open summaries", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
      }
      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(viewModel.t("Search subject or file", "Tafuta somo au faili")) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("materials_search")
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (grouped.isEmpty()) {
        Text(
          text = if (materials.isEmpty()) viewModel.t("No study materials yet.", "Hakuna vifaa vya masomo bado.")
          else viewModel.t("No file matches your search.", "Hakuna faili inayolingana na utafutaji."),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp)
        )
      }

      // Materials List
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        grouped.forEach { (code, list) ->
          item(key = "subject_$code") {
            val open = code !in collapsed
            SubjectHeader(
              code = code,
              title = list.first().courseTitle,
              count = list.size,
              expanded = open,
              modifier = Modifier.animateItem()
            ) { collapsed = if (open) collapsed + code else collapsed - code }
          }
          if (code !in collapsed) {
            items(list, key = { it.id }) { mat ->
          Box(modifier = Modifier.animateItem()) {
          val canModerate = viewModel.canEditOrDelete(mat.uploaderId)

          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
              )
              .testTag("material_item_${mat.id}")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                FileKindBadge(kind = DocumentKind.of(mat.attachment.fileName, mat.attachment.mimeType), size = 46.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = mat.courseCode,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = DeepBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "• ${mat.attachment.fileSize}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mat.approvalStatus == ContentApprovalStatus.PENDING_REVIEW) {
                      Spacer(modifier = Modifier.width(6.dp))
                      StatusBadge(
                        text = "Pending",
                        backgroundColor = WarmAmberLight,
                        contentColor = Color(0xFFB45309)
                      )
                    } else if (mat.approvalStatus == ContentApprovalStatus.DISAPPROVED) {
                      Spacer(modifier = Modifier.width(6.dp))
                      StatusBadge(
                        text = "Disapproved",
                        backgroundColor = DangerRedLight,
                        contentColor = DangerRed
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = mat.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  Text(
                    text = "By ${mat.uploadedBy} • ${DeadlineTime.friendly(mat.uploadedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Save for offline: one clear 48 dp toggle instead of a tiny pill
                IconButton(
                  onClick = { viewModel.toggleMaterialDownloaded(mat.id) },
                  modifier = Modifier.size(48.dp).testTag("download_toggle_${mat.id}")
                ) {
                  Icon(
                    imageVector = if (mat.isDownloaded) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                    contentDescription = if (mat.isDownloaded) "Saved for offline. Tap to remove." else "Save for offline",
                    tint = if (mat.isDownloaded) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Actions: read, analyse, and (for the owner of the file or an administrator) more
              val canAnalyse = mat.attachment.uriString != null &&
                DocumentKind.isAnalysable(mat.attachment.fileName, mat.attachment.mimeType)
              var menuOpen by remember { mutableStateOf(false) }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                SoftActionButton(
                  label = "Read", icon = Icons.Default.Visibility,
                  onClick = { onPreviewDocument(mat.attachment, mat.uploadedBy, mat.courseCode) },
                  modifier = Modifier.weight(1f), tag = "view_doc_btn_${mat.id}"
                )
                if (canAnalyse) {
                  SoftActionButton(
                    label = "AI summary", icon = Icons.Default.AutoAwesome,
                    onClick = { onAnalyseDocument(mat.attachment, mat.courseCode) },
                    color = Color(0xFF7C3AED), modifier = Modifier.weight(1f), tag = "analyse_doc_btn_${mat.id}"
                  )
                }
                if (canModerate) {
                  Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp).testTag("more_mat_${mat.id}")) {
                      Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                      if (user.role == Role.ADMIN && mat.approvalStatus != ContentApprovalStatus.APPROVED) {
                        DropdownMenuItem(
                          text = { Text("Approve") },
                          leadingIcon = { Icon(Icons.Default.ThumbUp, contentDescription = null, tint = SuccessGreen) },
                          onClick = { menuOpen = false; viewModel.setMaterialApproval(mat.id, ContentApprovalStatus.APPROVED) },
                          modifier = Modifier.testTag("approve_mat_${mat.id}")
                        )
                      }
                      if (user.role == Role.ADMIN && mat.approvalStatus != ContentApprovalStatus.DISAPPROVED) {
                        DropdownMenuItem(
                          text = { Text("Disapprove") },
                          leadingIcon = { Icon(Icons.Default.ThumbDown, contentDescription = null, tint = DangerRed) },
                          onClick = { menuOpen = false; viewModel.setMaterialApproval(mat.id, ContentApprovalStatus.DISAPPROVED) },
                          modifier = Modifier.testTag("disapprove_mat_${mat.id}")
                        )
                      }
                      DropdownMenuItem(
                        text = { Text("Delete", color = DangerRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
                        onClick = { menuOpen = false; deleteTarget = mat },
                        modifier = Modifier.testTag("delete_mat_${mat.id}")
                      )
                    }
                  }
                }
              }
            }
          }
        
          }
        }
          }
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }

    if (user.role == Role.LECTURER || user.role == Role.ADMIN) {
      FloatingActionButton(
        onClick = onOpenUploadDialog,
        containerColor = DeepBlue,
        contentColor = Color.White,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(20.dp)
          .testTag("fab_upload_material")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Upload Study Material"
        )
      }
    }
  }

  deleteTarget?.let { mat ->
    ConfirmSheet(
      title = "Delete this file?",
      message = "\"${mat.title}\" will be removed for everyone in ${mat.courseCode}. This cannot be undone.",
      confirmLabel = "Delete file",
      onDismiss = { deleteTarget = null },
      onConfirm = { viewModel.deleteMaterial(mat.id); deleteTarget = null },
      confirmTag = "confirm_delete_material_btn"
    )
  }
}


@Composable
private fun SubjectHeader(
  code: String,
  title: String,
  count: Int,
  expanded: Boolean,
  modifier: Modifier = Modifier,
  onToggle: () -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onToggle() }
      .padding(horizontal = 4.dp, vertical = 10.dp)
      .testTag("subject_header_$code"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
      Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Icon(
      imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
