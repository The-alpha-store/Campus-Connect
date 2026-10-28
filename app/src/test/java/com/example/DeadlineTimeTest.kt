package com.example

import com.example.util.DeadlineTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** All times are East Africa Time (UTC+3), whatever zone the phone or the test machine is in. */
class DeadlineTimeTest {

  // 26 Oct 2026, 11:30 in East Africa Time
  private val now: Long = Calendar.getInstance(DeadlineTime.zone).apply {
    clear(); set(2026, Calendar.OCTOBER, 26, 11, 30, 0)
  }.timeInMillis

  private fun minutesAgo(m: Long) = DeadlineTime.stamp(now - m * 60_000)

  @Test
  fun `a stamp is written in east africa time and reads back to the same minute`() {
    assertEquals("26 Oct 2026, 11:30 EAT", DeadlineTime.stamp(now))
    assertEquals(now, DeadlineTime.parseStamp("26 Oct 2026, 11:30 EAT"))
  }

  @Test
  fun `friendly words are always true for the moment they describe`() {
    assertEquals("Just now", DeadlineTime.friendly(minutesAgo(0), now = now))
    assertEquals("20 min ago", DeadlineTime.friendly(minutesAgo(20), now = now))
    assertEquals("Today, 08:30", DeadlineTime.friendly(minutesAgo(180), now = now))
    assertEquals("Yesterday, 11:30", DeadlineTime.friendly(minutesAgo(24 * 60), now = now))
    assertEquals("19 Oct, 11:30", DeadlineTime.friendly(minutesAgo(7 * 24 * 60), now = now))
    assertEquals("26 Oct 2025, 11:30", DeadlineTime.friendly(DeadlineTime.stamp(now - 365L * 24 * 3_600_000L), now = now))
  }

  @Test
  fun `the same stamp reads differently as time passes`() {
    val stamp = minutesAgo(5)
    assertEquals("5 min ago", DeadlineTime.friendly(stamp, now = now))
    assertEquals("Today, 11:25", DeadlineTime.friendly(stamp, now = now + 3 * 3_600_000L))
    assertEquals("Yesterday, 11:25", DeadlineTime.friendly(stamp, now = now + 24 * 3_600_000L))
  }

  @Test
  fun `midnight in east africa time is the boundary between today and yesterday`() {
    val justAfterMidnight = Calendar.getInstance(DeadlineTime.zone).apply { clear(); set(2026, Calendar.OCTOBER, 27, 0, 10, 0) }.timeInMillis
    val lateEvening = DeadlineTime.stamp(Calendar.getInstance(DeadlineTime.zone).apply { clear(); set(2026, Calendar.OCTOBER, 26, 23, 50, 0) }.timeInMillis)
    assertEquals("Yesterday, 23:50", DeadlineTime.friendly(lateEvening, now = justAfterMidnight + 30 * 60_000L))
  }

  @Test
  fun `date only text and unknown text are handled`() {
    assertEquals("Today", DeadlineTime.friendly("26 Oct 2026", now = now))
    assertEquals("Yesterday", DeadlineTime.friendly("25 Oct 2026", now = now))
    assertEquals("Just now", DeadlineTime.friendly("Just now", now = now)) // old text that is not a stamp is left alone
    assertEquals("Last term", DeadlineTime.friendly("Last term", now = now))
  }

  @Test
  fun `deadline text is read with or without a weekday and urgency follows the clock`() {
    val due = DeadlineTime.parse("Friday, 30 Oct 2026 \u2022 17:00 EAT")!!
    assertEquals(DeadlineTime.parse("30 Oct 2026 \u2022 17:00 EAT"), due)
    assertEquals(DeadlineTime.parse("Tomorrow, 30 Oct 2026 \u2022 17:00 EAT"), due)
    assertEquals("Due in 4 days", DeadlineTime.urgency("Friday, 30 Oct 2026 \u2022 17:00 EAT", "x", now = now))
    assertEquals("Due in 5 hours", DeadlineTime.urgency("26 Oct 2026 \u2022 17:00 EAT", "x", now = now))
    assertEquals("Overdue", DeadlineTime.urgency("26 Oct 2026 \u2022 09:00 EAT", "x", now = now))
    assertEquals("fallback", DeadlineTime.urgency("someday", "fallback", now = now))
    assertNull(DeadlineTime.parse("someday"))
  }

  @Test
  fun `times relative to today are built on the east africa calendar`() {
    assertEquals("Tuesday, 27 Oct 2026 \u2022 23:59 EAT", DeadlineTime.format(DeadlineTime.at(1, 23, 59, now)))
    assertEquals("Monday, 26 Oct 2026 \u2022 08:00 EAT", DeadlineTime.format(DeadlineTime.at(0, 8, 0, now)))
  }

  @Test
  fun `the teaching day is monday to saturday and sunday counts as monday`() {
    fun day(y: Int, m: Int, d: Int) = DeadlineTime.teachingDay(Calendar.getInstance(DeadlineTime.zone).apply { clear(); set(y, m, d, 12, 0) }.timeInMillis)
    assertEquals(1, day(2026, Calendar.OCTOBER, 26)) // Monday
    assertEquals(3, day(2026, Calendar.OCTOBER, 28)) // Wednesday
    assertEquals(6, day(2026, Calendar.OCTOBER, 31)) // Saturday
    assertEquals(1, day(2026, Calendar.NOVEMBER, 1)) // Sunday shows Monday
  }

  @Test
  fun `the day key and header follow east africa time not the phone zone`() {
    // 21:30 UTC on 26 Oct is already 00:30 on 27 Oct in East Africa.
    val utcLate = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, Calendar.OCTOBER, 26, 21, 30, 0) }.timeInMillis
    assertEquals("2026-10-27", DeadlineTime.dayKey(utcLate))
    assertEquals("Tue, 27 Oct 2026", DeadlineTime.dayLine(utcLate))
    assertTrue(DeadlineTime.stamp(utcLate).endsWith("00:30 EAT"))
  }
}
