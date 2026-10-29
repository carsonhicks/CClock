package im.carson.cclock.schedule

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.carson.cclock.alarm.AlarmService
import im.carson.cclock.data.ClockStore

/** Handles CClock's own alarms and notification actions. */
class ClockReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val key = intent.getStringExtra(EXTRA_KEY)
    val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
    when (intent.action) {
      ACTION_RING -> if (key != null) {
        ClockStore.finish(context, key, alarmId)
        AlarmService.start(context, intent.getStringExtra(EXTRA_LABEL) ?: "Alarm")
      }
      ACTION_SKIP -> if (key != null) ClockStore.finish(context, key, alarmId)
      ACTION_SNOOZE -> AlarmService.snooze(context)
      ACTION_STOP -> AlarmService.stop(context)
      ACTION_TICK -> Scheduler.reschedule(context)
    }
  }

  companion object {
    const val ACTION_RING = "im.carson.cclock.action.RING"
    const val ACTION_TICK = "im.carson.cclock.action.TICK"
    const val ACTION_SKIP = "im.carson.cclock.action.SKIP"
    const val ACTION_SNOOZE = "im.carson.cclock.action.SNOOZE"
    const val ACTION_STOP = "im.carson.cclock.action.STOP"
    private const val EXTRA_KEY = "key"
    private const val EXTRA_LABEL = "label"
    private const val EXTRA_ALARM_ID = "alarm_id"
    private val ACTIONS = listOf(ACTION_RING, ACTION_TICK, ACTION_SKIP, ACTION_SNOOZE, ACTION_STOP)

    fun pendingIntent(context: Context, action: String, ring: Ring? = null): PendingIntent {
      val intent = Intent(context, ClockReceiver::class.java).setAction(action)
      ring?.let {
        intent.putExtra(EXTRA_KEY, it.key).putExtra(EXTRA_LABEL, it.label).putExtra(EXTRA_ALARM_ID, it.alarmId)
      }
      return PendingIntent.getBroadcast(
        context, ACTIONS.indexOf(action), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}

/** Re-plans after reboots, clock changes, app updates, and permission changes. */
class SystemEventReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Scheduler.reschedule(context)
  }
}
