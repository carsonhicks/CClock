package im.carson.cclock

import android.app.Application
import im.carson.cclock.data.ClockStore
import im.carson.cclock.schedule.Notifications

class CClockApp : Application() {
  override fun onCreate() {
    super.onCreate()
    ClockStore.load(this)
    Notifications.createChannels(this)
  }
}
