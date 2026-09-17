package com.rork.blockblastsolver.data

import kotlinx.serialization.Serializable

/** A saved turn: the board that was scanned, the tray pieces, and the move the user took. */
@Serializable
data class ScanRecord(
    val id: String,
    val timestampMillis: Long,
    val boardBits: Long,
    val pieceIds: List<String>,
    val resultBoardBits: Long,
    val gainedScore: Int,
    val linesCleared: Int,
    val combo: Int,
    val source: String
)

@Serializable
data class HistoryPayload(
    val records: List<ScanRecord> = emptyList()
)
