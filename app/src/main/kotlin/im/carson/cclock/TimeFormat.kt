package im.carson.cclock

import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Hour and minute in the user's locale, following the system 12/24-hour setting. */
fun timeFormatter(context: Context): DateTimeFormatter {
  val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
  return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton))
}

fun DateTimeFormatter.formatMinutes(minutes: Int): String = format(LocalTime.of(minutes / 60, minutes % 60))

fun formatDuration(minutes: Int): String {
  val h = minutes / 60
  val m = minutes % 60
  return when {
    m == 0 -> "${h}h"
    h == 0 -> "${m}m"
    else -> "${h}h ${m}m"
  }
}

/** A timer or stopwatch reading: "1:02:03" with hours, "2:03" without. */
fun formatElapsed(millis: Long): String {
  val seconds = millis / 1000
  val h = seconds / 3600
  val m = seconds / 60 % 60
  val s = seconds % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Time left on a timer, rounded up so it reads 0:00 only when it's done. */
fun formatRemaining(millis: Long) = formatElapsed((millis + 999) / 1000 * 1000)

/** Hundredths of a second, for the stopwatch: ".07". */
fun formatHundredths(millis: Long) = ".%02d".format(millis / 10 % 100)

/** The week starting on the locale's first day. */
fun orderedWeek(): List<DayOfWeek> {
  val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
  return (0L until 7L).map { first.plus(it) }
}

fun formatDays(days: Set<Int>): String = when {
  days.isEmpty() -> "Once"
  days.size == 7 -> "Every day"
  days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
  days == setOf(6, 7) -> "Weekends"
  else -> orderedWeek().filter { it.value in days }.joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
