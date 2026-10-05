package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Widths for the screens on tablets and desktop windows. A phone-sized column of cards is
 * readable, but on a 27" screen it leaves two thirds of the window empty and hides half the day
 * below the fold – so from [TWO_PANE_MIN] on, screens lay their cards out side by side, up to
 * [MAX_CONTENT] wide.
 */
object WideLayout {
    /** One column of cards, as on a phone: the most a line of text should run. */
    val READABLE: Dp = 720.dp
    /** From this content width on, screens use two panes or a grid of cards. */
    val TWO_PANE_MIN: Dp = 1000.dp
    /** Beyond this, wider windows only get wider margins. */
    val MAX_CONTENT: Dp = 1440.dp
    val PANE_GAP: Dp = 20.dp

    /** How wide the content of a window with [available] room is laid out. */
    fun contentWidth(available: Dp): Dp = when {
        available >= TWO_PANE_MIN -> minOf(available, MAX_CONTENT)
        else -> minOf(available, READABLE)
    }
}

/**
 * A screen of cards as two scrolling panes side by side on a wide window – [primary] on the left,
 * [secondary] on the right – and as one list ([primary] then [secondary]) on a phone.
 */
@Composable
fun AdaptivePanes(
    modifier: Modifier = Modifier,
    testTag: String? = null,
    spacing: Dp = 10.dp,
    primary: LazyListScope.() -> Unit,
    secondary: LazyListScope.() -> Unit,
) {
    val insets = LocalScreenInsets.current
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (maxWidth >= WideLayout.TWO_PANE_MIN) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(WideLayout.PANE_GAP)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight().tagged(testTag),
                    contentPadding = insets,
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    content = primary
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight().tagged(testTag?.let { "${it}_secondary" }),
                    contentPadding = insets,
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    // The left pane starts with the (in a title bar, empty) screen title row; an
                    // empty row here keeps the two panes' first cards level.
                    item { }
                    secondary()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = WideLayout.READABLE).fillMaxHeight().padding(horizontal = 12.dp).tagged(testTag),
                contentPadding = insets,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                primary()
                secondary()
            }
        }
    }
}

/** Items for [AdaptiveCards]: a card per item, or a [fullWidth] row such as a heading or a search field. */
class AdaptiveCardsScope internal constructor() {
    internal class Entry(val key: Any?, val fullWidth: Boolean, val content: @Composable () -> Unit)
    internal val entries = mutableListOf<Entry>()

    fun item(key: Any? = null, fullWidth: Boolean = false, content: @Composable () -> Unit) {
        entries += Entry(key, fullWidth, content)
    }

    fun <T> items(list: List<T>, key: ((T) -> Any)? = null, content: @Composable (T) -> Unit) {
        list.forEach { element -> entries += Entry(key?.invoke(element), false) { content(element) } }
    }
}

/**
 * A list of cards that becomes a grid of columns at least [minColumnWidth] wide on a wide window
 * (cards of different heights packed like a masonry wall), and one column on a phone.
 */
@Composable
fun AdaptiveCards(
    modifier: Modifier = Modifier,
    testTag: String? = null,
    minColumnWidth: Dp = 440.dp,
    spacing: Dp = 10.dp,
    content: AdaptiveCardsScope.() -> Unit,
) {
    val insets = LocalScreenInsets.current
    val scope = AdaptiveCardsScope().apply(content)
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (maxWidth >= WideLayout.TWO_PANE_MIN) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minColumnWidth),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).tagged(testTag),
                contentPadding = insets,
                verticalItemSpacing = spacing,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                scope.entries.forEach { entry ->
                    item(key = entry.key, span = if (entry.fullWidth) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane) { entry.content() }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = WideLayout.READABLE).fillMaxHeight().padding(horizontal = 12.dp).tagged(testTag),
                contentPadding = insets,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                scope.entries.forEach { entry -> item(key = entry.key) { entry.content() } }
            }
        }
    }
}

private fun Modifier.tagged(tag: String?): Modifier = if (tag != null) testTag(tag) else this
