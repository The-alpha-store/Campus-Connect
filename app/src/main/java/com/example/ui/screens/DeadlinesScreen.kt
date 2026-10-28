package com.example.ui.screens

import androidx.compose.material.icons.filled.EventAvailable

import com.example.ui.components.EmptyState

import com.example.ui.components.SoftActionButton

import com.example.ui.util.rememberNow

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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.DeadlineItem
import com.example.data.model.Role
import com.example.ui.components.DeadlineCard
import com.example.ui.theme.DeepBlue
import com.example.viewmodel.CampusViewModel
import com.example.viewmodel.ScreenTab

@Composable
fun DeadlinesScreen(
  viewModel: CampusViewModel,
  onOpenAddDeadlineDialog: () -> Unit,
  onSubmitWorkClick: (DeadlineItem) -> Unit,
  onEditDeadline: (DeadlineItem) -> Unit = {},
  onOpenAttachment: (AttachmentFile, String?, String?) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier
) {
  val user by viewModel.currentUser.collectAsState()
  val deadlines by viewModel.deadlines.collectAsState()
  val submissions by viewModel.submissions.collectAsState()
  val swahili = viewModel.t("a", "b") == "b"
  val now = rememberNow()
  // A student's own handed-in work, newest first.
  val mySubmissions = if (user.role == Role.STUDENT) submissions.filter { it.studentId == user.id } else emptyList()

  val pendingDeadlines = deadlines.filter { !it.isCompleted }
  val completedDeadlines = deadlines.filter { it.isCompleted }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = viewModel.getString("tab_calendar"),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = viewModel.t("Assignments, CATs & submission dropboxes", "Kazi, CAT na masanduku ya kuwasilisha"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(12.dp))
      // The two shortcuts get a full row of their own, so they are big enough to read and to tap.
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SoftActionButton(
          label = viewModel.t("Quizzes", "Majaribio"), icon = Icons.Default.Quiz,
          onClick = { viewModel.setTab(ScreenTab.QUIZZES) },
          modifier = Modifier.weight(1f), tag = "nav_to_quizzes_btn"
        )
        SoftActionButton(
          label = if (user.role == Role.STUDENT) viewModel.t("My works", "Kazi zangu") else viewModel.t("All submissions", "Kazi zote"),
          icon = Icons.Default.AssignmentTurnedIn,
          onClick = { viewModel.setTab(ScreenTab.SUBMISSIONS) },
          modifier = Modifier.weight(1f), tag = "nav_to_submissions_btn"
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (deadlines.isEmpty()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          EmptyState(
            icon = Icons.Default.EventAvailable,
            title = viewModel.t("No deadlines yet", "Hakuna tarehe za mwisho bado"),
            body = if (user.role == Role.STUDENT)
              viewModel.t("Assignments and tests your lecturers set will appear here, with a reminder as each one gets close.", "Kazi na mitihani watakayoweka wahadhiri itaonekana hapa, pamoja na ukumbusho.")
            else viewModel.t("Tap + to set the first deadline for one of your courses.", "Gusa + kuweka tarehe ya kwanza kwa kozi yako.")
          )
        }
      }

      if (deadlines.isNotEmpty()) LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (pendingDeadlines.isNotEmpty()) {
          item {
            Text(
              text = "Upcoming Deadlines (${pendingDeadlines.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          items(pendingDeadlines, key = { it.id }) { dl ->
          Box(modifier = Modifier.animateItem()) {
            val canModerate = viewModel.canEditOrDelete(dl.lecturerId)

            DeadlineCard(
              deadline = dl,
              onToggleComplete = { viewModel.toggleDeadlineCompleted(dl.id) },
              onSubmitClick = if (user.role == Role.STUDENT) { { onSubmitWorkClick(dl) } } else null,
              onEditClick = if (canModerate) { { onEditDeadline(dl) } } else null,
              onDeleteClick = if (canModerate) { { viewModel.deleteDeadline(dl.id) } } else null,
              canModerate = canModerate,
              onAttachmentClick = dl.attachment?.let { att -> { onOpenAttachment(att, dl.lecturerName, dl.courseCode) } },
              onViewMySubmission = mySubmissions.firstOrNull { it.deadlineId == dl.id }
                ?.let { sub -> { onOpenAttachment(sub.attachment, sub.studentName, sub.courseCode) } },
              swahili = swahili,
              now = now
            )
          
          }
        }
        }

        if (completedDeadlines.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Submitted / Completed (${completedDeadlines.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          items(completedDeadlines, key = { it.id }) { dl ->
          Box(modifier = Modifier.animateItem()) {
            val canModerate = viewModel.canEditOrDelete(dl.lecturerId)

            DeadlineCard(
              deadline = dl,
              onToggleComplete = { viewModel.toggleDeadlineCompleted(dl.id) },
              onSubmitClick = if (user.role == Role.STUDENT) { { onSubmitWorkClick(dl) } } else null,
              onEditClick = if (canModerate) { { onEditDeadline(dl) } } else null,
              onDeleteClick = if (canModerate) { { viewModel.deleteDeadline(dl.id) } } else null,
              canModerate = canModerate,
              onAttachmentClick = dl.attachment?.let { att -> { onOpenAttachment(att, dl.lecturerName, dl.courseCode) } },
              onViewMySubmission = mySubmissions.firstOrNull { it.deadlineId == dl.id }
                ?.let { sub -> { onOpenAttachment(sub.attachment, sub.studentName, sub.courseCode) } },
              swahili = swahili,
              now = now
            )
          
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
        onClick = onOpenAddDeadlineDialog,
        containerColor = DeepBlue,
        contentColor = Color.White,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(20.dp)
          .testTag("fab_add_deadline")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Deadline"
        )
      }
    }
  }
}
