package im.carson.cclock.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import im.carson.cclock.MainActivity
import im.carson.cclock.ui.stopwatch.StopwatchPage
import im.carson.cclock.ui.timer.TimerPage
import kotlinx.coroutines.launch

private val pages = listOf("Clock", "Stopwatch", "Timer")

/** Clock, stopwatch, and timer as pages you swipe between, named in a row of labels along the bottom. */
@Composable
fun AppScreen(requestedPage: Int?, onPageOpened: () -> Unit) {
  val pager = rememberPagerState { pages.size }
  val scope = rememberCoroutineScope()
  val open = { page: Int -> scope.launch { pager.animateScrollToPage(page) } }

  LaunchedEffect(requestedPage) {
    if (requestedPage != null) {
      pager.scrollToPage(requestedPage)
      onPageOpened()
    }
  }
  // Back returns to the clock before leaving the app.
  BackHandler(enabled = pager.currentPage != MainActivity.PAGE_CLOCK) { open(MainActivity.PAGE_CLOCK) }

  Column(modifier = Modifier.fillMaxSize()) {
    HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
      when (page) {
        MainActivity.PAGE_CLOCK -> ClockPage(onOpenPage = { open(it) })
        MainActivity.PAGE_STOPWATCH -> StopwatchPage()
        else -> TimerPage()
      }
    }
    // Equal thirds of the full width, so the middle label sits at the true center and the outer
    // ones can be tapped right out to the screen edges. Their labels are inset to line up with
    // the page content.
    val alignments = listOf(Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd)
    val insets = listOf(PaddingValues(start = 24.dp), PaddingValues(), PaddingValues(end = 24.dp))
    Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
      pages.forEachIndexed { index, name ->
        Box(
          modifier = Modifier.weight(1f).clickable { open(index) }.padding(insets[index]).padding(vertical = 16.dp),
          contentAlignment = alignments[index],
        ) {
          Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = if (pager.currentPage == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }
  }
}
