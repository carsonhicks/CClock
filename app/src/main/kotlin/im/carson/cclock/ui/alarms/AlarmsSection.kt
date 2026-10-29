package im.carson.cclock.ui.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import im.carson.cclock.data.Alarm
import im.carson.cclock.data.ClockState
import im.carson.cclock.data.ClockStore
import im.carson.cclock.formatDays
import im.carson.cclock.formatMinutes
import im.carson.cclock.ui.components.AddButton
import im.carson.cclock.ui.components.DaySelector
import im.carson.cclock.ui.components.DialogButtons
import im.carson.cclock.ui.components.OutlinedDialog
import im.carson.cclock.ui.components.SectionTitle
import im.carson.cclock.ui.components.TextAction
import im.carson.cclock.ui.components.TimePickerDialog
import im.carson.cclock.ui.components.ValueRow
import im.carson.cclock.ui.components.rememberTimeFormatter

/** Standalone alarms, each with its own switch. */
@Composable
fun AlarmsSection(state: ClockState) {
  val context = LocalContext.current
  val formatter = rememberTimeFormatter()
  var editing by remember { mutableStateOf<Pair<Alarm, Boolean>?>(null) }

  SectionTitle("Alarms") {
    AddButton("Add alarm") { editing = Alarm(time = 7 * 60) to true }
  }
  // The last swiped-away alarm, kept briefly so the delete can be undone.
  var deleted by remember { mutableStateOf<Alarm?>(null) }
  deleted?.let { alarm ->
    LaunchedEffect(alarm) {
      delay(5_000)
      deleted = null
    }
  }

  val rows = (state.alarms + listOfNotNull(deleted)).sortedBy { it.time }
  if (rows.isEmpty()) {
    Text("No alarms", color = MaterialTheme.colorScheme.secondary)
  }
  rows.forEach { alarm ->
    key(alarm.id) {
      if (alarm == deleted) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("Deleted ${formatter.formatMinutes(alarm.time)}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
          TextAction("Undo") {
            ClockStore.saveAlarm(context, alarm)
            deleted = null
          }
        }
      } else {
        SwipeToDelete(onDelete = {
          deleted = alarm
          ClockStore.deleteAlarm(context, alarm.id)
        }) {
          AlarmRow(alarm, onClick = { editing = alarm to false })
        }
      }
    }
  }

  editing?.let { (alarm, isNew) -> AlarmEditor(alarm, isNew, onDismiss = { editing = null }) }
}

@Composable
private fun AlarmRow(alarm: Alarm, onClick: () -> Unit) {
  val context = LocalContext.current
  val formatter = rememberTimeFormatter()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        listOf(alarm.label, formatDays(alarm.days)).filter { it.isNotBlank() }.joinToString(" · "),
        color = MaterialTheme.colorScheme.secondary,
      )
      Text(
        formatter.formatMinutes(alarm.time),
        style = MaterialTheme.typography.titleLarge,
        color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary,
      )
    }
    Switch(checked = alarm.enabled, onCheckedChange = { ClockStore.saveAlarm(context, alarm.copy(enabled = it)) })
  }
}

/** Swipe the row left to delete it; "Delete" is revealed underneath as it slides. */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
  val state = rememberSwipeToDismissBoxState()
  SwipeToDismissBox(
    state = state,
    enableDismissFromStartToEnd = false,
    onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
    backgroundContent = {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Text("Delete", color = MaterialTheme.colorScheme.onBackground)
      }
    },
  ) { content() }
}

@Composable
private fun AlarmEditor(alarm: Alarm, isNew: Boolean, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val formatter = rememberTimeFormatter()
  var time by remember { mutableStateOf(alarm.time) }
  var days by remember { mutableStateOf(alarm.days) }
  var label by remember { mutableStateOf(alarm.label) }
  var picking by remember { mutableStateOf(isNew) }

  OutlinedDialog(onDismiss) {
    Text(if (isNew) "New alarm" else "Alarm", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    ValueRow("Time", formatter.formatMinutes(time)) { picking = true }
    Spacer(Modifier.height(8.dp))
    Text("Repeat · ${formatDays(days)}", color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(8.dp))
    DaySelector(days) { days = it }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
      value = label,
      onValueChange = { label = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Label") },
      singleLine = true,
    )
    DialogButtons {
      if (!isNew) {
        TextAction("Delete", modifier = Modifier.weight(1f)) {
          ClockStore.deleteAlarm(context, alarm.id)
          onDismiss()
        }
      }
      TextAction("Cancel", onClick = onDismiss)
      TextAction("Save") {
        ClockStore.saveAlarm(context, alarm.copy(time = time, days = days, label = label.trim(), enabled = true))
        onDismiss()
      }
    }
  }

  if (picking) {
    TimePickerDialog(
      title = "Time",
      initialMinutes = time,
      onDismiss = { picking = false },
      onConfirm = {
        time = it
        picking = false
      },
    )
  }
}
