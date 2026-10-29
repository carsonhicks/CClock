package im.carson.cclock.schedule

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import im.carson.cclock.MainActivity
import im.carson.cclock.R
import im.carson.cclock.alarm.AlarmActivity
import im.carson.cclock.alarm.Ringing
import im.carson.cclock.data.Stopwatch
import im.carson.cclock.data.Timer
import im.carson.cclock.formatElapsed
import im.carson.cclock.formatRemaining
import im.carson.cclock.timeFormatter

object Notifications {
  private const val CHANNEL_RINGING = "ringing"
  private const val CHANNEL_UPCOMING = "upcoming"
  private const val CHANNEL_RUNNING = "running"
  const val ID_RINGING = 1
  private const val ID_UPCOMING = 2
  private const val ID_TIMER = 3
  private const val ID_STOPWATCH = 4

  fun createChannels(context: Context) {
    val ringing = NotificationChannel(CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
      // AlarmService plays the sound and vibration itself.
      setSound(null, null)
      enableVibration(false)
      setBypassDnd(true)
    }
    val upcoming = NotificationChannel(CHANNEL_UPCOMING, "Upcoming alarms", NotificationManager.IMPORTANCE_LOW)
    val running = NotificationChannel(CHANNEL_RUNNING, "Timer and stopwatch", NotificationManager.IMPORTANCE_LOW)
    context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(ringing, upcoming, running))
  }

  fun ringing(context: Context, ringing: Ringing): Notification {
    val fullScreen = PendingIntent.getActivity(
      context, 0,
      Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val builder = NotificationCompat.Builder(context, CHANNEL_RINGING)
      .setSmallIcon(R.drawable.ic_alarm)
      .setContentTitle(ringing.label)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setFullScreenIntent(fullScreen, true)
      .setContentIntent(fullScreen)
    if (ringing.canSnooze) {
      builder.addAction(0, "Snooze ${Planner.SNOOZE.toMinutes()} min", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_SNOOZE))
    }
    return builder.addAction(0, "Stop", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_STOP)).build()
  }

  fun showUpcoming(context: Context, ring: Ring) {
    if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val time = timeFormatter(context).format(ring.at)
    val openApp = PendingIntent.getActivity(
      context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_UPCOMING)
      .setSmallIcon(R.drawable.ic_alarm)
      .setContentTitle(if (ring.isSnooze) "Snoozed until $time" else "Upcoming alarm at $time")
      .setContentText(ring.label)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setContentIntent(openApp)
      .addAction(0, if (ring.isSnooze) "Dismiss" else "Skip", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_SKIP, ring))
      .build()
    context.getSystemService(NotificationManager::class.java).notify(ID_UPCOMING, notification)
  }

  fun cancelUpcoming(context: Context) {
    context.getSystemService(NotificationManager::class.java).cancel(ID_UPCOMING)
  }

  /** Shows the timer while it's set, counting down live when running. */
  fun updateTimer(context: Context, timer: Timer?) {
    if (timer == null) return cancel(context, ID_TIMER)
    val endsAt = timer.endsAt
    val builder = running(context, MainActivity.PAGE_TIMER)
      .setContentTitle("Timer · ${formatElapsed(timer.duration)}")
    if (endsAt != null) {
      builder.setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true).setWhen(endsAt)
        .addAction(0, "Pause", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_TIMER_PAUSE))
    } else {
      builder.setContentText("Paused · ${formatRemaining(timer.remaining)} left")
        .addAction(0, "Resume", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_TIMER_RESUME))
    }
    builder.addAction(0, "Cancel", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_TIMER_CANCEL))
    notify(context, ID_TIMER, builder.build())
  }

  /** Shows the stopwatch while it's started, counting up live when running. */
  fun updateStopwatch(context: Context, stopwatch: Stopwatch) {
    if (stopwatch.isReset) return cancel(context, ID_STOPWATCH)
    val builder = running(context, MainActivity.PAGE_STOPWATCH).setContentTitle("Stopwatch")
    val runningSince = stopwatch.runningSince
    if (runningSince != null) {
      builder.setUsesChronometer(true).setShowWhen(true).setWhen(runningSince - stopwatch.elapsed)
        .addAction(0, "Pause", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_STOPWATCH_PAUSE))
    } else {
      builder.setContentText("Paused · ${formatElapsed(stopwatch.elapsed)}")
        .addAction(0, "Resume", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_STOPWATCH_RESUME))
    }
    builder.addAction(0, "Reset", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_STOPWATCH_RESET))
    notify(context, ID_STOPWATCH, builder.build())
  }

  private fun running(context: Context, page: Int) = NotificationCompat.Builder(context, CHANNEL_RUNNING)
    .setSmallIcon(R.drawable.ic_alarm)
    .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setSilent(true)
    .setContentIntent(MainActivity.openPage(context, page))

  private fun notify(context: Context, id: Int, notification: Notification) {
    if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    context.getSystemService(NotificationManager::class.java).notify(id, notification)
  }

  private fun cancel(context: Context, id: Int) {
    context.getSystemService(NotificationManager::class.java).cancel(id)
  }
}
