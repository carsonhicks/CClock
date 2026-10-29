package im.carson.cclock.data

import java.util.UUID
import kotlinx.serialization.Serializable

// Times of day are stored as minutes after midnight; days of week as ISO numbers (1 = Monday).

/** A recurring sleep window. [wakeDays] are the days you wake up on, like the iOS sleep schedule. */
@Serializable
data class SleepSchedule(
  val id: String = UUID.randomUUID().toString(),
  val wakeDays: Set<Int>,
  val bedtime: Int,
  val wake: Int,
  /** Whether sleep ends with an alarm. Off still silences notifications overnight. */
  val alarm: Boolean = true,
)

/**
 * A one-night change to the sleep that ends on [wakeDate] (ISO date). Null fields follow the
 * schedule: times come from the matching [SleepSchedule], [alarm] from its [SleepSchedule.alarm].
 */
@Serializable
data class SleepOverride(val wakeDate: String, val bedtime: Int? = null, val wake: Int? = null, val alarm: Boolean? = null) {
  val isEmpty get() = bedtime == null && alarm == null
}

/** A standalone alarm. Empty [days] means it rings once and then deletes itself. */
@Serializable
data class Alarm(
  val id: String = UUID.randomUUID().toString(),
  val time: Int,
  val days: Set<Int> = emptySet(),
  val label: String = "",
  val enabled: Boolean = true,
)

@Serializable
data class Snooze(val at: Long, val label: String)

// Timer and stopwatch times are epoch milliseconds and durations in milliseconds.

/** A countdown of [duration]. Running while [endsAt] is set, otherwise paused with [remaining] left. */
@Serializable
data class Timer(val duration: Long, val endsAt: Long? = null, val remaining: Long = duration) {
  val isRunning get() = endsAt != null

  fun remainingAt(now: Long) = endsAt?.let { maxOf(0L, it - now) } ?: remaining
}

/** Running while [runningSince] is set. [laps] are the total elapsed times when each lap ended. */
@Serializable
data class Stopwatch(val runningSince: Long? = null, val elapsed: Long = 0, val laps: List<Long> = emptyList()) {
  val isRunning get() = runningSince != null
  val isReset get() = !isRunning && elapsed == 0L

  fun elapsedAt(now: Long) = elapsed + (runningSince?.let { maxOf(0L, now - it) } ?: 0L)
}

@Serializable
data class ClockState(
  val sleepEnabled: Boolean = false,
  val schedules: List<SleepSchedule> = emptyList(),
  val overrides: List<SleepOverride> = emptyList(),
  val alarms: List<Alarm> = emptyList(),
  /** Keys of ring occurrences that already rang or were skipped. */
  val done: Set<String> = emptySet(),
  val snooze: Snooze? = null,
  val timer: Timer? = null,
  /** The last timer length started, offered again next time. */
  val lastTimer: Long = 5 * 60_000L,
  val stopwatch: Stopwatch = Stopwatch(),
)
