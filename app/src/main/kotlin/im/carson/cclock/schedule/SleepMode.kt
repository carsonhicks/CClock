package im.carson.cclock.schedule

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import im.carson.cclock.MainActivity
import im.carson.cclock.R

/** CClock's own Do Not Disturb mode, switched on for the length of each sleep period. */
object SleepMode {
  private const val TAG = "SleepMode"
  private val CONDITION_ID: Uri = Uri.parse("condition://im.carson.cclock/sleep")

  fun hasAccess(context: Context) =
    context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

  fun update(context: Context, enabled: Boolean, active: Boolean) {
    val nm = context.getSystemService(NotificationManager::class.java)
    try {
      val existing = nm.automaticZenRules.entries.find { it.value.conditionId == CONDITION_ID }?.key
      if (!enabled) {
        existing?.let(nm::removeAutomaticZenRule)
        return
      }
      val id = existing ?: nm.addAutomaticZenRule(buildRule(context))
      val summary = context.getString(if (active) R.string.sleep_mode_on else R.string.sleep_mode_off)
      nm.setAutomaticZenRuleState(
        id, Condition(CONDITION_ID, summary, if (active) Condition.STATE_TRUE else Condition.STATE_FALSE),
      )
    } catch (e: SecurityException) {
      Log.w(TAG, "No access to Do Not Disturb rules", e)
    }
  }

  private fun buildRule(context: Context): AutomaticZenRule {
    val name = context.getString(R.string.sleep_mode_name)
    val config = ComponentName(context, MainActivity::class.java)
    // Silence everything except alarms.
    val policy = ZenPolicy.Builder().disallowAllSounds().allowAlarms(true).build()
    val filter = NotificationManager.INTERRUPTION_FILTER_PRIORITY
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      return AutomaticZenRule.Builder(name, CONDITION_ID)
        .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)
        .setConfigurationActivity(config)
        .setInterruptionFilter(filter)
        .setZenPolicy(policy)
        .setIconResId(R.drawable.ic_sleep)
        .setTriggerDescription(context.getString(R.string.sleep_mode_trigger))
        .build()
    }
    return AutomaticZenRule(name, null, config, CONDITION_ID, policy, filter, true)
  }
}
