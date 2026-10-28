package com.example.ui.components

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBlue

/**
 * A comfortable action button for headers and cards: at least 44 dp tall (a proper touch target),
 * an icon, a full-size label that never clips, and a soft background in the action's colour.
 * Use [modifier] with `weight(1f)` to share a row equally.
 */
@Composable
fun SoftActionButton(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  color: Color = DeepBlue,
  filled: Boolean = false,
  tag: String? = null
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (filled) color else color.copy(alpha = 0.12f),
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .semantics { role = Role.Button }
      .then(if (tag != null) Modifier.testTag(tag) else Modifier)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = if (filled) Color.White else color, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (filled) Color.White else color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/** One option of a [SegmentedSwitch]. */
data class SwitchOption(val label: String, val icon: ImageVector)

/**
 * A two-or-more option switch such as Day | Week. The chosen option is filled and every option is
 * a full-height tap target. [onSelect] is called with the index of the option that was tapped.
 */
@Composable
fun SegmentedSwitch(
  options: List<SwitchOption>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  tag: String? = null
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    modifier = modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
  ) {
    Row(modifier = Modifier.padding(4.dp)) {
      options.forEachIndexed { index, option ->
        val selected = index == selectedIndex
        Box(
          modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) DeepBlue else Color.Transparent)
            .clickable(enabled = !selected) { onSelect(index) }
            .semantics { role = Role.Tab }
            .padding(horizontal = 14.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              option.icon, contentDescription = null, modifier = Modifier.size(18.dp),
              tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
              option.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

/** A centred empty state: icon, headline, a line of help and an optional action. */
@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.size(10.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.size(4.dp))
    Text(
      body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
    if (action != null) {
      Spacer(Modifier.size(14.dp))
      action()
    }
  }
}
