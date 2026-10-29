package im.carson.cclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Always dark: the app is meant to be pure black regardless of system theme.
private val OledColorScheme =
  darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = WhiteTrans80,
    onSecondary = Black,
    tertiary = WhiteTrans50,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Black,
    onSurfaceVariant = WhiteTrans80,
    outline = WhiteTrans50,
    outlineVariant = WhiteTrans50,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Black,
    surfaceContainer = Black,
    surfaceContainerHigh = Black,
    surfaceContainerHighest = Black,
  )

@Composable
fun CClockTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = OledColorScheme, typography = Typography, content = content)
}
