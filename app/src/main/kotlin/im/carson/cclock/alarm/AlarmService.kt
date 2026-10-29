package im.carson.cclock.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import im.carson.cclock.data.ClockStore
import im.carson.cclock.schedule.Notifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Ringing(val label: String, val canSnooze: Boolean)

/** Plays the alarm sound and vibration in the foreground until snoozed or stopped. */
class AlarmService : Service() {
  private var player: MediaPlayer? = null
  private var vibrator: Vibrator? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val ringing = Ringing(intent?.getStringExtra(EXTRA_LABEL) ?: "Alarm", intent?.getBooleanExtra(EXTRA_CAN_SNOOZE, true) ?: true)
    _ringing.value = ringing
    ServiceCompat.startForeground(
      this, Notifications.ID_RINGING, Notifications.ringing(this, ringing), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
    )
    if (player == null) startSound()
    if (vibrator == null) startVibration()
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    player?.release()
    vibrator?.cancel()
    _ringing.value = null
    super.onDestroy()
  }

  private fun startSound() {
    val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    player = try {
      MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
        setDataSource(this@AlarmService, uri)
        isLooping = true
        setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
        prepare()
        start()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Couldn't play alarm sound", e)
      null
    }
  }

  private fun startVibration() {
    vibrator = getSystemService(VibratorManager::class.java).defaultVibrator.also {
      val pattern = VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0)
      it.vibrate(pattern, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
    }
  }

  companion object {
    private const val TAG = "AlarmService"
    private const val EXTRA_LABEL = "label"
    private const val EXTRA_CAN_SNOOZE = "can_snooze"

    private val _ringing = MutableStateFlow<Ringing?>(null)

    /** What's ringing, or null when nothing is. */
    val ringing: StateFlow<Ringing?> = _ringing.asStateFlow()

    /** Starts ringing. Timers pass [canSnooze] false, since snoozing a timer doesn't make sense. */
    fun start(context: Context, label: String, canSnooze: Boolean = true) {
      ContextCompat.startForegroundService(
        context,
        Intent(context, AlarmService::class.java).putExtra(EXTRA_LABEL, label).putExtra(EXTRA_CAN_SNOOZE, canSnooze),
      )
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, AlarmService::class.java))
    }

    fun snooze(context: Context) {
      ringing.value?.takeIf { it.canSnooze }?.let { ClockStore.snooze(context, it.label) }
      stop(context)
    }
  }
}
