package com.rork.blockblastsolver.domain

/** The Block Blast board is always 8x8, so it maps exactly onto the 64 bits of a Long. */
const val BOARD_SIZE = 8
const val BOARD_CELLS = BOARD_SIZE * BOARD_SIZE

object BoardOps {

    val rowMasks: LongArray = LongArray(BOARD_SIZE) { row ->
        var mask = 0L
        for (col in 0 until BOARD_SIZE) mask = mask or bit(row, col)
        mask
    }

    val colMasks: LongArray = LongArray(BOARD_SIZE) { col ->
        var mask = 0L
        for (row in 0 until BOARD_SIZE) mask = mask or bit(row, col)
        mask
    }

    fun bit(row: Int, col: Int): Long = 1L shl (row * BOARD_SIZE + col)

    fun isFilled(bits: Long, row: Int, col: Int): Boolean = bits and bit(row, col) != 0L

    fun toggle(bits: Long, row: Int, col: Int): Long = bits xor bit(row, col)

    fun filledCount(bits: Long): Int = java.lang.Long.bitCount(bits)

    /** Mask for [piece] anchored with its bounding box top-left at ([row], [col]), or null if out of bounds. */
    fun maskFor(piece: Piece, row: Int, col: Int): Long? {
        if (row < 0 || col < 0) return null
        if (row + piece.height > BOARD_SIZE || col + piece.width > BOARD_SIZE) return null
        var mask = 0L
        for (cell in piece.cells) mask = mask or bit(row + cell.row, col + cell.col)
        return mask
    }

    fun canPlace(bits: Long, mask: Long): Boolean = bits and mask == 0L

    /** Rows and columns that become complete once [mask] is added to [bits]. */
    fun clearedLines(bits: Long): Pair<List<Int>, List<Int>> {
        val rows = mutableListOf<Int>()
        val cols = mutableListOf<Int>()
        for (i in 0 until BOARD_SIZE) {
            if (bits and rowMasks[i] == rowMasks[i]) rows.add(i)
            if (bits and colMasks[i] == colMasks[i]) cols.add(i)
        }
        return rows to cols
    }

    fun clearMask(rows: List<Int>, cols: List<Int>): Long {
        var mask = 0L
        for (r in rows) mask = mask or rowMasks[r]
        for (c in cols) mask = mask or colMasks[c]
        return mask
    }

    /** True when at least one of [pieces] still fits somewhere on [bits]. */
    fun hasAnyPlacement(bits: Long, pieces: List<Piece>): Boolean = pieces.any { piece ->
        for (row in 0..BOARD_SIZE - piece.height) {
            for (col in 0..BOARD_SIZE - piece.width) {
                val mask = maskFor(piece, row, col) ?: continue
                if (canPlace(bits, mask)) return@any true
            }
        }
        false
    }
}
