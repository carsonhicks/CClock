package im.carson.cclock.ui.timer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import im.carson.cclock.data.ClockStore
import im.carson.cclock.data.Timer
import im.carson.cclock.formatElapsed
import im.carson.cclock.formatRemaining
import im.carson.cclock.ui.components.BigButton
import im.carson.cclock.ui.components.rememberMillis
import im.carson.cclock.ui.theme.ClockTextStyle

/** Enter a length and start it, or watch and control the timer that's set. */
@Composable
fun TimerPage() {
  val state by ClockStore.state.collectAsState()
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // The reading sits where the clock does on the clock page.
    val readoutHeight = maxHeight * 0.3f
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
      val timer = state.timer
      if (timer == null) {
        TimerEntry(state.lastTimer, readoutHeight)
      } else {
        RunningTimer(timer, readoutHeight)
      }
    }
  }
}

@Composable
private fun ColumnScope.TimerEntry(lastTimer: Long, readoutHeight: Dp) {
  val context = LocalContext.current
  var digits by remember(lastTimer) { mutableStateOf(toDigits(lastTimer)) }
  // The last length is offered until a key is pressed, which starts a fresh entry.
  var fresh by remember(lastTimer) { mutableStateOf(true) }
  val type = { key: String ->
    val base = if (fresh) "" else digits
    digits = (base + key).trimStart('0').take(6)
    fresh = false
  }

  Box(modifier = Modifier.fillMaxWidth().height(readoutHeight), contentAlignment = Alignment.Center) {
    val padded = digits.padStart(6, '0')
    Row(verticalAlignment = Alignment.Bottom) {
      listOf("h" to padded.substring(0, 2), "m" to padded.substring(2, 4), "s" to padded.substring(4, 6)).forEach { (unit, value) ->
        Text(value, modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.displayLarge)
        Text(
          unit,
          modifier = Modifier.alignByBaseline().padding(start = 2.dp, end = 12.dp),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }

  listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫")).forEach { row ->
    Row(modifier = Modifier.fillMaxWidth()) {
      row.forEach { key ->
        Box(
          modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .clip(CircleShape)
            .clickable {
              if (key == "⌫") {
                digits = digits.dropLast(1)
                fresh = false
              } else {
                type(key)
              }
            },
          contentAlignment = Alignment.Center,
        ) {
          Text(key, style = MaterialTheme.typography.headlineMedium)
        }
      }
    }
  }
  Spacer(Modifier.weight(1f))
  val duration = digitsToMillis(digits)
  BigButton(
    "Start",
    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    filled = true,
    enabled = duration > 0,
  ) { ClockStore.startTimer(context, duration) }
}

@Composable
private fun ColumnScope.RunningTimer(timer: Timer, readoutHeight: Dp) {
  val context = LocalContext.current
  val now = rememberMillis(live = timer.isRunning)
  val remaining = timer.remainingAt(now)

  Column(
    modifier = Modifier.fillMaxWidth().height(readoutHeight),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(formatRemaining(remaining), style = ClockTextStyle, maxLines = 1)
    Text(
      if (timer.isRunning) "of ${formatElapsed(timer.duration)}" else "Paused · of ${formatElapsed(timer.duration)}",
      color = MaterialTheme.colorScheme.secondary,
    )
  }
  LinearProgressIndicator(
    progress = { if (timer.duration > 0) remaining.toFloat() / timer.duration else 0f },
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.outline,
    gapSize = 0.dp,
    drawStopIndicator = {},
  )
  Spacer(Modifier.height(32.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    BigButton("Cancel", modifier = Modifier.weight(1f)) { ClockStore.clearTimer(context) }
    BigButton("+1:00", modifier = Modifier.weight(1f)) { ClockStore.addToTimer(context, 60_000L) }
    if (timer.isRunning) {
      BigButton("Pause", modifier = Modifier.weight(1f), filled = true) { ClockStore.pauseTimer(context) }
    } else {
      BigButton("Resume", modifier = Modifier.weight(1f), filled = true) { ClockStore.resumeTimer(context) }
    }
  }
}

/** Typed digits as hhmmss, right-aligned, so "130" is 1 minute 30 seconds. Fields may exceed 59. */
internal fun digitsToMillis(digits: String): Long {
  val padded = digits.padStart(6, '0')
  val h = padded.substring(0, 2).toLong()
  val m = padded.substring(2, 4).toLong()
  val s = padded.substring(4, 6).toLong()
  return ((h * 60 + m) * 60 + s) * 1000
}

/** A length back as typed digits, the reverse of [digitsToMillis]. */
internal fun toDigits(millis: Long): String {
  val seconds = millis / 1000
  return "%02d%02d%02d".format(minOf(seconds / 3600, 99), seconds / 60 % 60, seconds % 60).trimStart('0')
}
