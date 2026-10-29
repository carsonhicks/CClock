package im.carson.cclock.ui.timer

import im.carson.cclock.data.Stopwatch
import im.carson.cclock.data.Timer
import im.carson.cclock.formatElapsed
import im.carson.cclock.formatHundredths
import im.carson.cclock.formatRemaining
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerMathTest {
  @Test
  fun typedDigitsFillFromTheRight() {
    assertEquals(90_000L, digitsToMillis("130"))
    assertEquals(90_000L, digitsToMillis("90"))
    assertEquals(3_723_000L, digitsToMillis("10203"))
    assertEquals(0L, digitsToMillis(""))
  }

  @Test
  fun lengthsRoundTripThroughDigits() {
    assertEquals("500", toDigits(5 * 60_000L))
    assertEquals("10203", toDigits(3_723_000L))
    assertEquals(3_723_000L, digitsToMillis(toDigits(3_723_000L)))
  }

  @Test
  fun timerCountsDownOnlyWhileRunning() {
    val running = Timer(60_000L, endsAt = 1_000_000L)
    assertEquals(15_000L, running.remainingAt(985_000L))
    assertEquals(0L, running.remainingAt(2_000_000L))
    assertEquals(20_000L, Timer(60_000L, remaining = 20_000L).remainingAt(5_000_000L))
  }

  @Test
  fun stopwatchAddsTheRunningStretch() {
    assertEquals(7_500L, Stopwatch(runningSince = 1_000L, elapsed = 2_500L).elapsedAt(6_000L))
    assertEquals(2_500L, Stopwatch(elapsed = 2_500L).elapsedAt(99_000L))
  }

  @Test
  fun readingsFormat() {
    assertEquals("0:05", formatElapsed(5_999L))
    assertEquals("1:02:03", formatElapsed(3_723_000L))
    assertEquals("0:05", formatRemaining(4_001L))
    assertEquals(".07", formatHundredths(1_075L))
  }
}
