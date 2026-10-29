package im.carson.cclock.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import im.carson.cclock.MainActivity
import im.carson.cclock.data.ClockStore
import java.time.ZonedDateTime

/** Turns the plan into AlarmManager alarms, sleep mode state, and CClock's ongoing notifications. */
object Scheduler {
  private const val TAG = "Scheduler"

  fun reschedule(context: Context) {
    val state = ClockStore.state.value
    val now = ZonedDateTime.now()
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    // The next ring goes through setAlarmClock so it shows as the system's next alarm.
    val ring = Planner.nextRing(state, now)
    val ringIntent = ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_RING, ring)
    if (ring == null) {
      alarmManager.cancel(ringIntent)
    } else {
      val info = AlarmManager.AlarmClockInfo(ring.at.toInstant().toEpochMilli(), openApp(context))
      tryExact { alarmManager.setAlarmClock(info, ringIntent) }
    }

    SleepMode.update(context, enabled = state.sleepEnabled, active = Planner.activeSleep(state, now) != null)

    if (ring != null && Planner.canSkip(ring, now)) {
      Notifications.showUpcoming(context, ring)
    } else {
      Notifications.cancelUpcoming(context)
    }

    // Timers aren't alarms, so they skip setAlarmClock and stay out of the system's next-alarm display.
    val timerIntent = ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_TIMER_DONE)
    val timerEnd = state.timer?.endsAt
    if (timerEnd == null) {
      alarmManager.cancel(timerIntent)
    } else {
      tryExact { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timerEnd, timerIntent) }
    }
    Notifications.updateTimer(context, state.timer)
    Notifications.updateStopwatch(context, state.stopwatch)

    // Wake up again when sleep starts or ends (a night without an alarm has no ring to end it),
    // or when the upcoming notification is due.
    val wakeups = Planner.sleepPeriods(state, now).flatMap { listOf(it.start, it.end) } +
      listOfNotNull(ring?.at?.minus(Planner.UPCOMING))
    val next = wakeups.filter { it.isAfter(now) }.minOrNull()
    val tickIntent = ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_TICK)
    if (next == null) {
      alarmManager.cancel(tickIntent)
    } else {
      tryExact { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), tickIntent) }
    }
  }

  private fun openApp(context: Context) = PendingIntent.getActivity(
    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
  )

  private inline fun tryExact(block: () -> Unit) {
    try {
      block()
    } catch (e: SecurityException) {
      Log.w(TAG, "Exact alarm permission missing", e)
    }
  }
}
