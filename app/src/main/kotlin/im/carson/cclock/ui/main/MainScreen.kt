package im.carson.cclock.ui.main

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import im.carson.cclock.data.ClockStore
import im.carson.cclock.schedule.Planner
import im.carson.cclock.ui.alarms.AlarmsSection
import im.carson.cclock.ui.components.PermissionRows
import im.carson.cclock.ui.components.TextAction
import im.carson.cclock.ui.components.rememberNow
import im.carson.cclock.ui.components.rememberTimeFormatter
import im.carson.cclock.ui.sleep.SleepSection
import im.carson.cclock.ui.theme.CClockTheme
import im.carson.cclock.ui.theme.ClockTextStyle
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything on one scrolling page: the clock up top, then sleep and alarms underneath. */
@Composable
fun MainScreen() {
  val context = LocalContext.current
  val now = rememberNow()
  val state by ClockStore.state.collectAsState()
  val formatter = rememberTimeFormatter()
  val ring = Planner.nextRing(state, now)
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // The clock takes the top of the first screen, with sleep and alarms visible below.
    val clockHeight = maxHeight * 0.3f
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .systemBarsPadding()
        .padding(horizontal = 24.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().height(clockHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        ClockFace(time = now)
        if (Planner.activeSleep(state, now) != null) {
          Text("Sleep mode on", color = MaterialTheme.colorScheme.secondary)
        }
        if (ring != null) {
          // Show the weekday when the next ring isn't within the next day.
          val dayPrefix = if (Duration.between(now, ring.at) > Duration.ofHours(23)) {
            ring.at.format(DateTimeFormatter.ofPattern("EEE ", Locale.getDefault()))
          } else {
            ""
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${ring.label} · $dayPrefix${formatter.format(ring.at)}", color = MaterialTheme.colorScheme.secondary)
            if (Planner.canSkip(ring, now)) {
              TextAction(if (ring.isSnooze) "Dismiss" else "Skip") { ClockStore.finish(context, ring.key, ring.alarmId) }
            }
          }
        }
      }
      PermissionRows(includeSleep = state.sleepEnabled)
      SleepSection(state, now)
      AlarmsSection(state)
      Spacer(Modifier.height(32.dp))
    }
  }
}

@Composable
fun ClockFace(time: ZonedDateTime, modifier: Modifier = Modifier) {
  val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
  val millis = time.toInstant().toEpochMilli()
  // The AM/PM marker is drawn smaller so 12-hour times fit the width.
  val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), if (is24Hour) "Hm" else "hm")
  val digits = DateFormat.format(pattern.replace("a", "").trim(), millis)
  val marker = if (is24Hour) null else DateFormat.format("a", millis)
  Row(modifier = modifier) {
    Text(
      text = digits.toString(),
      modifier = Modifier.alignByBaseline(),
      style = ClockTextStyle,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
    )
    if (marker != null) {
      Text(
        text = marker.toString(),
        modifier = Modifier.alignByBaseline().padding(start = 8.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.secondary,
        maxLines = 1,
      )
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ClockFacePreview() {
  CClockTheme { ClockFace(time = ZonedDateTime.now()) }
}
