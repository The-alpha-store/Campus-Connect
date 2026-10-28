@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Role
import com.example.ui.theme.DangerRed
import com.example.viewmodel.CampusViewModel
import java.security.SecureRandom

/**
 * An administrator adds someone directly: they are approved and can sign in at once. The first
 * password is shown only here, so the administrator can pass it on privately; the person should change it.
 *
 * With [adminOfCampus] set, the app manager adds an administrator to that campus instead.
 */
@Composable
fun AddUserSheet(
  viewModel: CampusViewModel,
  onDismiss: () -> Unit,
  adminOfCampus: String? = null,
  campusProgrammes: List<String> = emptyList()
) {
  val t = { en: String, sw: String -> viewModel.t(en, sw) }
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val isAdminMode = adminOfCampus != null

  var role by remember { mutableStateOf(if (isAdminMode) Role.ADMIN else Role.STUDENT) }
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var number by remember { mutableStateOf("") }
  var programme by remember { mutableStateOf<String?>(null) }
  var pickingProgramme by remember { mutableStateOf(false) }
  var password by remember { mutableStateOf(temporaryPassword()) }
  var error by remember { mutableStateOf<String?>(null) }
  var created by remember { mutableStateOf<Pair<String, String>?>(null) }

  if (created != null) {
    val (who, secret) = created!!
    AppBottomSheet(
      onDismissRequest = onDismiss,
      title = t("Added. Share these details privately.", "Imeongezwa. Toa maelezo haya kwa siri."),
      subtitle = t("The password is not shown again. The person should change it after signing in.", "Nenosiri halionyeshwi tena. Mtu abadilishe baada ya kuingia."),
      tag = "add_user_done_sheet",
      footer = {
        SheetButtons(
          dismissLabel = t("Done", "Imekamilika"), onDismiss = onDismiss,
          confirmLabel = t("Copy details", "Nakili maelezo"),
          onConfirm = {
            clipboard.setText(AnnotatedString("Email: $who\nPassword: $secret"))
            Toast.makeText(context, t("Copied", "Imenakiliwa"), Toast.LENGTH_SHORT).show()
          },
          confirmIcon = Icons.Default.ContentCopy, confirmTag = "copy_new_user_details_btn"
        )
      }
    ) {
      Spacer(Modifier.height(8.dp))
      Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(16.dp)
      ) {
        Text(t("Email", "Barua pepe"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(who, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("new_user_email"))
        Spacer(Modifier.height(10.dp))
        Text(t("First password", "Nenosiri la kwanza"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(secret, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("new_user_password"))
      }
      Spacer(Modifier.height(8.dp))
    }
    return
  }

  AppBottomSheet(
    onDismissRequest = onDismiss,
    title = if (isAdminMode) t("Add administrator", "Ongeza msimamizi") else t("Add a person", "Ongeza mtu"),
    subtitle = t("They are approved at once and can sign in with these details.", "Wanaidhinishwa mara moja na wanaweza kuingia kwa maelezo haya."),
    tag = "add_user_sheet",
    footer = {
      SheetButtons(
        dismissLabel = t("Cancel", "Ghairi"), onDismiss = onDismiss,
        confirmLabel = t("Add", "Ongeza"),
        onConfirm = {
          val result = if (isAdminMode) viewModel.addCampusAdmin(adminOfCampus!!, name, email, number, password)
          else viewModel.addUser(role, name, email, number, programme, password)
          if (result == null) created = email.trim().lowercase() to password else error = result
        },
        enabled = name.isNotBlank() && email.isNotBlank() && number.isNotBlank() &&
          (role != Role.STUDENT || !programme.isNullOrBlank()),
        confirmIcon = Icons.Default.PersonAdd, confirmTag = "confirm_add_user_btn"
      )
    }
  ) {
    Spacer(Modifier.height(4.dp))
    if (!isAdminMode) {
      SegmentedSwitch(
        options = listOf(
          SwitchOption(t("Student", "Mwanafunzi"), Icons.Default.School),
          SwitchOption(t("Lecturer", "Mhadhiri"), Icons.Default.Badge)
        ),
        selectedIndex = if (role == Role.STUDENT) 0 else 1,
        onSelect = { role = if (it == 0) Role.STUDENT else Role.LECTURER },
        tag = "add_user_role_switch"
      )
      Spacer(Modifier.height(12.dp))
    }
    OutlinedTextField(
      value = name, onValueChange = { name = it; error = null }, singleLine = true,
      label = { Text(t("Full name", "Jina kamili")) }, shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().testTag("add_user_name")
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
      value = email, onValueChange = { email = it; error = null }, singleLine = true,
      label = { Text(t("Email", "Barua pepe")) }, shape = RoundedCornerShape(14.dp),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      modifier = Modifier.fillMaxWidth().testTag("add_user_email")
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
      value = number, onValueChange = { number = it; error = null }, singleLine = true,
      label = { Text(if (role == Role.STUDENT) t("Student number", "Namba ya mwanafunzi") else t("Staff number", "Namba ya mfanyakazi")) },
      shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("add_user_number")
    )
    if (role == Role.STUDENT) {
      Spacer(Modifier.height(10.dp))
      if (campusProgrammes.isNotEmpty()) {
        PickerField(
          label = t("Programme", "Programu"), value = programme, placeholder = t("Choose a programme", "Chagua programu"),
          onClick = { pickingProgramme = true }, tag = "add_user_programme"
        )
      } else {
        OutlinedTextField(
          value = programme.orEmpty(), onValueChange = { programme = it }, singleLine = true,
          label = { Text(t("Programme", "Programu")) }, shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("add_user_programme")
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = password, onValueChange = { password = it; error = null }, singleLine = true,
        label = { Text(t("First password", "Nenosiri la kwanza")) }, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).testTag("add_user_password")
      )
      SoftActionButton(
        label = t("New", "Mpya"), icon = Icons.Default.Autorenew, onClick = { password = temporaryPassword() },
        tag = "add_user_generate_password"
      )
    }
    error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp).testTag("add_user_error")) }
    Spacer(Modifier.height(8.dp))
    if (pickingProgramme) {
      PickerSheet(
        title = t("Programme", "Programu"), options = campusProgrammes.map { PickerItem(it, it) },
        selectedId = programme, onSelect = { programme = it }, onDismiss = { pickingProgramme = false }
      )
    }
  }
}

/** 12 characters with capitals, small letters, a digit and a symbol, without look-alikes (0/O, 1/l/I). */
private fun temporaryPassword(): String {
  val random = SecureRandom()
  val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
  val lower = "abcdefghijkmnpqrstuvwxyz"
  val digits = "23456789"
  val symbols = "#$%&*?"
  fun pick(set: String) = set[random.nextInt(set.length)]
  val chars = mutableListOf(pick(upper), pick(lower), pick(digits), pick(symbols))
  repeat(8) { chars.add(pick(upper + lower + digits)) }
  chars.shuffle(random)
  return chars.joinToString("")
}
