package com.campusconnect.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Amber = Color(0xFFF59E0B)
val Success = Color(0xFF16A34A)
val Danger = Color(0xFFDC2626)

private val Light = lightColorScheme(
    primary = Color(0xFF1F4FD8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF), onPrimaryContainer = Color(0xFF0B1F66),
    secondary = Color(0xFF5B6478), onSecondary = Color.White,
    tertiary = Amber, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1D6), onTertiaryContainer = Color(0xFF5A3A00),
    error = Danger, onError = Color.White,
    background = Color(0xFFF5F7FB), onBackground = Color(0xFF14171F),
    surface = Color.White, onSurface = Color(0xFF14171F),
    surfaceVariant = Color(0xFFE9EDF7), onSurfaceVariant = Color(0xFF4A5164),
    outline = Color(0xFF8A92A6), outlineVariant = Color(0xFFE1E5EF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8F9FD),
    surfaceContainer = Color(0xFFF1F4FA), surfaceContainerHigh = Color(0xFFEBEFF8),
    surfaceContainerHighest = Color(0xFFE4E9F4),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FA8FF), onPrimary = Color(0xFF0B1F66),
    primaryContainer = Color(0xFF1B2F7A), onPrimaryContainer = Color(0xFFDDE5FF),
    secondary = Color(0xFFB4BCD0), onSecondary = Color(0xFF1B2030),
    tertiary = Color(0xFFFFB84D), onTertiary = Color(0xFF3A2500),
    tertiaryContainer = Color(0xFF4A3300), onTertiaryContainer = Color(0xFFFFE2A8),
    error = Color(0xFFFF8A80), onError = Color(0xFF3B0A06),
    background = Color(0xFF0D0F14), onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF14171F), onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF1E2230), onSurfaceVariant = Color(0xFFB4BCD0),
    outline = Color(0xFF6F788E), outlineVariant = Color(0xFF2B3040),
    surfaceContainerLowest = Color(0xFF0A0C10), surfaceContainerLow = Color(0xFF14171F),
    surfaceContainer = Color(0xFF181C26), surfaceContainerHigh = Color(0xFF1E2230),
    surfaceContainerHighest = Color(0xFF262B3B),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CampusTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
}
