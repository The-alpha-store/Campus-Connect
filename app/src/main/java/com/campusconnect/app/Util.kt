package com.campusconnect.app

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** All dates are stored in UTC and shown in East Africa Time. */
val EAT: ZoneId = ZoneId.of("Africa/Dar_es_Salaam")

// Formatters follow the app language (English or Kiswahili month and day names).
val TimeFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("HH:mm", Lang.locale)
val DateFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("dd MMM yyyy", Lang.locale)
val DayFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEE d MMM", Lang.locale)
val LongDayFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEEE, d MMMM", Lang.locale)

fun String.toInstantOrNull(): Instant? = try {
    OffsetDateTime.parse(this).toInstant()
} catch (e: Exception) {
    try { Instant.parse(this) } catch (e2: Exception) { null }
}

fun Instant.eat(): ZonedDateTime = atZone(EAT)
fun String.timeEat(): String = toInstantOrNull()?.eat()?.format(TimeFmt).orEmpty()
fun String.dateEat(): String = toInstantOrNull()?.eat()?.format(DateFmt).orEmpty()
fun String.localDateEat(): LocalDate? = toInstantOrNull()?.eat()?.toLocalDate()

fun String.relativeTime(): String {
    val t = toInstantOrNull() ?: return ""
    val m = Duration.between(t, Instant.now()).toMinutes()
    return when {
        m < 1 -> tr("Just now")
        m < 60 -> trf("%d min ago", m)
        m < 1440 -> trf("%d h ago", m / 60)
        m < 10080 -> trf("%d d ago", m / 1440)
        else -> dateEat()
    }
}

fun friendlyError(e: Throwable): String {
    val m = e.message.orEmpty()
    return when {
        m.contains("class_sessions_no_venue_overlap") -> tr("That venue is already booked for this time.")
        m.contains("Invalid login credentials", true) -> tr("Wrong email or password.")
        m.contains("already registered", true) || m.contains("already been registered", true) ->
            tr("That email is already registered.")
        m.contains("Email not confirmed", true) -> tr("Please confirm your email first, then sign in.")
        m.contains("duplicate key", true) -> tr("That already exists.")
        m.contains("row-level security", true) || m.contains("permission denied", true) ->
            tr("You are not allowed to do that.")
        m.contains("mime type", true) || m.contains("not supported", true) -> tr("That file type is not allowed.")
        e is java.io.IOException || m.contains("resolve host", true) || m.contains("timeout", true) ||
            e.javaClass.simpleName.contains("Timeout") -> tr("No internet connection.")
        m.isNotBlank() -> m.lines().first().take(160)
        else -> tr("Something went wrong. Try again.")
    }
}
