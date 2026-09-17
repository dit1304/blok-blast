package com.rork.blockblastsolver.domain

import kotlin.math.abs

/** One piece dropped at one anchor position. */
data class Placement(
    val piece: Piece,
    val row: Int,
    val col: Int,
    val mask: Long,
    /** Rows cleared by this specific placement, evaluated in move order. */
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    /** Board bits after the piece landed but before its lines were wiped. */
    val boardBeforeClear: Long,
    /** Board bits after the clear resolved. */
    val boardAfterClear: Long,
    val gainedScore: Int,
    val comboAtStep: Int
)

/** A full plan: every tray piece placed in a chosen order. */
data class MoveSolution(
    val placements: List<Placement>,
    val startBoard: Long,
    val finalBoard: Long,
    val totalScore: Int,
    val totalLinesCleared: Int,
    val maxCombo: Int,
    /** Heuristic quality score used for ranking; higher is better. */
    val rank: Double,
    val survivesNextWave: Boolean
) {
    val clearsLines: Boolean get() = totalLinesCleared > 0
}

/**
 * Exhaustive-with-pruning solver for a Block Blast turn.
 *
 * Block Blast gives the player three pieces, all of which must be placed before the next wave
 * arrives, and the player picks the order. The solver searches every order and every anchor,
 * scores each complete plan with the real game scoring model plus board-health heuristics,
 * and returns the best distinct plans.
 */
object Solver {

    private const val POINTS_PER_CELL = 1
    private const val POINTS_PER_LINE = 10
    private const val MAX_RESULTS = 6

    /**
     * Safety budget for the search. An almost-empty board with three 1x1 pieces would otherwise
     * expand to a quarter of a million branches; the budget keeps solving well under a second
     * while still exploring far more than a human ever could.
     */
    private const val MAX_NODES = 120_000

    /** Candidate plans kept in memory before ranking; plenty for picking the top six. */
    private const val MAX_CANDIDATES = 4_000

    private class SearchContext(val startBoard: Long) {
        val results = ArrayList<MoveSolution>(256)
        /** Visited (board, remaining-pieces) states, so permutations converging on the same state are explored once. */
        val visited = HashSet<Long>()
        var nodes = 0

        fun budgetExhausted(): Boolean = nodes >= MAX_NODES || results.size >= MAX_CANDIDATES
    }

    /** Scoring for clearing [lines] simultaneously at combo multiplier [combo]. */
    private fun lineScore(lines: Int, combo: Int): Int {
        if (lines <= 0) return 0
        val base = lines * POINTS_PER_LINE * BOARD_SIZE
        val simultaneousBonus = if (lines > 1) (lines - 1) * POINTS_PER_LINE * BOARD_SIZE / 2 else 0
        return (base + simultaneousBonus) * combo
    }

    /**
     * Solves a turn.
     *
     * @param board current filled cells as a 64-bit board
     * @param pieces the tray pieces, 1..3 of them
     * @param startingCombo combo multiplier carried into this turn (1 when the streak is broken)
     */
    fun solve(board: Long, pieces: List<Piece>, startingCombo: Int = 1): List<MoveSolution> {
        if (pieces.isEmpty()) return emptyList()
        val context = SearchContext(startBoard = board)
        val used = BooleanArray(pieces.size)
        val chosen = ArrayList<Placement>(pieces.size)

        search(
            board = board,
            pieces = pieces,
            used = used,
            depth = 0,
            combo = startingCombo.coerceAtLeast(1),
            scoreSoFar = 0,
            chosen = chosen,
            context = context
        )

        if (context.results.isEmpty()) return emptyList()

        return context.results
            .sortedByDescending { it.rank }
            .distinctBy { solution ->
                solution.placements.map { "${it.piece.id}@${it.row},${it.col}" }.sorted()
            }
            .take(MAX_RESULTS)
    }

    private fun search(
        board: Long,
        pieces: List<Piece>,
        used: BooleanArray,
        depth: Int,
        combo: Int,
        scoreSoFar: Int,
        chosen: ArrayList<Placement>,
        context: SearchContext
    ) {
        if (depth == pieces.size) {
            context.results.add(buildSolution(chosen.toList(), context.startBoard, board, scoreSoFar))
            return
        }
        if (context.budgetExhausted()) return

        var placedAny = false
        val triedAtThisDepth = HashSet<String>()
        for (index in pieces.indices) {
            if (used[index]) continue
            if (context.budgetExhausted()) break
            val piece = pieces[index]
            // Identical tray pieces produce identical subtrees; explore each shape once per depth.
            if (!triedAtThisDepth.add(piece.id)) continue

            used[index] = true

            for (row in 0..BOARD_SIZE - piece.height) {
                if (context.budgetExhausted()) break
                for (col in 0..BOARD_SIZE - piece.width) {
                    if (context.budgetExhausted()) break
                    val mask = BoardOps.maskFor(piece, row, col) ?: continue
                    if (!BoardOps.canPlace(board, mask)) continue

                    context.nodes++
                    val boardBeforeClear = board or mask
                    val (rows, cols) = BoardOps.clearedLines(boardBeforeClear)
                    val lines = rows.size + cols.size
                    val stepCombo = if (lines > 0) combo else 1
                    val cleared = if (lines > 0) BoardOps.clearMask(rows, cols) else 0L
                    val boardAfter = boardBeforeClear and cleared.inv()
                    val gained = piece.size * POINTS_PER_CELL + lineScore(lines, stepCombo)

                    chosen.add(
                        Placement(
                            piece = piece,
                            row = row,
                            col = col,
                            mask = mask,
                            clearedRows = rows,
                            clearedCols = cols,
                            boardBeforeClear = boardBeforeClear,
                            boardAfterClear = boardAfter,
                            gainedScore = gained,
                            comboAtStep = stepCombo
                        )
                    )
                    placedAny = true

                    // Two different orders can reach an identical board with the same pieces left;
                    // the remaining subtree is then identical, so explore it only once.
                    val remainingKey = remainingSignature(pieces, used)
                    val stateKey = boardAfter * 31L + remainingKey
                    if (context.visited.add(stateKey)) {
                        search(
                            board = boardAfter,
                            pieces = pieces,
                            used = used,
                            depth = depth + 1,
                            combo = if (lines > 0) stepCombo + 1 else 1,
                            scoreSoFar = scoreSoFar + gained,
                            chosen = chosen,
                            context = context
                        )
                    }

                    chosen.removeAt(chosen.size - 1)
                }
            }

            used[index] = false
        }

        // Dead end: the remaining pieces do not fit. Record the partial plan so the user still
        // sees the best possible salvage instead of an empty screen.
        if (!placedAny && chosen.isNotEmpty()) {
            context.results.add(buildSolution(chosen.toList(), context.startBoard, board, scoreSoFar))
        }
    }

    /** Bitmask of which tray slots are still unplaced, used as part of the memo key. */
    private fun remainingSignature(pieces: List<Piece>, used: BooleanArray): Long {
        var signature = 0L
        for (index in pieces.indices) {
            if (!used[index]) signature = signature or (1L shl index)
        }
        return signature
    }

    private fun buildSolution(
        placements: List<Placement>,
        startBoard: Long,
        finalBoard: Long,
        totalScore: Int
    ): MoveSolution {
        val lines = placements.sumOf { it.clearedRows.size + it.clearedCols.size }
        val maxCombo = placements.filter { it.clearedRows.isNotEmpty() || it.clearedCols.isNotEmpty() }
            .maxOfOrNull { it.comboAtStep } ?: 1
        val filled = BoardOps.filledCount(finalBoard)
        val openness = boardHealth(finalBoard)
        val survives = BoardOps.hasAnyPlacement(finalBoard, SURVIVAL_PROBES)

        // Rank blends real score with board health: a big clear that leaves a hostile board is a trap.
        val rank = totalScore * 1.0 +
            openness * 6.0 -
            filled * 1.6 +
            (if (survives) 40.0 else -220.0) +
            lines * 12.0

        return MoveSolution(
            placements = placements,
            startBoard = startBoard,
            finalBoard = finalBoard,
            totalScore = totalScore,
            totalLinesCleared = lines,
            maxCombo = maxCombo,
            rank = rank,
            survivesNextWave = survives
        )
    }

    /** Pieces used to test whether a resulting board can still accept typical next-wave shapes. */
    private val SURVIVAL_PROBES: List<Piece> = listOfNotNull(
        PieceCatalog.all.firstOrNull { it.family == "square2" },
        PieceCatalog.all.firstOrNull { it.family == "line4" && it.width == 4 },
        PieceCatalog.all.firstOrNull { it.family == "line4" && it.height == 4 },
        PieceCatalog.all.firstOrNull { it.family == "tshape4" }
    )

    /**
     * Board health heuristic. Rewards large contiguous empty regions and near-complete lines,
     * penalises isolated single holes that nothing can ever fill.
     */
    private fun boardHealth(bits: Long): Double {
        var score = 0.0
        var isolatedHoles = 0

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (BoardOps.isFilled(bits, row, col)) continue
                var freeNeighbours = 0
                if (row > 0 && !BoardOps.isFilled(bits, row - 1, col)) freeNeighbours++
                if (row < BOARD_SIZE - 1 && !BoardOps.isFilled(bits, row + 1, col)) freeNeighbours++
                if (col > 0 && !BoardOps.isFilled(bits, row, col - 1)) freeNeighbours++
                if (col < BOARD_SIZE - 1 && !BoardOps.isFilled(bits, row, col + 1)) freeNeighbours++
                if (freeNeighbours == 0) isolatedHoles++
                score += freeNeighbours * 0.5
            }
        }

        // Nearly-complete lines are valuable setup for the next turn.
        for (i in 0 until BOARD_SIZE) {
            val rowFilled = java.lang.Long.bitCount(bits and BoardOps.rowMasks[i])
            val colFilled = java.lang.Long.bitCount(bits and BoardOps.colMasks[i])
            if (rowFilled in 6..7) score += (rowFilled - 5) * 2.5
            if (colFilled in 6..7) score += (colFilled - 5) * 2.5
        }

        // Prefer a balanced spread rather than one crowded half of the board.
        val leftHalf = (0 until BOARD_SIZE).sumOf { r ->
            (0 until BOARD_SIZE / 2).count { c -> BoardOps.isFilled(bits, r, c) }
        }
        val rightHalf = BoardOps.filledCount(bits) - leftHalf
        score -= abs(leftHalf - rightHalf) * 0.4

        return score - isolatedHoles * 9.0
    }

    /** Short human-readable summary used on cards and history rows. */
    fun describe(solution: MoveSolution): String = when {
        solution.totalLinesCleared >= 3 -> "Hapus ${solution.totalLinesCleared} baris sekaligus"
        solution.totalLinesCleared == 2 -> "Hapus 2 baris"
        solution.totalLinesCleared == 1 -> "Hapus 1 baris"
        solution.survivesNextWave -> "Aman, tanpa hapus baris"
        else -> "Papan sempit, main hati-hati"
    }
}
