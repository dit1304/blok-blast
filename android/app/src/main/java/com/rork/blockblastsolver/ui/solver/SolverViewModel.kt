package com.rork.blockblastsolver.ui.solver

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.blockblastsolver.data.BoardDetector
import com.rork.blockblastsolver.data.ScanRecord
import com.rork.blockblastsolver.data.ServiceLocator
import com.rork.blockblastsolver.domain.BoardOps
import com.rork.blockblastsolver.domain.MoveSolution
import com.rork.blockblastsolver.domain.Piece
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.domain.Solver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** How the current board reached the app; kept for history badges. */
enum class BoardSource(val label: String, val storageKey: String) {
    CAMERA("Kamera", "camera"),
    IMPORT("Impor gambar", "import"),
    MANUAL("Input manual", "manual"),
    HISTORY("Dari riwayat", "history")
}

data class SolverUiState(
    val boardBits: Long = 0L,
    val pieceSlots: List<Piece?> = listOf(null, null, null),
    val startingCombo: Int = 1,
    val source: BoardSource = BoardSource.MANUAL,
    val detectionConfidence: Float? = null,
    val detectionNote: String? = null,
    val isAnalyzing: Boolean = false,
    val isSolving: Boolean = false,
    val solutions: List<MoveSolution> = emptyList(),
    val selectedIndex: Int = 0,
    val stepIndex: Int = 0,
    val errorMessage: String? = null
) {
    val pieces: List<Piece> get() = pieceSlots.filterNotNull()
    val selectedSolution: MoveSolution? get() = solutions.getOrNull(selectedIndex)
    val canSolve: Boolean get() = pieces.isNotEmpty() && BoardOps.filledCount(boardBits) < 64
}

sealed interface SolverEvent {
    data object DetectionFinished : SolverEvent
    data object SolutionReady : SolverEvent
    data class Message(val text: String) : SolverEvent
}

/**
 * Owns one solving session: the board being reviewed, the tray pieces, the ranked plans,
 * and the step the user is currently following.
 */
class SolverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.history(application)

    private val _state = MutableStateFlow(SolverUiState())
    val state: StateFlow<SolverUiState> = _state.asStateFlow()

    private val _events = Channel<SolverEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun analyze(bitmap: Bitmap, source: BoardSource) {
        _state.value = _state.value.copy(isAnalyzing = true, errorMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) { BoardDetector.detect(bitmap) }
            }.onFailure { Log.w(TAG, "Detection failed: ${it.message}") }
                .getOrNull()

            if (result == null || result.confidence <= 0.08f) {
                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    source = source,
                    detectionConfidence = result?.confidence ?: 0f,
                    detectionNote = "Papan tidak terbaca jelas. Perbaiki manual di bawah."
                )
                _events.send(SolverEvent.DetectionFinished)
                return@launch
            }

            val slots = MutableList<Piece?>(3) { null }
            result.pieces.take(3).forEachIndexed { index, piece -> slots[index] = piece }

            _state.value = _state.value.copy(
                boardBits = result.boardBits,
                pieceSlots = slots,
                source = source,
                isAnalyzing = false,
                detectionConfidence = result.confidence,
                detectionNote = when {
                    !result.trayDetected -> "Papan terbaca, blok belum terdeteksi. Pilih blok manual."
                    result.confidence < 0.4f -> "Hasil baca kurang yakin. Cek ulang kotak yang terisi."
                    else -> "Papan & blok terbaca. Cek sebentar lalu hitung."
                },
                solutions = emptyList(),
                selectedIndex = 0,
                stepIndex = 0
            )
            _events.send(SolverEvent.DetectionFinished)
        }
    }

    fun startManualSession() {
        _state.value = SolverUiState(source = BoardSource.MANUAL, detectionNote = "Ketuk kotak untuk menandai papan.")
    }

    fun toggleCell(row: Int, col: Int) {
        val current = _state.value
        _state.value = current.copy(
            boardBits = BoardOps.toggle(current.boardBits, row, col),
            solutions = emptyList(),
            selectedIndex = 0,
            stepIndex = 0
        )
    }

    fun clearBoard() {
        _state.value = _state.value.copy(boardBits = 0L, solutions = emptyList(), stepIndex = 0)
    }

    fun setPiece(slot: Int, pieceId: String?) {
        val current = _state.value
        if (slot !in current.pieceSlots.indices) return
        val slots = current.pieceSlots.toMutableList()
        slots[slot] = pieceId?.let { PieceCatalog.find(it) }
        _state.value = current.copy(
            pieceSlots = slots,
            solutions = emptyList(),
            selectedIndex = 0,
            stepIndex = 0
        )
    }

    fun setStartingCombo(combo: Int) {
        _state.value = _state.value.copy(
            startingCombo = combo.coerceIn(1, 9),
            solutions = emptyList(),
            stepIndex = 0
        )
    }

    fun solve() {
        val current = _state.value
        if (!current.canSolve) {
            viewModelScope.launch { _events.send(SolverEvent.Message("Pilih minimal satu blok dulu.")) }
            return
        }
        _state.value = current.copy(isSolving = true, errorMessage = null)
        viewModelScope.launch {
            val solutions = withContext(Dispatchers.Default) {
                Solver.solve(current.boardBits, current.pieces, current.startingCombo)
            }
            if (solutions.isEmpty()) {
                _state.value = _state.value.copy(
                    isSolving = false,
                    errorMessage = "Tidak ada blok yang bisa masuk ke papan ini."
                )
                _events.send(SolverEvent.Message("Tidak ada langkah yang mungkin."))
                return@launch
            }
            _state.value = _state.value.copy(
                isSolving = false,
                solutions = solutions,
                selectedIndex = 0,
                stepIndex = 0
            )
            _events.send(SolverEvent.SolutionReady)
        }
    }

    fun selectSolution(index: Int) {
        if (index !in _state.value.solutions.indices) return
        _state.value = _state.value.copy(selectedIndex = index, stepIndex = 0)
    }

    fun setStep(step: Int) {
        val solution = _state.value.selectedSolution ?: return
        _state.value = _state.value.copy(stepIndex = step.coerceIn(0, solution.placements.lastIndex))
    }

    fun nextStep() = setStep(_state.value.stepIndex + 1)

    fun previousStep() = setStep(_state.value.stepIndex - 1)

    /** Saves the currently selected plan to history. */
    fun commitSelected() {
        val current = _state.value
        val solution = current.selectedSolution ?: return
        viewModelScope.launch {
            runCatching {
                repository.add(
                    ScanRecord(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = System.currentTimeMillis(),
                        boardBits = solution.startBoard,
                        pieceIds = solution.placements.map { it.piece.id },
                        resultBoardBits = solution.finalBoard,
                        gainedScore = solution.totalScore,
                        linesCleared = solution.totalLinesCleared,
                        combo = solution.maxCombo,
                        source = current.source.storageKey
                    )
                )
            }.onFailure { Log.w(TAG, "Failed to save record: ${it.message}") }

            // Chain into the next turn: keep the resulting board, clear the tray.
            _state.value = SolverUiState(
                boardBits = solution.finalBoard,
                startingCombo = if (solution.clearsLines) (solution.maxCombo + 1).coerceAtMost(9) else 1,
                source = BoardSource.MANUAL,
                detectionNote = "Langkah tersimpan. Papan lanjut dari hasil tadi."
            )
            _events.send(SolverEvent.Message("Langkah disimpan ke riwayat."))
        }
    }

    fun loadRecord(record: ScanRecord) {
        val slots = MutableList<Piece?>(3) { null }
        record.pieceIds.take(3).forEachIndexed { index, id -> slots[index] = PieceCatalog.find(id) }
        _state.value = SolverUiState(
            boardBits = record.boardBits,
            pieceSlots = slots,
            source = BoardSource.HISTORY,
            detectionNote = "Dimuat dari riwayat."
        )
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private companion object {
        const val TAG = "SolverViewModel"
    }
}
