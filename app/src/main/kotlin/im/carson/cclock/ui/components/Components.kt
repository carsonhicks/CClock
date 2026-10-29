package im.carson.cclock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import im.carson.cclock.R
import im.carson.cclock.orderedWeek
import im.carson.cclock.schedule.Permissions
import im.carson.cclock.timeFormatter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/** The current time, updated on each second boundary. */
@Composable
fun rememberNow(): ZonedDateTime {
  var now by remember { mutableStateOf(ZonedDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000 - System.currentTimeMillis() % 1000)
      now = ZonedDateTime.now()
    }
  }
  return now
}

@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
  val context = LocalContext.current
  val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
  return remember(is24Hour) { timeFormatter(context) }
}

/** A section heading with optional controls on the right, like an add button or a switch. */
@Composable
fun SectionTitle(title: String, actions: @Composable RowScope.() -> Unit = {}) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
    actions()
  }
}

@Composable
fun AddButton(contentDescription: String, onClick: () -> Unit) {
  IconButton(onClick = onClick) {
    Icon(painterResource(R.drawable.ic_add), contentDescription, tint = MaterialTheme.colorScheme.primary)
  }
}

@Composable
fun SectionHeader(text: String) {
  Text(
    text,
    modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.tertiary,
  )
}

@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
  TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = ButtonDefaults.textButtonColors(
      contentColor = MaterialTheme.colorScheme.primary,
      disabledContentColor = MaterialTheme.colorScheme.tertiary,
    ),
  ) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
fun ValueRow(label: String, value: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
    Text(value, style = MaterialTheme.typography.titleLarge)
  }
}

/** Lists permissions CClock is missing, each linking to the screen that grants it. */
@Composable
fun PermissionRows(includeSleep: Boolean) {
  val context = LocalContext.current
  var resumes by remember { mutableIntStateOf(0) }
  LifecycleResumeEffect(Unit) {
    resumes++
    onPauseOrDispose {}
  }
  val missing = remember(resumes, includeSleep) { Permissions.missing(context, includeSleep) }
  if (missing.isEmpty()) return
  SectionHeader("Needs permission")
  missing.forEach { permission ->
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(permission.label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
      TextAction("Allow") { context.startActivity(permission.intent) }
    }
  }
}

/** A black dialog with a thin outline, since a black dialog on a black screen has no edge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
  BasicAlertDialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Column(modifier = Modifier.padding(24.dp), content = content)
    }
  }
}

@Composable
fun DialogButtons(content: @Composable () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    content()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
  val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
  val state = rememberTimePickerState(initialMinutes / 60, initialMinutes % 60, is24Hour)
  val colors = MaterialTheme.colorScheme
  OutlinedDialog(onDismiss) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = colors.tertiary)
    Spacer(Modifier.height(16.dp))
    TimePicker(
      state = state,
      colors = TimePickerDefaults.colors(
        clockDialColor = colors.background,
        clockDialSelectedContentColor = colors.onPrimary,
        clockDialUnselectedContentColor = colors.onBackground,
        selectorColor = colors.primary,
        containerColor = colors.background,
        periodSelectorBorderColor = colors.outline,
        periodSelectorSelectedContainerColor = colors.primary,
        periodSelectorUnselectedContainerColor = colors.background,
        periodSelectorSelectedContentColor = colors.onPrimary,
        periodSelectorUnselectedContentColor = colors.onBackground,
        timeSelectorSelectedContainerColor = colors.primary,
        timeSelectorUnselectedContainerColor = colors.background,
        timeSelectorSelectedContentColor = colors.onPrimary,
        timeSelectorUnselectedContentColor = colors.onBackground,
      ),
    )
    DialogButtons {
      TextAction("Cancel", onClick = onDismiss)
      TextAction("OK") { onConfirm(state.hour * 60 + state.minute) }
    }
  }
}

@Composable
fun DaySelector(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    orderedWeek().forEach { day ->
      val on = day.value in selected
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .then(if (on) Modifier.background(colors.primary) else Modifier.border(1.dp, colors.outline, CircleShape))
          .clickable { onChange(if (on) selected - day.value else selected + day.value) },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
          color = if (on) colors.onPrimary else colors.onBackground,
        )
      }
    }
  }
}
