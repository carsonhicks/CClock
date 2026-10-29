package im.carson.cclock.schedule

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** A permission CClock needs but doesn't have, with the settings screen that grants it. */
data class MissingPermission(val label: String, val intent: Intent)

object Permissions {
  fun missing(context: Context, includeSleep: Boolean): List<MissingPermission> = buildList {
    val nm = context.getSystemService(NotificationManager::class.java)
    val packageUri = Uri.parse("package:${context.packageName}")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      add(MissingPermission(
        "Notifications",
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
      ))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !nm.canUseFullScreenIntent()) {
      add(MissingPermission("Full-screen alarms", Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    ) {
      add(MissingPermission("Alarms & reminders", Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)))
    }
    if (includeSleep && !SleepMode.hasAccess(context)) {
      add(MissingPermission("Do Not Disturb access", Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
    }
  }
}
