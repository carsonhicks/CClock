package im.carson.cclock.alarm

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import im.carson.cclock.schedule.Planner
import im.carson.cclock.ui.components.rememberNow
import im.carson.cclock.ui.main.ClockFace
import im.carson.cclock.ui.theme.CClockTheme

/** Full-screen alarm shown over the lock screen while [AlarmService] is ringing. */
class AlarmActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setShowWhenLocked(true)
    setTurnScreenOn(true)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      CClockTheme {
        val label by AlarmService.ringing.collectAsState()
        LaunchedEffect(label) { if (label == null) finish() }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AlarmScreen(
            label = label.orEmpty(),
            onSnooze = { AlarmService.snooze(this) },
            onStop = { AlarmService.stop(this) },
          )
        }
      }
    }
  }
}

@Composable
private fun AlarmScreen(label: String, onSnooze: () -> Unit, onStop: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.weight(1f))
    ClockFace(time = rememberNow())
    Text(label, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.weight(1f))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(64.dp), border = border) {
        Text("Snooze ${Planner.SNOOZE.toMinutes()} min", style = MaterialTheme.typography.titleMedium)
      }
      OutlinedButton(
        onClick = onStop,
        modifier = Modifier.weight(1f).height(64.dp),
        border = border,
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Text("Stop", style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}
