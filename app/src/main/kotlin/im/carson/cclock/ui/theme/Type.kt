package im.carson.cclock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// OLauncher uses sans-serif-light throughout.
private fun TextStyle.light() = copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light)

private val base = Typography()

/** The big clock face. Separate from displayLarge, which the time picker also uses. */
val ClockTextStyle = base.displayLarge.light().copy(fontSize = 88.sp, lineHeight = 96.sp)

val Typography =
  Typography(
    displayLarge = base.displayLarge.light(),
    displaySmall = base.displaySmall.light(),
    headlineMedium = base.headlineMedium.light(),
    titleLarge = base.titleLarge.light(),
    titleMedium = base.titleMedium.light(),
    bodyLarge = base.bodyLarge.light(),
    bodyMedium = base.bodyMedium.light(),
    labelLarge = base.labelLarge.light(),
  )
