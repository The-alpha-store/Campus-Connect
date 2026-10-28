package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBlue
import kotlinx.coroutines.delay

/**
 * The first thing a person sees: the logo settles in, the name and promise fade up, three dots pulse,
 * then [onFinished] hands over to sign-in or the app. It is short on purpose (about a second and a half)
 * and always in the brand colours, whatever the phone's light or dark setting.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, t: (String, String) -> String) {
  val logoScale = remember { Animatable(0.7f) }
  val logoAlpha = remember { Animatable(0f) }
  val textAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    logoAlpha.animateTo(1f, tween(450, easing = LinearEasing))
  }
  LaunchedEffect(Unit) {
    logoScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
  }
  LaunchedEffect(Unit) {
    delay(350)
    textAlpha.animateTo(1f, tween(500))
    delay(750)
    onFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(Color(0xFF1E40AF), DeepBlue, Color(0xFF172554))))
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .scale(logoScale.value)
          .alpha(logoAlpha.value)
          .size(112.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.School, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(50.dp))
        }
      }
      Spacer(Modifier.height(26.dp))
      Text(
        "CampusConnect",
        color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.alpha(textAlpha.value)
      )
      Spacer(Modifier.height(8.dp))
      Text(
        t("Your campus, in one place", "Chuo chako, mahali pamoja"),
        color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        modifier = Modifier.alpha(textAlpha.value).padding(horizontal = 32.dp)
      )
    }
    PulsingDots(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).alpha(textAlpha.value))
  }
}

@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "splash-dots")
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    repeat(3) { index ->
      val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 160, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot-$index"
      )
      Box(Modifier.size(9.dp).alpha(alpha).clip(CircleShape).background(Color.White))
    }
  }
}
