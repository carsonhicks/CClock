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
import im.carson.cclock.timeFormatter

object Notifications {
  private const val CHANNEL_RINGING = "ringing"
  private const val CHANNEL_UPCOMING = "upcoming"
  const val ID_RINGING = 1
  private const val ID_UPCOMING = 2

  fun createChannels(context: Context) {
    val ringing = NotificationChannel(CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
      // AlarmService plays the sound and vibration itself.
      setSound(null, null)
      enableVibration(false)
      setBypassDnd(true)
    }
    val upcoming = NotificationChannel(CHANNEL_UPCOMING, "Upcoming alarms", NotificationManager.IMPORTANCE_LOW)
    context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(ringing, upcoming))
  }

  fun ringing(context: Context, label: String): Notification {
    val fullScreen = PendingIntent.getActivity(
      context, 0,
      Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(context, CHANNEL_RINGING)
      .setSmallIcon(R.drawable.ic_alarm)
      .setContentTitle(label)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setFullScreenIntent(fullScreen, true)
      .setContentIntent(fullScreen)
      .addAction(0, "Snooze ${Planner.SNOOZE.toMinutes()} min", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_SNOOZE))
      .addAction(0, "Stop", ClockReceiver.pendingIntent(context, ClockReceiver.ACTION_STOP))
      .build()
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
}
