@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import androidx.compose.foundation.layout.heightIn

import com.example.ui.components.SwitchOption

import com.example.ui.components.SegmentedSwitch

import com.example.ui.util.rememberNow

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.AddSessionSheet
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClassSession
import com.example.data.model.Role
import com.example.data.model.SessionStatus
import com.example.ui.components.SessionCard
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberLight
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimetableScreen(
  viewModel: CampusViewModel,
  onOpenChangeVenueDialog: (String) -> Unit,
  onOpenCancelDialog: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val user by viewModel.currentUser.collectAsState()
  val sessions by viewModel.sessions.collectAsState()
  val is24Hour by viewModel.is24HourFormat.collectAsState()
  val selectedDay by viewModel.selectedDay.collectAsState()
  val isWeekView by viewModel.isWeekView.collectAsState()

  val days = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat"
  )

  val dayNames = mapOf(
    1 to viewModel.t("Monday", "Jumatatu"),
    2 to viewModel.t("Tuesday", "Jumanne"),
    3 to viewModel.t("Wednesday", "Jumatano"),
    4 to viewModel.t("Thursday", "Alhamisi"),
    5 to viewModel.t("Friday", "Ijumaa"),
    6 to viewModel.t("Saturday", "Jumamosi")
  )

  // Always in day order, then by start time, and grouped so every class sits under its own day.
  val filteredSessions = if (isWeekView) sessions else sessions.filter { it.dayOfWeek == selectedDay }
  val groupedSessions = filteredSessions
    .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    .groupBy { it.dayOfWeek }
  // The clock moves on, so "Today" and the date stay true, even after midnight.
  val clockNow = rememberNow(60_000L)
  val todayNumber = DeadlineTime.teachingDay(clockNow)

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Header & Week Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = viewModel.getString("tab_timetable"),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${DeadlineTime.dayLine(clockNow)} • ${viewModel.t("East Africa Time", "Saa za Afrika Mashariki")}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        var showAdd by remember { mutableStateOf(false) }
        if (viewModel.schedulableCourses().isNotEmpty()) {
          IconButton(onClick = { showAdd = true }, modifier = Modifier.testTag("add_session_btn")) {
            Icon(Icons.Default.Add, contentDescription = viewModel.t("Add a class", "Ongeza kipindi"), tint = DeepBlue)
          }
        }
        if (showAdd) AddSessionSheet(viewModel = viewModel, initialDay = selectedDay, onDismiss = { showAdd = false })

        SegmentedSwitch(
          options = listOf(
            SwitchOption(viewModel.t("Day", "Siku"), Icons.Default.ViewAgenda),
            SwitchOption(viewModel.t("Week", "Wiki"), Icons.Default.ViewWeek)
          ),
          selectedIndex = if (isWeekView) 1 else 0,
          onSelect = { viewModel.toggleWeekView() },
          tag = "toggle_week_view_btn"
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Day Strip (only active if not in full week view)
    if (!isWeekView) {
      val today = DeadlineTime.teachingDay(clockNow)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        days.forEach { (dayNum, dayLabel) ->
          val isSelected = selectedDay == dayNum
          val isToday = today == dayNum
          val count = sessions.count { it.dayOfWeek == dayNum }
          val hasChanges = sessions.any { it.dayOfWeek == dayNum && it.status == SessionStatus.CHANGED }

          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 76.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(if (isSelected) DeepBlue else MaterialTheme.colorScheme.surface)
              .border(
                width = if (isToday && !isSelected) 2.dp else 1.dp,
                color = when {
                  isSelected -> DeepBlue
                  isToday -> DeepBlue
                  else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { viewModel.setSelectedDay(dayNum) }
              .padding(vertical = 10.dp)
              .testTag("day_strip_$dayLabel"),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${DeadlineTime.dateOfTeachingDay(dayNum, clockNow)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (count > 0) "$count" else "\u2013",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasChanges) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(if (isSelected) WarmAmberLight else WarmAmber)
                  )
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }

    // Sessions List
    if (filteredSessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "No classes",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No sessions scheduled",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Enjoy your day off or review course materials",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        groupedSessions.forEach { (day, daySessions) ->
          stickyHeader(key = "day-$day") {
            DayHeader(
              name = dayNames[day] ?: "",
              count = daySessions.size,
              isToday = day == todayNumber,
              hasChanges = daySessions.any { it.status == SessionStatus.CHANGED || it.status == SessionStatus.CANCELLED },
              todayLabel = viewModel.t("Today", "Leo"),
              countLabel = viewModel.t(
                if (daySessions.size == 1) "1 class" else "${daySessions.size} classes",
                if (daySessions.size == 1) "Kipindi 1" else "Vipindi ${daySessions.size}"
              )
            )
          }
          items(daySessions, key = { it.id }) { session ->
            Box(modifier = Modifier.animateItem()) {
              SessionCard(
                session = session,
                currentUserRole = user.role,
                is24Hour = is24Hour,
                canModerate = viewModel.canEditOrDelete(session.lecturerId),
                onEditClick = { onOpenChangeVenueDialog(session.id) },
                onCancelClick = { onOpenCancelDialog(session.id) },
                onDeleteClick = if (viewModel.canEditOrDelete(session.lecturerId)) { { viewModel.deleteSession(session.id) } } else null
              )
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

/** A day's banner: stays pinned at the top while that day's classes scroll underneath. */
@Composable
private fun DayHeader(
  name: String,
  count: Int,
  isToday: Boolean,
  hasChanges: Boolean,
  todayLabel: String,
  countLabel: String
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth().testTag("day_header_$name")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(width = 4.dp, height = 22.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(if (isToday) DeepBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isToday) DeepBlue else MaterialTheme.colorScheme.onSurface
      )
      if (isToday) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = todayLabel,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DeepBlue)
            .padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      if (hasChanges) {
        Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(WarmAmber))
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = countLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
