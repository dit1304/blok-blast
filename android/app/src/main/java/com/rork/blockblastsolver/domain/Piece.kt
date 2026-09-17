package com.rork.blockblastsolver.domain

/** A single cell offset inside a piece, relative to the piece's top-left bounding box corner. */
data class PieceCell(val row: Int, val col: Int)

/**
 * A tray piece in its exact orientation. Block Blast never lets the player rotate a piece,
 * so every orientation is catalogued as its own selectable piece.
 */
data class Piece(
    val id: String,
    val family: String,
    val cells: List<PieceCell>
) {
    val width: Int = (cells.maxOfOrNull { it.col } ?: 0) + 1
    val height: Int = (cells.maxOfOrNull { it.row } ?: 0) + 1
    val size: Int = cells.size
}

private fun normalize(cells: List<PieceCell>): List<PieceCell> {
    val minRow = cells.minOf { it.row }
    val minCol = cells.minOf { it.col }
    return cells
        .map { PieceCell(it.row - minRow, it.col - minCol) }
        .sortedWith(compareBy({ it.row }, { it.col }))
}

private fun rotate(cells: List<PieceCell>): List<PieceCell> =
    normalize(cells.map { PieceCell(it.col, -it.row) })

private fun rotations(family: String, base: List<PieceCell>): List<Piece> {
    val seen = mutableListOf<List<PieceCell>>()
    var current = normalize(base)
    repeat(4) {
        if (seen.none { it == current }) seen.add(current)
        current = rotate(current)
    }
    return seen.mapIndexed { index, cells -> Piece("$family-$index", family, cells) }
}

private fun rect(rows: Int, cols: Int): List<PieceCell> =
    (0 until rows).flatMap { r -> (0 until cols).map { c -> PieceCell(r, c) } }

/** Every piece shape that can appear in the Block Blast tray, expanded to all orientations. */
object PieceCatalog {

    private val families: List<Pair<String, List<PieceCell>>> = listOf(
        "dot" to rect(1, 1),
        "line2" to rect(1, 2),
        "line3" to rect(1, 3),
        "line4" to rect(1, 4),
        "line5" to rect(1, 5),
        "square2" to rect(2, 2),
        "square3" to rect(3, 3),
        "rect23" to rect(2, 3),
        "corner3" to listOf(PieceCell(0, 0), PieceCell(1, 0), PieceCell(1, 1)),
        "corner5" to listOf(
            PieceCell(0, 0), PieceCell(1, 0), PieceCell(2, 0),
            PieceCell(2, 1), PieceCell(2, 2)
        ),
        "lshape4" to listOf(PieceCell(0, 0), PieceCell(1, 0), PieceCell(2, 0), PieceCell(2, 1)),
        "jshape4" to listOf(PieceCell(0, 1), PieceCell(1, 1), PieceCell(2, 1), PieceCell(2, 0)),
        "tshape4" to listOf(PieceCell(0, 0), PieceCell(0, 1), PieceCell(0, 2), PieceCell(1, 1)),
        "sshape4" to listOf(PieceCell(0, 1), PieceCell(0, 2), PieceCell(1, 0), PieceCell(1, 1)),
        "zshape4" to listOf(PieceCell(0, 0), PieceCell(0, 1), PieceCell(1, 1), PieceCell(1, 2)),
        "diag2" to listOf(PieceCell(0, 0), PieceCell(1, 1)),
        "diag3" to listOf(PieceCell(0, 0), PieceCell(1, 1), PieceCell(2, 2))
    )

    /** Display order groups small pieces first so the picker reads predictably. */
    val all: List<Piece> = families
        .flatMap { (family, cells) -> rotations(family, cells) }
        .sortedWith(compareBy({ it.size }, { it.family }, { it.id }))

    private val byId: Map<String, Piece> = all.associateBy { it.id }

    fun find(id: String): Piece? = byId[id]

    fun label(piece: Piece): String = when (piece.family) {
        "dot" -> "1 blok"
        "line2" -> "Garis 2"
        "line3" -> "Garis 3"
        "line4" -> "Garis 4"
        "line5" -> "Garis 5"
        "square2" -> "Kotak 2x2"
        "square3" -> "Kotak 3x3"
        "rect23" -> "Balok 2x3"
        "corner3" -> "Sudut kecil"
        "corner5" -> "Sudut besar"
        "lshape4" -> "Bentuk L"
        "jshape4" -> "Bentuk J"
        "tshape4" -> "Bentuk T"
        "sshape4" -> "Bentuk S"
        "zshape4" -> "Bentuk Z"
        "diag2" -> "Diagonal 2"
        "diag3" -> "Diagonal 3"
        else -> "Blok"
    }
}
