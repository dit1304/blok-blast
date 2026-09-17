package com.rork.blockblastsolver.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.blockblastsolver.data.ScanRecord
import com.rork.blockblastsolver.data.ServiceLocator
import com.rork.blockblastsolver.domain.StatsCalculator
import com.rork.blockblastsolver.domain.StatsSummary
import com.rork.blockblastsolver.util.formatRelativeTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val query: String = "",
    val records: List<ScanRecord> = emptyList(),
    val isLoaded: Boolean = false
)

/** Backs both the Riwayat and Statistik tabs; both read the same persisted records. */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.history(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val allRecords: StateFlow<List<ScanRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<HistoryUiState> = combine(allRecords, _query) { records, query ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            records
        } else {
            records.filter { record ->
                val haystack = buildString {
                    append(formatRelativeTimestamp(record.timestampMillis))
                    append(' ')
                    append(record.gainedScore)
                    append(" combo x")
                    append(record.combo)
                    append(' ')
                    append(if (record.linesCleared > 0) "${record.linesCleared} baris solusi ditemukan" else "aman tanpa hapus baris")
                }
                haystack.contains(trimmed, ignoreCase = true)
            }
        }
        HistoryUiState(query = query, records = filtered, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    val stats: StateFlow<StatsSummary> = allRecords
        .map { StatsCalculator.compute(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            StatsCalculator.compute(emptyList())
        )

    fun setQuery(value: String) {
        _query.value = value
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clear() }
    }
}
