package com.rork.blockblastsolver.data

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.rork.blockblastsolver.domain.BOARD_SIZE
import com.rork.blockblastsolver.domain.BoardOps
import com.rork.blockblastsolver.domain.Piece
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.domain.PieceCell
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Outcome of analysing a photo or screenshot of the game. */
data class DetectionResult(
    val boardBits: Long,
    val pieces: List<Piece>,
    /** 0f..1f confidence that the 8x8 grid was located correctly. */
    val confidence: Float,
    val trayDetected: Boolean
)

/**
 * Locates the 8x8 Block Blast board inside a photo and reads which cells are filled,
 * then reads the three tray pieces from the strip underneath the board.
 *
 * The detector is intentionally conservative: it returns a confidence value and the user
 * always confirms or fixes the reading on the review screen before solving.
 */
object BoardDetector {

    private const val TAG = "BoardDetector"
    private const val WORK_WIDTH = 420

    fun detect(source: Bitmap): DetectionResult {
        val bitmap = scaleForAnalysis(source)
        val gray = toLuma(bitmap)
        val width = bitmap.width
        val height = bitmap.height

        val board = findBoard(gray, width, height)
            ?: return DetectionResult(0L, emptyList(), 0f, false)

        val bits = readBoardBits(gray, width, board)
        val unit = board.size.toFloat() / BOARD_SIZE
        val pieces = readTray(gray, width, height, board, unit)

        return DetectionResult(
            boardBits = bits,
            pieces = pieces,
            confidence = board.confidence,
            trayDetected = pieces.isNotEmpty()
        )
    }

    private data class BoardBox(val left: Int, val top: Int, val size: Int, val confidence: Float)

    private fun scaleForAnalysis(source: Bitmap): Bitmap {
        if (source.width <= WORK_WIDTH) return source
        val ratio = WORK_WIDTH.toFloat() / source.width
        val height = max(1, (source.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(source, WORK_WIDTH, height, true)
    }

    private fun toLuma(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            (Color.red(pixel) * 77 + Color.green(pixel) * 151 + Color.blue(pixel) * 28) shr 8
        }
    }

    private fun luma(gray: IntArray, width: Int, x: Int, y: Int): Int {
        val index = y * width + x
        return if (index in gray.indices) gray[index] else 0
    }

    /**
     * Searches square candidates for the one whose 8x8 subdivision looks most like a
     * Block Blast grid: cell interiors that are flat, and a strongly bimodal spread
     * between empty and filled cells.
     */
    private fun findBoard(gray: IntArray, width: Int, height: Int): BoardBox? {
        var best: BoardBox? = null
        var bestScore = Float.NEGATIVE_INFINITY

        val minSize = (min(width, height) * 0.45f).roundToInt()
        val maxSize = min(width, height)
        val sizeStep = max(4, (maxSize - minSize) / 14)
        val posStep = max(3, width / 40)

        var size = maxSize
        while (size >= minSize) {
            var top = 0
            while (top + size <= height) {
                var left = 0
                while (left + size <= width) {
                    val score = scoreCandidate(gray, width, left, top, size)
                    if (score > bestScore) {
                        bestScore = score
                        best = BoardBox(left, top, size, 0f)
                    }
                    left += posStep
                }
                top += posStep
            }
            size -= sizeStep
        }

        val candidate = best ?: return null
        val refined = refine(gray, width, height, candidate, bestScore)
        val confidence = (refined.second / 120f).coerceIn(0f, 1f)
        Log.d(TAG, "board candidate ${refined.first} score=${refined.second} confidence=$confidence")
        return refined.first.copy(confidence = confidence)
    }

    /** Local hill-climb around the coarse candidate for a tighter fit. */
    private fun refine(
        gray: IntArray,
        width: Int,
        height: Int,
        coarse: BoardBox,
        coarseScore: Float
    ): Pair<BoardBox, Float> {
        var best = coarse
        var bestScore = coarseScore
        var step = max(2, coarse.size / 24)

        while (step >= 1) {
            var improved = false
            val deltas = listOf(
                Triple(-step, 0, 0), Triple(step, 0, 0),
                Triple(0, -step, 0), Triple(0, step, 0),
                Triple(0, 0, -step), Triple(0, 0, step),
                Triple(-step, -step, step * 2), Triple(step, step, -step * 2)
            )
            for ((dx, dy, ds) in deltas) {
                val left = best.left + dx
                val top = best.top + dy
                val size = best.size + ds
                if (left < 0 || top < 0 || size < 24) continue
                if (left + size > width || top + size > height) continue
                val score = scoreCandidate(gray, width, left, top, size)
                if (score > bestScore) {
                    bestScore = score
                    best = BoardBox(left, top, size, 0f)
                    improved = true
                }
            }
            if (!improved) step /= 2
        }
        return best to bestScore
    }

    private fun scoreCandidate(gray: IntArray, width: Int, left: Int, top: Int, size: Int): Float {
        val unit = size.toFloat() / BOARD_SIZE
        if (unit < 4f) return Float.NEGATIVE_INFINITY

        val means = FloatArray(BOARD_SIZE * BOARD_SIZE)
        var withinVariance = 0f

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cx = left + unit * (col + 0.5f)
                val cy = top + unit * (row + 0.5f)
                val inset = (unit * 0.24f).coerceAtLeast(1f)
                var sum = 0f
                var sumSq = 0f
                var count = 0
                val offsets = floatArrayOf(-inset, 0f, inset)
                for (ox in offsets) {
                    for (oy in offsets) {
                        val value = luma(gray, width, (cx + ox).roundToInt(), (cy + oy).roundToInt()).toFloat()
                        sum += value
                        sumSq += value * value
                        count++
                    }
                }
                val mean = sum / count
                means[row * BOARD_SIZE + col] = mean
                withinVariance += (sumSq / count - mean * mean).coerceAtLeast(0f)
            }
        }

        val globalMean = means.average().toFloat()
        var betweenVariance = 0f
        for (mean in means) {
            val diff = mean - globalMean
            betweenVariance += diff * diff
        }
        betweenVariance /= means.size
        withinVariance /= means.size

        // A real grid also has visible separator lines; sample the seams between cells.
        var seamContrast = 0f
        for (i in 1 until BOARD_SIZE) {
            val seam = unit * i
            var seamSum = 0f
            var interiorSum = 0f
            var samples = 0
            for (j in 0 until BOARD_SIZE) {
                val along = unit * (j + 0.5f)
                seamSum += luma(gray, width, (left + seam).roundToInt(), (top + along).roundToInt())
                seamSum += luma(gray, width, (left + along).roundToInt(), (top + seam).roundToInt())
                interiorSum += luma(gray, width, (left + seam - unit * 0.35f).roundToInt(), (top + along).roundToInt())
                interiorSum += luma(gray, width, (left + along).roundToInt(), (top + seam - unit * 0.35f).roundToInt())
                samples += 2
            }
            seamContrast += abs(seamSum - interiorSum) / samples
        }
        seamContrast /= (BOARD_SIZE - 1)

        return betweenVariance / 40f - withinVariance / 60f + seamContrast * 1.4f
    }

    private fun readBoardBits(gray: IntArray, width: Int, board: BoardBox): Long {
        val unit = board.size.toFloat() / BOARD_SIZE
        val means = FloatArray(BOARD_SIZE * BOARD_SIZE)
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                means[row * BOARD_SIZE + col] = cellMean(gray, width, board.left, board.top, unit, row, col)
            }
        }
        val threshold = otsuThreshold(means)
        var bits = 0L
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (means[row * BOARD_SIZE + col] > threshold) bits = bits or BoardOps.bit(row, col)
            }
        }
        // Filled blocks are brighter than the empty board in every Block Blast skin.
        // If the "filled" set covers almost everything the split is meaningless; treat as empty.
        val filled = BoardOps.filledCount(bits)
        return if (filled >= 62) 0L else bits
    }

    private fun cellMean(
        gray: IntArray,
        width: Int,
        left: Int,
        top: Int,
        unit: Float,
        row: Int,
        col: Int
    ): Float {
        val cx = left + unit * (col + 0.5f)
        val cy = top + unit * (row + 0.5f)
        val inset = (unit * 0.26f).coerceAtLeast(1f)
        var sum = 0f
        var count = 0
        val offsets = floatArrayOf(-inset, 0f, inset)
        for (ox in offsets) {
            for (oy in offsets) {
                sum += luma(gray, width, (cx + ox).roundToInt(), (cy + oy).roundToInt())
                count++
            }
        }
        return sum / count
    }

    private fun otsuThreshold(values: FloatArray): Float {
        val minValue = values.min()
        val maxValue = values.max()
        if (maxValue - minValue < 16f) return maxValue + 1f

        var bestThreshold = (minValue + maxValue) / 2f
        var bestVariance = -1f
        var candidate = minValue
        val step = (maxValue - minValue) / 48f
        while (candidate < maxValue) {
            val low = values.filter { it <= candidate }
            val high = values.filter { it > candidate }
            if (low.isNotEmpty() && high.isNotEmpty()) {
                val weightLow = low.size.toFloat() / values.size
                val weightHigh = 1f - weightLow
                val diff = high.average().toFloat() - low.average().toFloat()
                val variance = weightLow * weightHigh * diff * diff
                if (variance > bestVariance) {
                    bestVariance = variance
                    bestThreshold = candidate
                }
            }
            candidate += step
        }
        return bestThreshold
    }

    /**
     * Reads the tray under the board. Bright pixels in the strip are grouped into connected
     * components, each component is rasterised onto a grid of the tray's own block size and
     * matched against the piece catalogue.
     */
    private fun readTray(
        gray: IntArray,
        width: Int,
        height: Int,
        board: BoardBox,
        boardUnit: Float
    ): List<Piece> {
        val trayTop = (board.top + board.size + boardUnit * 0.25f).roundToInt()
        val trayBottom = min(height - 1, (trayTop + boardUnit * 3.4f).roundToInt())
        if (trayBottom - trayTop < 8) return emptyList()

        val trayWidth = width
        val trayHeight = trayBottom - trayTop
        val samples = FloatArray(trayWidth * trayHeight)
        for (y in 0 until trayHeight) {
            for (x in 0 until trayWidth) {
                samples[y * trayWidth + x] = luma(gray, width, x, trayTop + y).toFloat()
            }
        }
        val threshold = otsuThreshold(samples)
        val mask = BooleanArray(samples.size) { samples[it] > threshold + 6f }

        val components = connectedComponents(mask, trayWidth, trayHeight)
        if (components.isEmpty()) return emptyList()

        val unit = estimateTrayUnit(components, boardUnit)
        return components
            .filter { it.area > (unit * unit * 0.35f) }
            .sortedBy { it.minX }
            .take(3)
            .mapNotNull { component -> matchPiece(component, mask, trayWidth, unit) }
    }

    private data class Component(
        val pixels: MutableList<Int> = mutableListOf(),
        var minX: Int = Int.MAX_VALUE,
        var maxX: Int = Int.MIN_VALUE,
        var minY: Int = Int.MAX_VALUE,
        var maxY: Int = Int.MIN_VALUE
    ) {
        val area: Int get() = pixels.size
        val boxWidth: Int get() = maxX - minX + 1
        val boxHeight: Int get() = maxY - minY + 1
    }

    private fun connectedComponents(mask: BooleanArray, width: Int, height: Int): List<Component> {
        val visited = BooleanArray(mask.size)
        val result = mutableListOf<Component>()
        val stack = ArrayDeque<Int>()

        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            val component = Component()
            stack.addLast(start)
            visited[start] = true

            while (stack.isNotEmpty()) {
                val index = stack.removeLast()
                val x = index % width
                val y = index / width
                component.pixels.add(index)
                component.minX = min(component.minX, x)
                component.maxX = max(component.maxX, x)
                component.minY = min(component.minY, y)
                component.maxY = max(component.maxY, y)

                if (x > 0) pushIf(mask, visited, stack, index - 1)
                if (x < width - 1) pushIf(mask, visited, stack, index + 1)
                if (y > 0) pushIf(mask, visited, stack, index - width)
                if (y < height - 1) pushIf(mask, visited, stack, index + width)
            }
            result.add(component)
        }
        return result.filter { it.area > 12 }
    }

    private fun pushIf(mask: BooleanArray, visited: BooleanArray, stack: ArrayDeque<Int>, index: Int) {
        if (index in mask.indices && mask[index] && !visited[index]) {
            visited[index] = true
            stack.addLast(index)
        }
    }

    /** Tray blocks are drawn smaller than board cells; derive the unit from the components. */
    private fun estimateTrayUnit(components: List<Component>, boardUnit: Float): Float {
        val smallest = components
            .map { min(it.boxWidth, it.boxHeight).toFloat() }
            .filter { it > 4f }
            .minOrNull()
        val estimate = smallest ?: (boardUnit * 0.7f)
        return estimate.coerceIn(boardUnit * 0.35f, boardUnit * 1.1f)
    }

    private fun matchPiece(
        component: Component,
        mask: BooleanArray,
        width: Int,
        unit: Float
    ): Piece? {
        val cols = (component.boxWidth / unit).roundToInt().coerceIn(1, 5)
        val rows = (component.boxHeight / unit).roundToInt().coerceIn(1, 5)
        val cellWidth = component.boxWidth.toFloat() / cols
        val cellHeight = component.boxHeight.toFloat() / rows

        val cells = mutableListOf<PieceCell>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = component.minX + cellWidth * (col + 0.5f)
                val cy = component.minY + cellHeight * (row + 0.5f)
                var hits = 0
                var total = 0
                val stepX = cellWidth * 0.22f
                val stepY = cellHeight * 0.22f
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val x = (cx + dx * stepX).roundToInt()
                        val y = (cy + dy * stepY).roundToInt()
                        val index = y * width + x
                        if (index in mask.indices) {
                            total++
                            if (mask[index]) hits++
                        }
                    }
                }
                if (total > 0 && hits.toFloat() / total >= 0.5f) cells.add(PieceCell(row, col))
            }
        }
        if (cells.isEmpty()) return null

        val normalizedRows = cells.minOf { it.row }
        val normalizedCols = cells.minOf { it.col }
        val normalized = cells
            .map { PieceCell(it.row - normalizedRows, it.col - normalizedCols) }
            .sortedWith(compareBy({ it.row }, { it.col }))

        return PieceCatalog.all.firstOrNull { candidate ->
            candidate.cells.sortedWith(compareBy({ it.row }, { it.col })) == normalized
        }
    }
}
