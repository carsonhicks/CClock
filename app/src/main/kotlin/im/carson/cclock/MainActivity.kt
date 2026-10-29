package im.carson.cclock

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import im.carson.cclock.schedule.Scheduler
import im.carson.cclock.ui.main.AppScreen
import im.carson.cclock.ui.theme.CClockTheme

class MainActivity : ComponentActivity() {
  /** A page a notification asked to open, until the pager has moved to it. */
  private var requestedPage by mutableStateOf<Int?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Transparent bars with light icons over the black background.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
    if (savedInstanceState == null) requestedPage = intent.page
    setContent {
      CClockTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppScreen(requestedPage = requestedPage, onPageOpened = { requestedPage = null })
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    requestedPage = intent.page
  }

  override fun onResume() {
    super.onResume()
    // Permissions may have been granted in Settings while we were away.
    Scheduler.reschedule(this)
  }

  private val Intent.page get() = getIntExtra(EXTRA_PAGE, -1).takeIf { it >= 0 }

  companion object {
    const val PAGE_CLOCK = 0
    const val PAGE_STOPWATCH = 1
    const val PAGE_TIMER = 2
    private const val EXTRA_PAGE = "page"

    fun openPage(context: Context, page: Int): PendingIntent = PendingIntent.getActivity(
      context, 100 + page,
      Intent(context, MainActivity::class.java).putExtra(EXTRA_PAGE, page).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
