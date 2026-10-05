package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app icon (composeApp/icons/icon.svg) drawn in Compose: the stopwatch with the green "done"
 * badge on the blue tile, so the mark in the navigation rail is the same as in the Dock and on the
 * home screen – not a plain stopwatch that looks like the Today tab's icon.
 */
@Composable
fun AppMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size).semantics { contentDescription = "WorkTracker" }) {
        // The SVG is drawn on a 1024 grid.
        val u = this.size.width / 1024f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        val white = Color.White

        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF2F6BFF), Color(0xFF1638B8)), start = Offset.Zero, end = Offset(this.size.width, this.size.height)),
            cornerRadius = CornerRadius(224 * u)
        )
        // Crown
        drawRoundRect(white, topLeft = p(462f, 150f), size = Size(100 * u, 70 * u), cornerRadius = CornerRadius(22 * u))
        drawRect(white, topLeft = p(492f, 205f), size = Size(40 * u, 60 * u))
        // Dial
        drawCircle(white, radius = 300 * u, center = p(512f, 560f), style = Stroke(width = 58 * u))
        // Quarter ticks
        val tick = 30 * u
        drawLine(white, p(512f, 326f), p(512f, 372f), tick, StrokeCap.Round)
        drawLine(white, p(512f, 748f), p(512f, 794f), tick, StrokeCap.Round)
        drawLine(white, p(278f, 560f), p(324f, 560f), tick, StrokeCap.Round)
        drawLine(white, p(700f, 560f), p(746f, 560f), tick, StrokeCap.Round)
        // Hands
        drawLine(white, p(512f, 560f), p(512f, 400f), 46 * u, StrokeCap.Round)
        drawLine(white, p(512f, 560f), p(646f, 620f), 46 * u, StrokeCap.Round)
        drawCircle(white, radius = 34 * u, center = p(512f, 560f))
        // "Done" badge
        drawCircle(Color(0xFF10B981), radius = 130 * u, center = p(790f, 790f))
        drawCircle(white, radius = 130 * u, center = p(790f, 790f), style = Stroke(width = 34 * u))
        val check = Path().apply {
            moveTo(728 * u, 792 * u); lineTo(772 * u, 836 * u); lineTo(856 * u, 744 * u)
        }
        drawPath(check, white, style = Stroke(width = 40 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
