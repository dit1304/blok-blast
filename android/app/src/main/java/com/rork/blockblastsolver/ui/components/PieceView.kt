package com.rork.blockblastsolver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.domain.Piece
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.ui.theme.GridFilled
import com.rork.blockblastsolver.ui.theme.GridFilledEdge
import kotlin.math.max

/** Draws a tray piece at its natural aspect ratio inside a square box of [box]. */
@Composable
fun PieceView(
    piece: Piece,
    modifier: Modifier = Modifier,
    box: Dp = 44.dp,
    color: Color = GridFilled
) {
    Canvas(
        modifier = modifier
            .size(box)
            .semantics { contentDescription = PieceCatalog.label(piece) }
    ) {
        val span = max(piece.width, piece.height)
        val unit = size.minDimension / span
        val offsetX = (size.width - unit * piece.width) / 2f
        val offsetY = (size.height - unit * piece.height) / 2f
        val gap = unit * 0.12f
        val radius = CornerRadius(unit * 0.18f, unit * 0.18f)

        for (cell in piece.cells) {
            val topLeft = Offset(
                offsetX + cell.col * unit + gap / 2f,
                offsetY + cell.row * unit + gap / 2f
            )
            val cellSize = Size(unit - gap, unit - gap)
            drawRoundRect(color = color, topLeft = topLeft, size = cellSize, cornerRadius = radius)
            drawRoundRect(
                color = GridFilledEdge.copy(alpha = 0.55f),
                topLeft = Offset(topLeft.x, topLeft.y + cellSize.height * 0.7f),
                size = Size(cellSize.width, cellSize.height * 0.3f),
                cornerRadius = CornerRadius(radius.x * 0.6f, radius.y * 0.6f)
            )
        }
    }
}
