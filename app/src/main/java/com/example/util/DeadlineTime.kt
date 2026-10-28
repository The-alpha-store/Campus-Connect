package com.example.util


import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Deadlines are shown as text such as "Friday, 24 Oct 2026 • 17:00 EAT". This turns that text into a
 * real moment and back, so the app can tell how long is left instead of showing a fixed word.
 */
object DeadlineTime {
  val zone: TimeZone = TimeZone.getTimeZone("Africa/Dar_es_Salaam")

  private fun shown() = SimpleDateFormat("EEEE, d MMM yyyy '\u2022' HH:mm", Locale.ENGLISH).apply { timeZone = zone }
  private fun core() = SimpleDateFormat("d MMM yyyy '\u2022' HH:mm", Locale.ENGLISH).apply {
    timeZone = zone
    isLenient = false
  }

  fun format(millis: Long): String = shown().format(Date(millis)) + " EAT"

  /** The moment a deadline text means, or null when the text is not in the app's format. */
  fun parse(text: String): Long? {
    // Drop the zone and any leading weekday, "Today" or "Tomorrow": the date itself is what counts.
    val body = text.replace("EAT", "").trim().let { t ->
      if (t.contains(", ")) t.substringAfter(", ") else t
    }.trim()
    return runCatching { core().parse(body)?.time }.getOrNull()
  }

  /** Tomorrow at 23:59: a sensible starting point for a new deadline. */
  fun tomorrowEvening(now: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance(zone).apply {
      timeInMillis = now
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  /** How long is left, in words. Falls back to [fallback] when the text can't be read. */
  fun urgency(dueText: String, fallback: String, swahili: Boolean = false, now: Long = System.currentTimeMillis()): String {
    val due = parse(dueText) ?: return fallback
    val diff = due - now
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
      diff < 0 -> if (swahili) "Muda umepita" else "Overdue"
      minutes < 60 -> if (swahili) "Dakika ${minutes.coerceAtLeast(1)} zimebaki" else "Due in ${minutes.coerceAtLeast(1)} min"
      hours < 24 -> if (swahili) "Saa $hours zimebaki" else "Due in $hours hours"
      days < 7 -> if (swahili) "Siku ${days.coerceAtLeast(1)} zimebaki" else "Due in ${days.coerceAtLeast(1)} days"
      days < 14 -> if (swahili) "Wiki ijayo" else "Next week"
      else -> if (swahili) "Inakuja" else "Upcoming"
    }
  }

  // ---------- Timestamps: "when did this happen" ----------

  private fun stampFormat() = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH).apply { timeZone = zone; isLenient = false }
  private fun dateOnlyFormat() = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).apply { timeZone = zone; isLenient = false }

  /** The moment as the app stores it: "26 Oct 2026, 11:30 EAT". Always East Africa Time, whatever the phone's zone. */
  fun stamp(millis: Long = System.currentTimeMillis()): String = stampFormat().format(Date(millis)) + " EAT"

  /** A moment [daysFromNow] days from now at the given East Africa clock time. Used for dates relative to today. */
  fun at(daysFromNow: Int, hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance(zone).apply {
      timeInMillis = now
      add(Calendar.DAY_OF_YEAR, daysFromNow)
      set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

  /** Reads a stamp back. Understands "26 Oct 2026, 11:30 EAT" and date-only "26 Oct 2026". Null for anything else. */
  fun parseStamp(text: String): Long? {
    val body = text.replace("EAT", "").trim()
    return runCatching { stampFormat().parse(body)?.time }.getOrNull()
      ?: runCatching { dateOnlyFormat().parse(body)?.time }.getOrNull()
  }

  private fun epochDay(millis: Long): Long = (millis + zone.getOffset(millis)) / 86_400_000L

  /**
   * A stamp in friendly, always-true words: "Just now", "12 min ago", "Today, 10:45", "Yesterday, 14:20",
   * "18 Oct, 09:15" or, from another year, "18 Oct 2025". Text that is not a stamp is returned unchanged.
   */
  fun friendly(text: String, swahili: Boolean = false, now: Long = System.currentTimeMillis()): String {
    val millis = parseStamp(text) ?: return text
    val dateOnly = !text.contains(':')
    val clock = SimpleDateFormat("HH:mm", Locale.ENGLISH).apply { timeZone = zone }.format(Date(millis))
    val dayDiff = epochDay(now) - epochDay(millis)
    val minutes = (now - millis) / 60_000
    return when {
      !dateOnly && minutes in 0..0 -> if (swahili) "Sasa hivi" else "Just now"
      !dateOnly && minutes in 1..59 -> if (swahili) "Dakika $minutes zilizopita" else "$minutes min ago"
      dayDiff == 0L -> if (dateOnly) (if (swahili) "Leo" else "Today") else (if (swahili) "Leo, $clock" else "Today, $clock")
      dayDiff == 1L -> if (dateOnly) (if (swahili) "Jana" else "Yesterday") else (if (swahili) "Jana, $clock" else "Yesterday, $clock")
      else -> {
        val sameYear = Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.YEAR) ==
          Calendar.getInstance(zone).apply { timeInMillis = now }.get(Calendar.YEAR)
        val day = SimpleDateFormat(if (sameYear) "d MMM" else "d MMM yyyy", Locale.ENGLISH).apply { timeZone = zone }.format(Date(millis))
        if (dateOnly) day else "$day, $clock"
      }
    }
  }

  /** Today's date for a header: "Mon, 26 Oct 2026". */
  fun dayLine(now: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).apply { timeZone = zone }.format(Date(now))

  /** Monday = 1 ... Saturday = 6 in East Africa Time; Sunday counts as Monday, the next teaching day. */
  fun teachingDay(now: Long = System.currentTimeMillis()): Int {
    val dow = Calendar.getInstance(zone).apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 1 else dow - 1
  }

  /** Day of the month for a teaching day (1 = Monday ... 6 = Saturday) of the week being shown. On Sunday that is next week. */
  fun dateOfTeachingDay(day: Int, now: Long = System.currentTimeMillis()): Int {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    cal.add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) 1 else -(dow - Calendar.MONDAY))
    cal.add(Calendar.DAY_OF_YEAR, day - 1)
    return cal.get(Calendar.DAY_OF_MONTH)
  }

  /** The EAT calendar day as "2026-10-26", for counters that reset at midnight. */
  fun dayKey(now: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(now))

  fun isOverdue(dueText: String, now: Long = System.currentTimeMillis()): Boolean =
    parse(dueText)?.let { it < now } ?: false
}
