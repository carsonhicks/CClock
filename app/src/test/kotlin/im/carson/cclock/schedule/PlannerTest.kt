package im.carson.cclock.schedule

import im.carson.cclock.data.Alarm
import im.carson.cclock.data.ClockState
import im.carson.cclock.data.SleepOverride
import im.carson.cclock.data.SleepSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
  private val zone = ZoneId.of("America/Chicago")

  // 2026-10-26 is a Monday.
  private fun at(day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
    LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone)

  private val weekdays = SleepSchedule(id = "wd", wakeDays = setOf(1, 2, 3, 4, 5), bedtime = 22 * 60 + 30, wake = 6 * 60 + 30)
  private val weekends = SleepSchedule(id = "we", wakeDays = setOf(6, 7), bedtime = 23 * 60 + 30, wake = 8 * 60)
  private val sleeping = ClockState(sleepEnabled = true, schedules = listOf(weekdays, weekends))

  @Test
  fun bedtimeBeforeMidnightStartsTheEveningBefore() {
    val period = Planner.sleepPeriod(sleeping, LocalDate.of(2026, 10, 27), zone)!!
    assertEquals(at(26, 22, 30), period.start)
    assertEquals(at(27, 6, 30), period.end)
  }

  @Test
  fun weekendScheduleAppliesOnSaturdayWakeUp() {
    // Friday night into Saturday uses the weekend schedule.
    val period = Planner.nextSleep(sleeping, at(30, 12))!!
    assertEquals(at(30, 23, 30), period.start)
    assertEquals(at(31, 8), period.end)
  }

  @Test
  fun activeDuringTheNightOnly() {
    assertNotNull(Planner.activeSleep(sleeping, at(27, 2)))
    assertNull(Planner.activeSleep(sleeping, at(27, 12)))
    assertNull(Planner.activeSleep(sleeping.copy(sleepEnabled = false), at(27, 2)))
  }

  @Test
  fun overrideChangesOneNightOnly() {
    val state = sleeping.copy(overrides = listOf(SleepOverride("2026-10-27", bedtime = 0, wake = 9 * 60)))
    val changed = Planner.sleepPeriod(state, LocalDate.of(2026, 10, 27), zone)!!
    assertEquals(at(27, 0), changed.start)
    assertEquals(at(27, 9), changed.end)
    assertTrue(changed.isOverride)
    val following = Planner.sleepPeriod(state, LocalDate.of(2026, 10, 28), zone)!!
    assertEquals(at(28, 6, 30), following.end)
  }

  @Test
  fun wakeUpRingsAtEndOfSleep() {
    val ring = Planner.nextRing(sleeping, at(27, 2))!!
    assertEquals(at(27, 6, 30), ring.at)
    assertEquals("sleep:2026-10-27", ring.key)
  }

  @Test
  fun scheduleAlarmOffKeepsSleepModeButDoesNotRing() {
    val state = sleeping.copy(schedules = listOf(weekdays.copy(alarm = false), weekends))
    assertNotNull(Planner.activeSleep(state, at(27, 2)))
    // Weekday wake-ups are silent; the weekend schedule still rings Saturday.
    assertEquals(at(31, 8), Planner.nextRing(state, at(27, 2))!!.at)
    assertNull(Planner.nextRing(state.copy(schedules = listOf(weekdays.copy(alarm = false))), at(27, 2)))
  }

  @Test
  fun nightWithoutAlarmRingsTheFollowingMorning() {
    val state = sleeping.copy(overrides = listOf(SleepOverride("2026-10-27", alarm = false)))
    val tonight = Planner.sleepPeriod(state, LocalDate.of(2026, 10, 27), zone)!!
    assertEquals(at(27, 6, 30), tonight.end)
    assertTrue(!tonight.isOverride && !tonight.alarm)
    assertNotNull(Planner.activeSleep(state, at(27, 2)))
    assertEquals(at(28, 6, 30), Planner.nextRing(state, at(27, 2))!!.at)
  }

  @Test
  fun nightAlarmCanRingWhileWakeAlarmIsOff() {
    val state = sleeping.copy(schedules = listOf(weekdays.copy(alarm = false), weekends), overrides = listOf(SleepOverride("2026-10-28", alarm = true)))
    assertEquals(at(28, 6, 30), Planner.nextRing(state, at(27, 2))!!.at)
  }

  @Test
  fun skippingWakeUpEndsSleepAndMovesToNextNight() {
    val state = sleeping.copy(done = setOf("sleep:2026-10-27"))
    assertNull(Planner.activeSleep(state, at(27, 5)))
    assertEquals(at(28, 6, 30), Planner.nextRing(state, at(27, 5))!!.at)
  }

  @Test
  fun earliestOfAlarmAndWakeUpRingsFirst() {
    val state = sleeping.copy(alarms = listOf(Alarm(id = "a", time = 6 * 60, days = setOf(2))))
    val ring = Planner.nextRing(state, at(27, 2))!!
    assertEquals(at(27, 6), ring.at)
    assertEquals("a", ring.alarmId)
  }

  @Test
  fun repeatingAlarmSkipsToItsNextDay() {
    val alarm = Alarm(id = "a", time = 7 * 60, days = setOf(1, 3))
    val first = Planner.alarmRing(alarm, ClockState(alarms = listOf(alarm)), at(26, 8))!!
    assertEquals(at(28, 7), first.at)
    val skipped = ClockState(alarms = listOf(alarm), done = setOf(first.key))
    assertEquals(at(26, 7).plusWeeks(1), Planner.alarmRing(alarm, skipped, at(26, 8))!!.at)
  }

  @Test
  fun oneShotAlarmRingsAtNextOccurrence() {
    val alarm = Alarm(id = "a", time = 7 * 60)
    assertEquals(at(27, 7), Planner.alarmRing(alarm, ClockState(), at(26, 8))!!.at)
    assertEquals(at(26, 7), Planner.alarmRing(alarm, ClockState(), at(26, 6))!!.at)
  }

  @Test
  fun justRungAlarmStaysDueWithinGraceUntilMarkedDone() {
    val alarm = Alarm(id = "a", time = 7 * 60, days = setOf(1))
    val state = ClockState(alarms = listOf(alarm))
    val ring = Planner.nextRing(state, at(26, 7).plusSeconds(20))!!
    assertEquals(at(26, 7), ring.at)
    val after = Planner.nextRing(state.copy(done = setOf(ring.key)), at(26, 7).plusSeconds(20))!!
    assertEquals(at(26, 7).plusWeeks(1), after.at)
  }

  @Test
  fun skipOnlyOfferedWithinUpcomingWindow() {
    val ring = Ring("k", at(27, 6, 30), "Wake up")
    assertTrue(Planner.canSkip(ring, at(27, 4, 30)))
    assertTrue(!Planner.canSkip(ring, at(27, 4, 29)))
  }

  @Test
  fun pruneDropsOldBookkeeping() {
    val state = sleeping.copy(
      done = setOf("sleep:2026-10-20", "sleep:2026-10-27", "alarm:a:${at(20, 7).toEpochSecond()}", "alarm:a:${at(28, 7).toEpochSecond()}"),
      overrides = listOf(SleepOverride("2026-10-20", 0, 60), SleepOverride("2026-10-28", 0, 60)),
    )
    val pruned = Planner.prune(state, at(27, 12))
    assertEquals(setOf("sleep:2026-10-27", "alarm:a:${at(28, 7).toEpochSecond()}"), pruned.done)
    assertEquals(listOf("2026-10-28"), pruned.overrides.map { it.wakeDate })
  }
}
