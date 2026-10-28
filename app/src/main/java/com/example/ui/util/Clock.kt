package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * The current time in milliseconds, refreshed every [intervalMs] so anything computed from it
 * ("due in 3 hours", "Today", the date in a header) stays true while the screen is open.
 */
@Composable
fun rememberNow(intervalMs: Long = 30_000L): Long {
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(intervalMs) {
    while (true) {
      delay(intervalMs)
      now = System.currentTimeMillis()
    }
  }
  return now
}
