package im.carson.cclock.ui.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import im.carson.cclock.data.ClockStore
import im.carson.cclock.formatElapsed
import im.carson.cclock.formatHundredths
import im.carson.cclock.ui.components.BigButton
import im.carson.cclock.ui.components.rememberMillis
import im.carson.cclock.ui.theme.ClockTextStyle

@Composable
fun StopwatchPage() {
  val context = LocalContext.current
  val state by ClockStore.state.collectAsState()
  val stopwatch = state.stopwatch
  val elapsed = stopwatch.elapsedAt(rememberMillis(live = stopwatch.isRunning))

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // The reading sits where the clock does on the clock page.
    val readoutHeight = maxHeight * 0.3f
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
      Column(
        modifier = Modifier.fillMaxWidth().height(readoutHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Reading(elapsed)
        if (stopwatch.laps.isNotEmpty()) {
          val lap = elapsed - stopwatch.laps.last()
          Text(
            "Lap ${stopwatch.laps.size + 1} · ${formatElapsed(lap)}${formatHundredths(lap)}",
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (stopwatch.isRunning) {
          BigButton("Lap", modifier = Modifier.weight(1f)) { ClockStore.lapStopwatch(context) }
          BigButton("Pause", modifier = Modifier.weight(1f), filled = true) { ClockStore.pauseStopwatch(context) }
        } else {
          BigButton("Reset", modifier = Modifier.weight(1f), enabled = !stopwatch.isReset) { ClockStore.resetStopwatch(context) }
          BigButton(if (stopwatch.isReset) "Start" else "Resume", modifier = Modifier.weight(1f), filled = true) {
            ClockStore.startStopwatch(context)
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      // Finished laps, newest first: each lap's own time, then the total when it ended.
      val laps = stopwatch.laps.mapIndexed { i, total -> Triple(i + 1, total - (stopwatch.laps.getOrNull(i - 1) ?: 0L), total) }
      LazyColumn(modifier = Modifier.weight(1f)) {
        items(laps.asReversed(), key = { it.first }) { (number, lap, total) ->
          Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text("Lap $number", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
            Text("${formatElapsed(lap)}${formatHundredths(lap)}", modifier = Modifier.width(120.dp), textAlign = TextAlign.End)
            Text(
              "${formatElapsed(total)}${formatHundredths(total)}",
              modifier = Modifier.width(120.dp),
              textAlign = TextAlign.End,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
        }
      }
    }
  }
}

/** Minutes and seconds at clock size, with smaller hundredths beside them. */
@Composable
private fun Reading(elapsed: Long) {
  Row {
    Text(formatElapsed(elapsed), modifier = Modifier.alignByBaseline(), style = ClockTextStyle, maxLines = 1)
    Text(
      formatHundredths(elapsed),
      modifier = Modifier.alignByBaseline(),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.secondary,
      maxLines = 1,
    )
  }
}
