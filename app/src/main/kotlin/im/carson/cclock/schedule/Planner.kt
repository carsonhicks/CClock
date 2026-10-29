package im.carson.cclock.schedule

import im.carson.cclock.data.Alarm
import im.carson.cclock.data.ClockState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class SleepPeriod(
  val key: String,
  val wakeDate: LocalDate,
  val start: ZonedDateTime,
  val end: ZonedDateTime,
  /** Whether this night's times differ from the schedule. */
  val isOverride: Boolean,
  /** Whether an alarm rings when this sleep ends. */
  val alarm: Boolean,
)

data class Ring(val key: String, val at: ZonedDateTime, val label: String, val alarmId: String? = null) {
  val isSnooze get() = key == Planner.SNOOZE_KEY
}

/** Pure scheduling rules: works out when sleep starts and ends and what rings next. */
object Planner {
  const val SNOOZE_KEY = "snooze"
  val SNOOZE: Duration = Duration.ofMinutes(10)

  /** How long before an alarm the upcoming notification and the option to skip appear. */
  val UPCOMING: Duration = Duration.ofHours(2)

  /** Rings this recent still count, so a reschedule that races a ring's delivery can't drop it. */
  private val GRACE: Duration = Duration.ofMinutes(1)

  fun sleepMinutes(bedtime: Int, wake: Int) = (wake - bedtime + 1440) % 1440

  fun sleepPeriod(state: ClockState, wakeDate: LocalDate, zone: ZoneId): SleepPeriod? {
    if (!state.sleepEnabled) return null
    val override = state.overrides.find { it.wakeDate == wakeDate.toString() }
    val schedule = state.schedules.find { wakeDate.dayOfWeek.value in it.wakeDays }
    val bedtime = override?.bedtime ?: schedule?.bedtime ?: return null
    val wake = override?.wake ?: schedule?.wake ?: return null
    val startDate = if (bedtime < wake) wakeDate else wakeDate.minusDays(1)
    return SleepPeriod(
      key = "sleep:$wakeDate",
      wakeDate = wakeDate,
      start = startDate.atTime(bedtime / 60, bedtime % 60).atZone(zone),
      end = wakeDate.atTime(wake / 60, wake % 60).atZone(zone),
      isOverride = override?.bedtime != null,
      alarm = override?.alarm ?: schedule?.alarm ?: true,
    )
  }

  /** Sleep periods that haven't ended or been skipped, soonest first. */
  fun sleepPeriods(state: ClockState, now: ZonedDateTime, since: ZonedDateTime = now): List<SleepPeriod> =
    (-1L..8L)
      .mapNotNull { sleepPeriod(state, now.toLocalDate().plusDays(it), now.zone) }
      .filter { it.end.isAfter(since) && it.key !in state.done }

  fun activeSleep(state: ClockState, now: ZonedDateTime) =
    sleepPeriods(state, now).firstOrNull { !it.start.isAfter(now) }

  fun nextSleep(state: ClockState, now: ZonedDateTime) = sleepPeriods(state, now).firstOrNull()

  fun alarmRing(alarm: Alarm, state: ClockState, since: ZonedDateTime): Ring? {
    if (!alarm.enabled) return null
    for (offset in 0L..14L) {
      val date = since.toLocalDate().plusDays(offset)
      if (alarm.days.isNotEmpty() && date.dayOfWeek.value !in alarm.days) continue
      val at = date.atTime(alarm.time / 60, alarm.time % 60).atZone(since.zone)
      val key = "alarm:${alarm.id}:${at.toEpochSecond()}"
      if (at.isAfter(since) && key !in state.done) return Ring(key, at, alarm.label.ifBlank { "Alarm" }, alarm.id)
    }
    return null
  }

  fun nextRing(state: ClockState, now: ZonedDateTime): Ring? {
    val since = now.minus(GRACE)
    val rings = buildList {
      state.snooze?.let {
        val at = Instant.ofEpochMilli(it.at).atZone(now.zone)
        if (at.isAfter(since)) add(Ring(SNOOZE_KEY, at, it.label))
      }
      sleepPeriods(state, now, since).firstOrNull { it.alarm }?.let { add(Ring(it.key, it.end, "Wake up")) }
      state.alarms.mapNotNullTo(this) { alarmRing(it, state, since) }
    }
    return rings.minByOrNull { it.at }
  }

  fun canSkip(ring: Ring, now: ZonedDateTime) = !now.isBefore(ring.at.minus(UPCOMING))

  /** Drops bookkeeping for days that are over. */
  fun prune(state: ClockState, now: ZonedDateTime): ClockState {
    val yesterday = now.toLocalDate().minusDays(1)
    val cutoff = now.minusDays(1)
    return state.copy(
      done = state.done.filterTo(mutableSetOf()) { key ->
        if (key.startsWith("sleep:")) {
          !LocalDate.parse(key.removePrefix("sleep:")).isBefore(yesterday)
        } else {
          (key.substringAfterLast(':').toLongOrNull() ?: 0L) > cutoff.toEpochSecond()
        }
      },
      overrides = state.overrides.filter { !it.isEmpty && !LocalDate.parse(it.wakeDate).isBefore(yesterday) },
      snooze = state.snooze?.takeIf { it.at > cutoff.toInstant().toEpochMilli() },
    )
  }
}
