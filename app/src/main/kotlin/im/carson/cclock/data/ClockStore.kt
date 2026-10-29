package im.carson.cclock.data

import android.content.Context
import im.carson.cclock.schedule.Planner
import im.carson.cclock.schedule.Scheduler
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Holds all user data in a single JSON file. Every edit re-plans alarms and sleep mode. */
object ClockStore {
  private val json = Json { ignoreUnknownKeys = true }
  private lateinit var file: File
  private val _state = MutableStateFlow(ClockState())
  val state: StateFlow<ClockState> = _state.asStateFlow()

  fun load(context: Context) {
    file = File(context.filesDir, "clock.json")
    _state.value = runCatching { json.decodeFromString<ClockState>(file.readText()) }.getOrDefault(ClockState())
  }

  fun edit(context: Context, transform: (ClockState) -> ClockState) {
    val next = Planner.prune(transform(_state.value), ZonedDateTime.now())
    _state.value = next
    val tmp = File(file.path + ".tmp")
    tmp.writeText(json.encodeToString(ClockState.serializer(), next))
    tmp.renameTo(file)
    Scheduler.reschedule(context)
  }

  /** Saves [schedule], taking its days away from any other schedule (each day has one schedule). */
  fun saveSchedule(context: Context, schedule: SleepSchedule) = edit(context) { state ->
    val others = state.schedules
      .filter { it.id != schedule.id }
      .map { it.copy(wakeDays = it.wakeDays - schedule.wakeDays) }
      .filter { it.wakeDays.isNotEmpty() }
    state.copy(schedules = others + schedule)
  }

  fun deleteSchedule(context: Context, id: String) = edit(context) { state ->
    state.copy(schedules = state.schedules.filter { it.id != id })
  }

  /** Changes the times for the night ending on [wakeDate]; null times go back to the schedule. */
  fun setNightTimes(context: Context, wakeDate: String, bedtime: Int?, wake: Int?) =
    editNight(context, wakeDate) { it.copy(bedtime = bedtime, wake = wake) }

  /** Turns the wake-up alarm on or off for the night ending on [wakeDate] only. */
  fun setNightAlarm(context: Context, wakeDate: String, alarm: Boolean) = editNight(context, wakeDate) { night ->
    val day = LocalDate.parse(wakeDate).dayOfWeek.value
    val scheduled = state.value.schedules.find { day in it.wakeDays }?.alarm ?: true
    night.copy(alarm = alarm.takeIf { it != scheduled })
  }

  /** Turns the wake-up alarm on or off for a whole schedule, dropping its one-night alarm changes. */
  fun setScheduleAlarm(context: Context, schedule: SleepSchedule, alarm: Boolean) = edit(context) { state ->
    state.copy(
      schedules = state.schedules.map { if (it.id == schedule.id) it.copy(alarm = alarm) else it },
      overrides = state.overrides.map {
        if (LocalDate.parse(it.wakeDate).dayOfWeek.value in schedule.wakeDays) it.copy(alarm = null) else it
      },
    )
  }

  private fun editNight(context: Context, wakeDate: String, change: (SleepOverride) -> SleepOverride) = edit(context) { state ->
    val current = state.overrides.find { it.wakeDate == wakeDate } ?: SleepOverride(wakeDate)
    state.copy(overrides = state.overrides.filter { it.wakeDate != wakeDate } + change(current))
  }

  fun saveAlarm(context: Context, alarm: Alarm) = edit(context) { state ->
    state.copy(alarms = state.alarms.filter { it.id != alarm.id } + alarm)
  }

  fun deleteAlarm(context: Context, id: String) = edit(context) { state ->
    state.copy(alarms = state.alarms.filter { it.id != id })
  }

  fun snooze(context: Context, label: String) = edit(context) { state ->
    state.copy(snooze = Snooze(System.currentTimeMillis() + Planner.SNOOZE.toMillis(), label))
  }

  /** Marks a ring as handled, whether it rang or was skipped, so it won't ring again. One-time alarms are deleted. */
  fun finish(context: Context, key: String, alarmId: String?) = edit(context) { state ->
    if (key == Planner.SNOOZE_KEY) {
      state.copy(snooze = null)
    } else {
      state.copy(
        done = state.done + key,
        alarms = state.alarms.filter { it.id != alarmId || it.days.isNotEmpty() },
      )
    }
  }

  fun startTimer(context: Context, duration: Long) = edit(context) { state ->
    state.copy(timer = Timer(duration, endsAt = System.currentTimeMillis() + duration), lastTimer = duration)
  }

  fun pauseTimer(context: Context) = editTimer(context) { it.copy(endsAt = null, remaining = it.remainingAt(System.currentTimeMillis())) }

  fun resumeTimer(context: Context) = editTimer(context) { it.copy(endsAt = System.currentTimeMillis() + it.remaining) }

  fun addToTimer(context: Context, millis: Long) = editTimer(context) {
    it.copy(duration = it.duration + millis, endsAt = it.endsAt?.plus(millis), remaining = it.remaining + millis)
  }

  /** Clears the timer, whether cancelled or done ringing. */
  fun clearTimer(context: Context) = edit(context) { it.copy(timer = null) }

  private fun editTimer(context: Context, change: (Timer) -> Timer) = edit(context) { state ->
    state.copy(timer = state.timer?.let(change))
  }

  fun startStopwatch(context: Context) = editStopwatch(context) { it.copy(runningSince = System.currentTimeMillis()) }

  fun pauseStopwatch(context: Context) = editStopwatch(context) {
    Stopwatch(elapsed = it.elapsedAt(System.currentTimeMillis()), laps = it.laps)
  }

  fun lapStopwatch(context: Context) = editStopwatch(context) { it.copy(laps = it.laps + it.elapsedAt(System.currentTimeMillis())) }

  fun resetStopwatch(context: Context) = editStopwatch(context) { Stopwatch() }

  private fun editStopwatch(context: Context, change: (Stopwatch) -> Stopwatch) = edit(context) { state ->
    state.copy(stopwatch = change(state.stopwatch))
  }
}
