package com.rork.blockblastsolver.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.domain.BOARD_SIZE
import com.rork.blockblastsolver.domain.BoardOps
import com.rork.blockblastsolver.ui.theme.GridCell
import com.rork.blockblastsolver.ui.theme.GridFilled
import com.rork.blockblastsolver.ui.theme.GridFilledEdge
import com.rork.blockblastsolver.ui.theme.GridLine
import com.rork.blockblastsolver.ui.theme.NeonPink
import com.rork.blockblastsolver.ui.theme.NeonTeal

/** Highlight layers drawn on top of the board, keyed by board bit masks. */
data class BoardHighlight(
    val placementMask: Long = 0L,
    val clearedMask: Long = 0L,
    val ghostMask: Long = 0L,
    val accent: Color = NeonTeal
)

/**
 * The board renderer used everywhere in the app: scan review, solution preview, history
 * thumbnails and alternative-move cards. Pass [onCellTap] to make it editable.
 */
@Composable
fun BoardGrid(
    bits: Long,
    modifier: Modifier = Modifier,
    highlight: BoardHighlight = BoardHighlight(),
    animateHighlight: Boolean = false,
    onCellTap: ((row: Int, col: Int) -> Unit)? = null
) {
    val pulse: Float = if (animateHighlight) {
        val transition = rememberInfiniteTransition(label = "highlight")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "highlightAlpha"
        )
        value
    } else {
        1f
    }

    val tapModifier = if (onCellTap != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val unit = size.width.toFloat() / BOARD_SIZE
                val col = (offset.x / unit).toInt().coerceIn(0, BOARD_SIZE - 1)
                val row = (offset.y / unit).toInt().coerceIn(0, BOARD_SIZE - 1)
                onCellTap(row, col)
            }
        }
    } else {
        Modifier
    }

    val filledCount = remember(bits) { BoardOps.filledCount(bits) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(tapModifier)
            .semantics {
                contentDescription = "Papan 8 kali 8, $filledCount kotak terisi"
            }
    ) {
        val unit = size.width / BOARD_SIZE
        val gap = unit * 0.06f
        val radius = unit * 0.16f

        // Base cells
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val topLeft = Offset(col * unit + gap / 2f, row * unit + gap / 2f)
                val cellSize = Size(unit - gap, unit - gap)
                drawRoundRectCompat(GridCell, topLeft, cellSize, radius)
            }
        }

        // Grid lines
        for (i in 0..BOARD_SIZE) {
            val position = i * unit
            drawLine(
                color = GridLine,
                start = Offset(position, 0f),
                end = Offset(position, size.height),
                strokeWidth = unit * 0.02f
            )
            drawLine(
                color = GridLine,
                start = Offset(0f, position),
                end = Offset(size.width, position),
                strokeWidth = unit * 0.02f
            )
        }

        // Occupied cells with a subtle bevel so the board reads like the real game
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (!BoardOps.isFilled(bits, row, col)) continue
                if (highlight.placementMask and BoardOps.bit(row, col) != 0L) continue
                val inset = unit * 0.1f
                val topLeft = Offset(col * unit + inset, row * unit + inset)
                val cellSize = Size(unit - inset * 2, unit - inset * 2)
                val cleared = highlight.clearedMask and BoardOps.bit(row, col) != 0L
                drawRoundRectCompat(
                    if (cleared) NeonTeal.copy(alpha = 0.32f) else GridFilled,
                    topLeft,
                    cellSize,
                    radius
                )
                drawRoundRectCompat(
                    GridFilledEdge.copy(alpha = 0.6f),
                    Offset(topLeft.x, topLeft.y + cellSize.height * 0.68f),
                    Size(cellSize.width, cellSize.height * 0.32f),
                    radius * 0.6f
                )
            }
        }

        // Ghost (pending / alternative) cells
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (highlight.ghostMask and BoardOps.bit(row, col) == 0L) continue
                val inset = unit * 0.14f
                drawRoundRectCompat(
                    NeonPink.copy(alpha = 0.55f),
                    Offset(col * unit + inset, row * unit + inset),
                    Size(unit - inset * 2, unit - inset * 2),
                    radius
                )
            }
        }

        // The recommended placement: glowing accent blocks on top of everything
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (highlight.placementMask and BoardOps.bit(row, col) == 0L) continue
                val inset = unit * 0.08f
                val topLeft = Offset(col * unit + inset, row * unit + inset)
                val cellSize = Size(unit - inset * 2, unit - inset * 2)

                drawRoundRectCompat(
                    highlight.accent.copy(alpha = 0.22f * pulse),
                    Offset(topLeft.x - unit * 0.12f, topLeft.y - unit * 0.12f),
                    Size(cellSize.width + unit * 0.24f, cellSize.height + unit * 0.24f),
                    radius * 1.6f
                )
                drawRoundRectCompat(highlight.accent, topLeft, cellSize, radius)
                drawRoundRectCompat(
                    Color.White.copy(alpha = 0.35f),
                    topLeft,
                    Size(cellSize.width, cellSize.height * 0.28f),
                    radius * 0.6f
                )
            }
        }
    }
}

private fun DrawScope.drawRoundRectCompat(
    color: Color,
    topLeft: Offset,
    size: Size,
    radius: Float
) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
}

/** Compact non-interactive board used inside history rows and alternative cards. */
@Composable
fun BoardThumbnail(
    bits: Long,
    modifier: Modifier = Modifier,
    highlight: BoardHighlight = BoardHighlight()
) {
    BoardGrid(
        bits = bits,
        modifier = modifier,
        highlight = highlight,
        animateHighlight = false
    )
}

internal val ThumbnailSize = 64.dp
