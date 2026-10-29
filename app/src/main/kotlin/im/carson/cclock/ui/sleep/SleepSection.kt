package im.carson.cclock.ui.sleep

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import im.carson.cclock.R
import androidx.compose.ui.unit.dp
import im.carson.cclock.data.ClockState
import im.carson.cclock.data.ClockStore
import im.carson.cclock.data.SleepSchedule
import im.carson.cclock.formatDays
import im.carson.cclock.formatDuration
import im.carson.cclock.formatMinutes
import im.carson.cclock.orderedWeek
import im.carson.cclock.schedule.Planner
import im.carson.cclock.schedule.SleepPeriod
import im.carson.cclock.ui.components.AddButton
import im.carson.cclock.ui.components.DaySelector
import im.carson.cclock.ui.components.DialogButtons
import im.carson.cclock.ui.components.OutlinedDialog
import im.carson.cclock.ui.components.SectionTitle
import im.carson.cclock.ui.components.TextAction
import im.carson.cclock.ui.components.TimePickerDialog
import im.carson.cclock.ui.components.ValueRow
import im.carson.cclock.ui.components.rememberTimeFormatter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface SleepEdit {
  /** Changes only the given night, like iOS "Next Wake Up Only". */
  data class Night(val period: SleepPeriod) : SleepEdit

  /** Changes a recurring schedule. */
  data class Schedule(val schedule: SleepSchedule, val isNew: Boolean) : SleepEdit
}

/** The sleep schedule: an on/off switch, the next night, and the recurring schedules. */
@Composable
fun SleepSection(state: ClockState, now: ZonedDateTime) {
  val context = LocalContext.current
  val formatter = rememberTimeFormatter()
  var editing by remember { mutableStateOf<SleepEdit?>(null) }
  val unusedDays = (1..7).toSet() - state.schedules.flatMap { it.wakeDays }.toSet()

  SectionTitle("Sleep") {
    if (unusedDays.isNotEmpty()) {
      AddButton("Add sleep schedule") {
        editing = SleepEdit.Schedule(SleepSchedule(wakeDays = unusedDays, bedtime = 22 * 60 + 30, wake = 6 * 60 + 30), isNew = true)
      }
    }
    Switch(
      checked = state.sleepEnabled,
      onCheckedChange = { enabled -> ClockStore.edit(context) { it.copy(sleepEnabled = enabled) } },
    )
  }

  if (state.schedules.isEmpty()) {
    Text("Silences notifications from bedtime until your wake-up alarm.", color = MaterialTheme.colorScheme.secondary)
  }

  Planner.nextSleep(state, now)?.let { next ->
    val wakeDay = next.wakeDate.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val minutes = Planner.sleepMinutes(next.start.toMinuteOfDay(), next.end.toMinuteOfDay())
    Row(verticalAlignment = Alignment.CenterVertically) {
      SleepRow(
        label = buildString {
          append("Next · wake $wakeDay · ${formatDuration(minutes)}")
          if (next.isOverride) append(" · this night only")
          if (!next.alarm) append(" · no alarm")
        },
        times = "${formatter.format(next.start)} → ${formatter.format(next.end)}",
        modifier = Modifier.weight(1f),
      ) { editing = SleepEdit.Night(next) }
      if (next.isOverride) {
        TextAction("Reset") { ClockStore.setNightTimes(context, next.wakeDate.toString(), null, null) }
      }
      AlarmToggle(next.alarm, "this wake-up") { ClockStore.setNightAlarm(context, next.wakeDate.toString(), it) }
    }
  }

  val week = orderedWeek().map { it.value }
  state.schedules.sortedBy { schedule -> week.indexOfFirst { it in schedule.wakeDays } }.forEach { schedule ->
    Row(verticalAlignment = Alignment.CenterVertically) {
      SleepRow(
        label = formatDays(schedule.wakeDays) + if (schedule.alarm) "" else " · no alarm",
        times = "${formatter.formatMinutes(schedule.bedtime)} → ${formatter.formatMinutes(schedule.wake)}",
        modifier = Modifier.weight(1f),
        dimmed = !state.sleepEnabled,
      ) { editing = SleepEdit.Schedule(schedule, isNew = false) }
      AlarmToggle(schedule.alarm, "the alarm for ${formatDays(schedule.wakeDays)}") {
        ClockStore.setScheduleAlarm(context, schedule, it)
      }
    }
  }

  editing?.let { SleepEditor(it, onDismiss = { editing = null }) }
}

/** An alarm icon, crossed out when off, that turns a wake-up alarm on or off. */
@Composable
private fun AlarmToggle(on: Boolean, what: String, onChange: (Boolean) -> Unit) {
  IconButton(onClick = { onChange(!on) }) {
    Icon(
      painterResource(if (on) R.drawable.ic_alarm else R.drawable.ic_alarm_off),
      contentDescription = if (on) "Turn off $what" else "Turn on $what",
      tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
    )
  }
}

@Composable
private fun SleepRow(label: String, times: String, modifier: Modifier = Modifier, dimmed: Boolean = false, onClick: () -> Unit) {
  Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
    Text(label, color = MaterialTheme.colorScheme.secondary)
    Text(
      times,
      style = MaterialTheme.typography.titleLarge,
      color = if (dimmed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
    )
  }
}

private fun ZonedDateTime.toMinuteOfDay() = hour * 60 + minute

@Composable
private fun SleepEditor(edit: SleepEdit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val formatter = rememberTimeFormatter()
  var bedtime by remember {
    mutableStateOf(
      when (edit) {
        is SleepEdit.Night -> edit.period.start.toMinuteOfDay()
        is SleepEdit.Schedule -> edit.schedule.bedtime
      },
    )
  }
  var wake by remember {
    mutableStateOf(
      when (edit) {
        is SleepEdit.Night -> edit.period.end.toMinuteOfDay()
        is SleepEdit.Schedule -> edit.schedule.wake
      },
    )
  }
  var days by remember { mutableStateOf((edit as? SleepEdit.Schedule)?.schedule?.wakeDays.orEmpty()) }
  var picking by remember { mutableStateOf<String?>(null) }

  OutlinedDialog(onDismiss) {
    val title = when (edit) {
      is SleepEdit.Night -> "This night only"
      is SleepEdit.Schedule -> if (edit.isNew) "New schedule" else "Schedule"
    }
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    ValueRow("Bedtime", formatter.formatMinutes(bedtime)) { picking = "Bedtime" }
    ValueRow("Wake up", formatter.formatMinutes(wake)) { picking = "Wake up" }
    Text(
      formatDuration(Planner.sleepMinutes(bedtime, wake)),
      modifier = Modifier.align(Alignment.End),
      color = MaterialTheme.colorScheme.tertiary,
    )
    if (edit is SleepEdit.Schedule) {
      Spacer(Modifier.height(16.dp))
      Text("Wake-up days", color = MaterialTheme.colorScheme.secondary)
      Spacer(Modifier.height(8.dp))
      DaySelector(days) { days = it }
    }
    DialogButtons {
      if (edit is SleepEdit.Schedule && !edit.isNew) {
        TextAction("Delete", modifier = Modifier.weight(1f)) {
          ClockStore.deleteSchedule(context, edit.schedule.id)
          onDismiss()
        }
      }
      TextAction("Cancel", onClick = onDismiss)
      TextAction("Save", enabled = bedtime != wake && (edit is SleepEdit.Night || days.isNotEmpty())) {
        when (edit) {
          is SleepEdit.Night -> ClockStore.setNightTimes(context, edit.period.wakeDate.toString(), bedtime, wake)
          is SleepEdit.Schedule -> ClockStore.saveSchedule(context, edit.schedule.copy(wakeDays = days, bedtime = bedtime, wake = wake))
        }
        onDismiss()
      }
    }
  }

  picking?.let { which ->
    TimePickerDialog(
      title = which,
      initialMinutes = if (which == "Bedtime") bedtime else wake,
      onDismiss = { picking = null },
      onConfirm = { minutes ->
        if (which == "Bedtime") bedtime = minutes else wake = minutes
        picking = null
      },
    )
  }
}
