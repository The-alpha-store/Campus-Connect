package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CampusMaintenance
import com.example.ui.components.SoftActionButton
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.WarmAmber
import com.example.util.DeadlineTime

/**
 * Shown instead of the app while the person's campus is paused for maintenance. Nothing else in the app
 * works for them (the database refuses it too); they can see why, check again, or sign out.
 */
@Composable
fun MaintenanceScreen(
  maintenance: CampusMaintenance,
  onCheckAgain: () -> Unit,
  onSignOut: () -> Unit,
  t: (String, String) -> String
) {
  Surface(modifier = Modifier.fillMaxSize().testTag("maintenance_screen"), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier.size(96.dp).clip(CircleShape).background(WarmAmber.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Build, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(44.dp))
      }
      Spacer(Modifier.height(22.dp))
      Text(
        t("Under maintenance", "Matengenezo yanaendelea"),
        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(6.dp))
      Text(maintenance.campusName, style = MaterialTheme.typography.titleMedium, color = DeepBlue, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
      Spacer(Modifier.height(14.dp))
      Text(
        maintenance.note.ifBlank {
          t("CampusConnect is being updated for your campus. Everything will be back soon.", "CampusConnect inasasishwa kwa chuo chako. Kila kitu kitarudi hivi karibuni.")
        },
        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (maintenance.since.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
          t("Paused ${DeadlineTime.friendly(maintenance.since).lowercase()}", "Imesimamishwa ${DeadlineTime.friendly(maintenance.since, swahili = true).lowercase()}"),
          style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(Modifier.height(28.dp))
      SoftActionButton(
        label = t("Check again", "Angalia tena"), icon = Icons.Default.Refresh, onClick = onCheckAgain,
        filled = true, modifier = Modifier.fillMaxWidth(), tag = "maintenance_check_again_btn"
      )
      Spacer(Modifier.height(10.dp))
      SoftActionButton(
        label = t("Sign out", "Toka"), icon = Icons.Default.Logout, onClick = onSignOut,
        modifier = Modifier.fillMaxWidth(), tag = "maintenance_sign_out_btn"
      )
      Spacer(Modifier.height(18.dp))
      Text(
        t("Your work is safe. Nothing is lost while the campus is paused.", "Kazi yako ni salama. Hakuna kinachopotea wakati chuo kimesimamishwa."),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
      )
    }
  }
}
